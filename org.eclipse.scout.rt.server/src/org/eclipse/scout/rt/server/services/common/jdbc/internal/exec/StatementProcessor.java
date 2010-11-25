/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.services.common.jdbc.internal.exec;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.scout.commons.BeanUtility;
import org.eclipse.scout.commons.TriState;
import org.eclipse.scout.commons.beans.FastPropertyDescriptor;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.holders.BeanArrayHolderFilter;
import org.eclipse.scout.commons.holders.IBeanArrayHolder;
import org.eclipse.scout.commons.holders.IHolder;
import org.eclipse.scout.commons.holders.ITableHolder;
import org.eclipse.scout.commons.holders.NVPair;
import org.eclipse.scout.commons.holders.TableHolderFilter;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.ServerJob;
import org.eclipse.scout.rt.server.services.common.jdbc.ISelectStreamHandler;
import org.eclipse.scout.rt.server.services.common.jdbc.ISqlService;
import org.eclipse.scout.rt.server.services.common.jdbc.IStatementCache;
import org.eclipse.scout.rt.server.services.common.jdbc.IStatementProcessor;
import org.eclipse.scout.rt.server.services.common.jdbc.IStatementProcessorMonitor;
import org.eclipse.scout.rt.server.services.common.jdbc.SqlBind;
import org.eclipse.scout.rt.server.services.common.jdbc.parsers.BindModel;
import org.eclipse.scout.rt.server.services.common.jdbc.parsers.BindParser;
import org.eclipse.scout.rt.server.services.common.jdbc.parsers.IntoModel;
import org.eclipse.scout.rt.server.services.common.jdbc.parsers.IntoParser;
import org.eclipse.scout.rt.server.services.common.jdbc.parsers.token.DatabaseSpecificToken;
import org.eclipse.scout.rt.server.services.common.jdbc.parsers.token.FunctionInputToken;
import org.eclipse.scout.rt.server.services.common.jdbc.parsers.token.IToken;
import org.eclipse.scout.rt.server.services.common.jdbc.parsers.token.ValueInputToken;
import org.eclipse.scout.rt.server.services.common.jdbc.parsers.token.ValueOutputToken;
import org.eclipse.scout.rt.server.services.common.jdbc.style.ISqlStyle;

public class StatementProcessor implements IStatementProcessor {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(StatementProcessor.class);
  private static final Object NULL = new Object();

  private ISqlService m_callerService;
  private String m_originalStm;
  private Object[] m_bindBases;
  private int m_maxRowCount;
  private BindModel m_bindModel;
  private IToken[] m_ioTokens;
  private List<IBindInput> m_inputList;
  private List<IBindOutput> m_outputList;
  // state
  private int m_currentInputBatchIndex = -1;
  private int m_currentOutputBatchIndex = -1;
  private String m_currentInputStm;
  private TreeMap<Integer/* jdbcBindIndex */, SqlBind> m_currentInputBindMap;

  public StatementProcessor(ISqlService callerService, String stm, Object[] bindBases) throws ProcessingException {
    this(callerService, stm, bindBases, 0);
  }

  public StatementProcessor(ISqlService callerService, String stm, Object[] bindBases, int maxRowCount) throws ProcessingException {
    if (stm == null) throw new ProcessingException("statement is null");
    try {
      m_callerService = callerService;
      m_originalStm = stm;
      m_maxRowCount = maxRowCount;
      // expand bind bases to list
      ArrayList<Object> bases = new ArrayList<Object>();
      if (bindBases != null) {
        for (int i = 0, n = bindBases.length; i < n; i++) {
          bases.add(bindBases[i]);
        }
      }
      // add server session as default
      IServerSession session = ServerJob.getCurrentSession();
      if (session != null) {
        bases.add(session);
        // add shared context by default
        Map<String, Object> shMap = session.getSharedVariableMap();
        bases.add(shMap);
      }
      m_bindBases = bases.toArray();
      //
      m_inputList = new ArrayList<IBindInput>();
      m_outputList = new ArrayList<IBindOutput>();
      //
      IntoModel intoModel = new IntoParser(m_originalStm).parse();
      String stmWithoutSelectInto = intoModel.getFilteredStatement();
      //
      m_bindModel = new BindParser(stmWithoutSelectInto).parse();
      m_ioTokens = m_bindModel.getIOTokens();
      //
      int jdbcBindIndex = 1;
      for (IToken t : m_ioTokens) {
        IBindInput in = null;
        IBindOutput out = null;
        if (t.isInput()) {
          in = createInput(t, m_bindBases);
          if (in.isJdbcBind()) {
            in.setJdbcBindIndex(jdbcBindIndex);
          }
          m_inputList.add(in);
        }
        if (t.isOutput()) {
          out = createOutput(t, m_bindBases);
          if (out.isJdbcBind()) {
            out.setJdbcBindIndex(jdbcBindIndex);
          }
          m_outputList.add(out);
        }
        //
        if ((in != null && in.isJdbcBind()) || (out != null && out.isJdbcBind())) {
          jdbcBindIndex++;
        }
      }
      for (IToken t : m_bindModel.getAllTokens()) {
        if (t instanceof DatabaseSpecificToken) {
          processDatabaseSpecificToken((DatabaseSpecificToken) t, callerService.getSqlStyle());
        }
      }
      // add select into out binds
      for (IToken t : intoModel.getOutputTokens()) {
        IBindOutput out = createOutput(t, m_bindBases);
        if (!out.isSelectInto()) throw new ProcessingException("out parameter is not a 'select into': " + out);
        if (out.isJdbcBind()) throw new ProcessingException("out parameter is a jdbc bind: " + out);
        out.setJdbcBindIndex(-1);
        m_outputList.add(out);
      }
    }
    catch (ProcessingException e) {
      e.addContextMessage("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm);
      throw e;
    }
    catch (Throwable e) {
      throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
    }
  }

  protected TreeMap<Integer, SqlBind> getCurrentInputBindMap() {
    return m_currentInputBindMap;
  }

  protected ISqlService getCallerService() {
    return m_callerService;
  }

  protected Object[] processResultRow(ResultSet rs) throws SQLException {
    ISqlStyle sqlStyle = m_callerService.getSqlStyle();
    ResultSetMetaData meta = rs.getMetaData();
    int colCount = meta.getColumnCount();
    Object[] row = new Object[colCount];
    for (int i = 0; i < colCount; i++) {
      int type = meta.getColumnType(i + 1);
      row[i] = sqlStyle.readBind(rs, meta, type, i + 1);
    }
    return row;
  }

  protected List<Object[]> processResultRows(ResultSet rs, int maxRowCount) throws SQLException, ProcessingException {
    ArrayList<Object[]> rows = new ArrayList<Object[]>();
    while (rs.next()) {
      Object[] row = processResultRow(rs);
      rows.add(row);
      if (maxRowCount > 0 && rows.size() >= maxRowCount) {
        break;
      }
    }
    return rows;
  }

  /*
   * (non-Javadoc)
   * @seeorg.eclipse.scout.rt.server.services.common.sql.internal.exec.
   * IStatementProcessor#processSelect(java.sql.Connection,
   * org.eclipse.scout.rt.
   * server.services.common.sql.internal.exec.PreparedStatementCache)
   */
  public Object[][] processSelect(Connection conn, IStatementCache cache, IStatementProcessorMonitor monitor) throws ProcessingException {
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      ArrayList<Object[]> rows = new ArrayList<Object[]>();
      while (hasNextInputBatch()) {
        nextInputBatch();
        prepareInputStatementAndBinds();
        ps = cache.getPreparedStatement(conn, m_currentInputStm);
        bindBatch(ps);
        rs = ps.executeQuery();
        for (Object[] row : processResultRows(rs, m_maxRowCount)) {
          rows.add(row);
          nextOutputBatch();
          consumeSelectIntoRow(row);
        }
      }
      finishOutputBatch();
      if (monitor != null) {
        monitor.postFetchData(conn, ps, rs, rows);
      }
      return rows.toArray(new Object[rows.size()][]);
    }
    catch (ProcessingException e) {
      e.addContextMessage("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm);
      throw e;
    }
    catch (Throwable e) {
      throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
    }
    finally {
      if (rs != null) try {
        rs.close();
      }
      catch (Throwable t) {
      }
      try {
        cache.releasePreparedStatement(ps);
      }
      catch (SQLException e) {
        throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
      }
    }
  }

  /*
   * (non-Javadoc)
   * @seeorg.eclipse.scout.rt.server.services.common.sql.internal.exec.
   * IStatementProcessor
   * #processSelectInto(java.sql.Connection,org.eclipse.scout.
   * rt.server.services.common.sql.internal.exec.PreparedStatementCache)
   */
  public void processSelectInto(Connection conn, IStatementCache cache, IStatementProcessorMonitor monitor) throws ProcessingException {
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      int rowCount = 0;
      while (hasNextInputBatch()) {
        nextInputBatch();
        prepareInputStatementAndBinds();
        ps = cache.getPreparedStatement(conn, m_currentInputStm);
        bindBatch(ps);
        rs = ps.executeQuery();
        for (Object[] row : processResultRows(rs, m_maxRowCount)) {
          nextOutputBatch();
          consumeSelectIntoRow(row);
          rowCount++;
        }
      }
      finishOutputBatch();
      if (monitor != null) {
        monitor.postFetchData(conn, ps, rs, null);
      }
    }
    catch (ProcessingException e) {
      e.addContextMessage("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm);
      throw e;
    }
    catch (Throwable e) {
      throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
    }
    finally {
      if (rs != null) try {
        rs.close();
      }
      catch (Throwable t) {
      }
      try {
        cache.releasePreparedStatement(ps);
      }
      catch (SQLException e) {
        throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
      }
    }
  }

  public void processSelectStreaming(Connection conn, IStatementCache cache, ISelectStreamHandler handler) throws ProcessingException {
    PreparedStatement ps = null;
    ResultSet rs = null;
    ISqlStyle sqlStyle = m_callerService.getSqlStyle();
    try {
      int rowCount = 0;
      while (hasNextInputBatch()) {
        nextInputBatch();
        prepareInputStatementAndBinds();
        ps = cache.getPreparedStatement(conn, m_currentInputStm);
        bindBatch(ps);
        rs = ps.executeQuery();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        while (rs.next()) {
          ArrayList<SqlBind> row = new ArrayList<SqlBind>(colCount);
          for (int i = 0; i < colCount; i++) {
            int type = meta.getColumnType(i + 1);
            Object value = sqlStyle.readBind(rs, meta, type, i + 1);
            row.add(new SqlBind(type, value));
          }
          handler.handleRow(conn, ps, rs, rowCount, row);
          rowCount++;
          if (m_maxRowCount > 0 && rowCount >= m_maxRowCount) {
            break;
          }
        }
      }
      finishOutputBatch();
      handler.finished(conn, ps, rs, rowCount);
    }
    catch (ProcessingException e) {
      e.addContextMessage("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm);
      throw e;
    }
    catch (Throwable e) {
      throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
    }
    finally {
      if (rs != null) try {
        rs.close();
      }
      catch (Throwable t) {
      }
      try {
        cache.releasePreparedStatement(ps);
      }
      catch (SQLException e) {
        throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
      }
    }
  }

  /*
   * (non-Javadoc)
   * @seeorg.eclipse.scout.rt.server.services.common.sql.internal.exec.
   * IStatementProcessor#processModification(java.sql.Connection,
   * org.eclipse.scout
   * .rt.server.services.common.sql.internal.exec.PreparedStatementCache)
   */
  public int processModification(Connection conn, IStatementCache cache, IStatementProcessorMonitor monitor) throws ProcessingException {
    PreparedStatement ps = null;
    int rowCount = 0;
    try {
      while (hasNextInputBatch()) {
        nextInputBatch();
        prepareInputStatementAndBinds();
        ps = cache.getPreparedStatement(conn, m_currentInputStm);
        bindBatch(ps);
        rowCount = rowCount + ps.executeUpdate();
      }
      return rowCount;
    }
    catch (ProcessingException e) {
      e.addContextMessage("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm);
      throw e;
    }
    catch (Throwable e) {
      throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
    }
    finally {
      try {
        cache.releasePreparedStatement(ps);
      }
      catch (SQLException e) {
        throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
      }
    }
  }

  /*
   * (non-Javadoc)
   * @seeorg.eclipse.scout.rt.server.services.common.sql.internal.exec.
   * IStatementProcessor#processStoredProcedure(java.sql.Connection,
   * org.eclipse.
   * scout.rt.server.services.common.sql.internal.exec.PreparedStatementCache)
   */
  public boolean processStoredProcedure(Connection conn, IStatementCache cache, IStatementProcessorMonitor monitor) throws ProcessingException {
    CallableStatement cs = null;
    boolean status = true;
    try {
      int batchCount = 0;
      while (hasNextInputBatch()) {
        nextInputBatch();
        prepareInputStatementAndBinds();
        cs = cache.getCallableStatement(conn, m_currentInputStm);
        bindBatch(cs);
        status = status && cs.execute();
        nextOutputBatch();
        consumeOutputRow(cs);
        batchCount++;
      }
      finishOutputBatch();
      return status;
    }
    catch (ProcessingException e) {
      e.addContextMessage("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm);
      throw e;
    }
    catch (Throwable e) {
      throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
    }
    finally {
      try {
        cache.releaseCallableStatement(cs);
      }
      catch (SQLException e) {
        throw new ProcessingException("SQL (original): " + m_originalStm + ", SQL (current): " + m_currentInputStm, e);
      }
    }
  }

  /*
   * (non-Javadoc)
   * @seeorg.eclipse.scout.rt.server.services.common.sql.internal.exec.
   * IStatementProcessor#createPlainText()
   */
  public String createPlainText() throws ProcessingException {
    for (IToken t : m_ioTokens) {
      if (t instanceof ValueInputToken) {
        ValueInputToken vt = (ValueInputToken) t;
        if (vt.isPlainSql()) {
          // ok
        }
        else if (vt.isPlainValue()) {
          // ok
        }
        else {
          vt.setPlainValue(true);
        }
      }
      else if (t instanceof FunctionInputToken) {
        FunctionInputToken ft = (FunctionInputToken) t;
        ft.setPlainValue(true);
      }
    }
    if (hasNextInputBatch()) {
      nextInputBatch();
      prepareInputStatementAndBinds();
      resetInputBatch();
    }
    return m_currentInputStm;
  }

  /*
   * (non-Javadoc)
   * @seeorg.eclipse.scout.rt.server.services.common.sql.internal.exec.
   * IStatementProcessor#simulate()
   */
  public void simulate() throws ProcessingException {
    while (hasNextInputBatch()) {
      nextInputBatch();
      prepareInputStatementAndBinds();
    }
  }

  /**
   * when there are batch inputs, all batch inputs must agree to have another
   * batch when there are no batch inputs, only first batch is valid
   */
  private boolean hasNextInputBatch() {
    int nextIndex = m_currentInputBatchIndex + 1;
    int batchInputCount = 0;
    int batchAcceptCount = 0;
    for (IBindInput input : m_inputList) {
      if (input.isBatch()) {
        batchInputCount++;
        if (input.hasBatch(nextIndex)) {
          batchAcceptCount++;
        }
      }
    }
    if (batchInputCount > 0) {
      return batchInputCount == batchAcceptCount;
    }
    else {
      return nextIndex == 0;
    }
  }

  private void resetInputBatch() {
    m_currentInputBatchIndex = -1;
    for (IBindInput in : m_inputList) {
      in.setNextBatchIndex(m_currentInputBatchIndex);
    }
  }

  private void nextInputBatch() {
    m_currentInputBatchIndex++;
    for (IBindInput in : m_inputList) {
      in.setNextBatchIndex(m_currentInputBatchIndex);
    }
  }

  private void nextOutputBatch() {
    m_currentOutputBatchIndex++;
    for (IBindOutput out : m_outputList) {
      out.setNextBatchIndex(m_currentOutputBatchIndex);
    }
  }

  private void consumeSelectIntoRow(Object[] row) throws ProcessingException {
    int index = 0;
    for (IBindOutput out : m_outputList) {
      if (out.isSelectInto()) {
        out.consumeValue(row[index]);
        index++;
      }
    }
  }

  private void consumeOutputRow(CallableStatement cs) throws ProcessingException, SQLException {
    for (IBindOutput out : m_outputList) {
      if (out.isJdbcBind()) {
        out.consumeValue(cs.getObject(out.getJdbcBindIndex()));
      }
    }
  }

  private void finishOutputBatch() throws ProcessingException {
    for (IBindOutput out : m_outputList) {
      out.finishBatch();
    }
  }

  private void prepareInputStatementAndBinds() throws ProcessingException {
    StringBuffer debugBuf = null;
    if (LOG.isDebugEnabled()) {
      debugBuf = new StringBuffer();
    }
    if (debugBuf != null) {
      debugBuf = new StringBuffer();
      debugBuf.append("SQL");
      debugBuf.append("\n");
      debugBuf.append(m_originalStm);
    }
    // bind inputs and set replace token on inputs
    m_currentInputBindMap = new TreeMap<Integer, SqlBind>();
    for (IBindInput in : m_inputList) {
      SqlBind bind = in.produceSqlBindAndSetReplaceToken(m_callerService.getSqlStyle());
      assert (bind != null) == in.isJdbcBind();
      if (bind != null) {
        m_currentInputBindMap.put(in.getJdbcBindIndex(), bind);
      }
      if (debugBuf != null) {
        debugBuf.append("\n");
        debugBuf.append("IN ");
        debugBuf.append(in.getToken().getParsedToken());
        debugBuf.append(" => ");
        debugBuf.append(in.getToken().getReplaceToken());
        if (bind != null) {
          debugBuf.append(" [");
          debugBuf.append(SqlBind.decodeJdbcType(bind.getSqlType()) + " " + bind.getValue());
          debugBuf.append("]");
        }
      }
    }
    // set replace token on outputs
    for (IBindOutput out : m_outputList) {
      out.setReplaceToken(m_callerService.getSqlStyle());
      if (debugBuf != null) {
        debugBuf.append("\n");
        debugBuf.append("OUT ");
        debugBuf.append(out.getToken().getParsedToken());
        debugBuf.append(" => ");
        debugBuf.append(out.getToken().getReplaceToken());
        debugBuf.append(" [");
        debugBuf.append(out.getBindType().getSimpleName());
        debugBuf.append("]");
      }
    }
    m_currentInputStm = m_bindModel.getFilteredStatement();
    if (debugBuf != null) {
      LOG.debug(debugBuf.toString());
    }
  }

  private void bindBatch(PreparedStatement ps) throws ProcessingException {
    try {
      // bind inputs
      if (ps instanceof PreparedStatement) {
        writeBinds(ps);
      }
      // register outputs
      if (ps instanceof CallableStatement) {
        registerOutputs((CallableStatement) ps);
      }
    }
    catch (Throwable e) {
      throw new ProcessingException("unexpected exception", e);
    }
  }

  protected void writeBinds(PreparedStatement ps) throws SQLException {
    ISqlStyle sqlStyle = m_callerService.getSqlStyle();
    for (Map.Entry<Integer, SqlBind> e : m_currentInputBindMap.entrySet()) {
      sqlStyle.writeBind(ps, e.getKey(), e.getValue());
    }
  }

  protected void registerOutputs(CallableStatement cs) throws SQLException {
    ISqlStyle sqlStyle = m_callerService.getSqlStyle();
    for (IBindOutput out : m_outputList) {
      if (out.isJdbcBind()) {
        sqlStyle.registerOutput(cs, out.getJdbcBindIndex(), out.getBindType());
      }
    }
  }

  private IBindInput createInput(IToken bindToken, Object[] bindBases) throws ProcessingException {
    IBindInput o = null;
    if (bindToken instanceof ValueInputToken) {
      ValueInputToken valueInputToken = (ValueInputToken) bindToken;
      String[] path = valueInputToken.getName().split("[.]");
      for (int i = 0; i < bindBases.length; i++) {
        Object bindBase = bindBases[i];
        Class nullType = null;
        if (bindBase instanceof NVPair) {
          nullType = ((NVPair) bindBase).getNullType();
        }
        o = createInputRec(valueInputToken, path, bindBases[i], nullType);
        if (o != null) {
          break;
        }
      }
      if (o == null) throw new ProcessingException("Cannot find input for '" + valueInputToken + "' in bind bases.");
    }
    else if (bindToken instanceof FunctionInputToken) {
      o = new FunctionInput(m_callerService, m_bindBases, (FunctionInputToken) bindToken);
    }
    return o;
  }

  private IBindInput createInputRec(ValueInputToken bindToken, String[] path, Object bindBase, Class nullType) throws ProcessingException {
    boolean terminal = (path.length == 1);
    Object o = null;
    boolean found = false;
    if (bindBase instanceof Map) {
      // handle all terminal cases for map
      o = ((Map) bindBase).get(path[0]);
      if (o != null) {
        found = true;
      }
      else if (((Map) bindBase).containsKey(path[0])) {
        found = true;
      }
      if (found) {
        // special case: table holder and table filter are preemptive terminals
        if (o instanceof ITableHolder) {
          return new TableHolderInput((ITableHolder) o, null, path[1], bindToken);
        }
        else if (o instanceof TableHolderFilter) {
          return new TableHolderInput(((TableHolderFilter) o).getTableHolder(), ((TableHolderFilter) o).getFilteredRows(), path[1], bindToken);
        }
        else if (o instanceof IBeanArrayHolder) {
          return new BeanArrayHolderInput((IBeanArrayHolder) o, null, path[1], bindToken);
        }
        else if (o instanceof BeanArrayHolderFilter) {
          return new BeanArrayHolderInput(((BeanArrayHolderFilter) o).getBeanArrayHolder(), ((BeanArrayHolderFilter) o).getFilteredBeans(), path[1], bindToken);
        }
        else {
          if (terminal) {
            return createInputTerminal(o, nullType, bindToken);
          }
          else {
            if (o == null) {
              throw new ProcessingException("input bind " + bindToken + " resolves to null on path element: " + path[0]);
            }
          }
        }
      }
    }
    else if (bindBase instanceof NVPair) {
      // handle all terminal cases for nvpair
      if (((NVPair) bindBase).getName().equals(path[0])) {
        o = ((NVPair) bindBase).getValue();
        found = true;
        // special case: table holder and table filter are preemptive terminals
        if (o instanceof ITableHolder) {
          return new TableHolderInput((ITableHolder) o, null, path[1], bindToken);
        }
        else if (o instanceof TableHolderFilter) {
          return new TableHolderInput(((TableHolderFilter) o).getTableHolder(), ((TableHolderFilter) o).getFilteredRows(), path[1], bindToken);
        }
        else if (o instanceof IBeanArrayHolder) {
          return new BeanArrayHolderInput((IBeanArrayHolder) o, null, path[1], bindToken);
        }
        else if (o instanceof BeanArrayHolderFilter) {
          return new BeanArrayHolderInput(((BeanArrayHolderFilter) o).getBeanArrayHolder(), ((BeanArrayHolderFilter) o).getFilteredBeans(), path[1], bindToken);
        }
        else {
          if (terminal) {
            return createInputTerminal(o, nullType, bindToken);
          }
          else {
            if (o == null) {
              throw new ProcessingException("input bind " + bindToken + " resolves to null on path element: " + path[0]);
            }
          }
        }
      }
    }
    else if (bindBase instanceof ITableHolder) {
      // handle all terminal cases for table holder
      ITableHolder table = (ITableHolder) bindBase;
      try {
        Method m = table.getClass().getMethod("get" + Character.toUpperCase(path[0].charAt(0)) + path[0].substring(1), new Class[]{int.class});
        if (m != null) {
          found = true;
          return new TableHolderInput(table, null, path[0], bindToken);
        }
      }
      catch (Throwable t) {
        found = false;
        // nop
      }
    }
    else if (bindBase instanceof TableHolderFilter) {
      // handle all terminal cases for table holder filter
      TableHolderFilter filter = (TableHolderFilter) bindBase;
      ITableHolder table = filter.getTableHolder();
      try {
        Method m = table.getClass().getMethod("get" + Character.toUpperCase(path[0].charAt(0)) + path[0].substring(1), new Class[]{int.class});
        if (m != null) {
          found = true;
          return new TableHolderInput(table, filter.getFilteredRows(), path[0], bindToken);
        }
      }
      catch (Throwable t) {
        // nop
        found = false;
      }
    }
    else if (bindBase instanceof IBeanArrayHolder) {
      // handle all terminal cases for BeanArrayHolder
      IBeanArrayHolder holder = (IBeanArrayHolder) bindBase;
      try {
        Method m = holder.getHolderType().getMethod("get" + Character.toUpperCase(path[0].charAt(0)) + path[0].substring(1));
        if (m != null) {
          found = true;
          return new BeanArrayHolderInput(holder, null, path[0], bindToken);
        }
      }
      catch (Throwable t1) {
        try {
          Method m = holder.getHolderType().getMethod("is" + Character.toUpperCase(path[0].charAt(0)) + path[0].substring(1));
          if (m != null) {
            found = true;
            return new BeanArrayHolderInput(holder, null, path[0], bindToken);
          }
        }
        catch (Throwable t2) {
          found = false;
          // nop
        }
      }
    }
    else if (bindBase instanceof BeanArrayHolderFilter) {
      // handle all terminal cases for table holder filter
      BeanArrayHolderFilter filter = (BeanArrayHolderFilter) bindBase;
      IBeanArrayHolder holder = filter.getBeanArrayHolder();
      try {
        Method m = holder.getHolderType().getMethod("get" + Character.toUpperCase(path[0].charAt(0)) + path[0].substring(1));
        if (m != null) {
          found = true;
          return new BeanArrayHolderInput(holder, filter.getFilteredBeans(), path[0], bindToken);
        }
      }
      catch (Throwable t1) {
        try {
          Method m = holder.getHolderType().getMethod("is" + Character.toUpperCase(path[0].charAt(0)) + path[0].substring(1));
          if (m != null) {
            found = true;
            return new BeanArrayHolderInput(holder, null, path[0], bindToken);
          }
        }
        catch (Throwable t2) {
          found = false;
          // nop
        }
      }
    }
    else if (bindBase != null) {
      if (bindBase.getClass().isArray() && terminal) {
        return new BeanPropertyInput(path[0], (Object[]) bindBase, bindToken);
      }
      if (bindBase instanceof Collection && terminal) {
        return new BeanPropertyInput(path[0], ((Collection) bindBase).toArray(), bindToken);
      }
      /* bean propertry */
      try {
        Object propertyBean = bindBase;
        FastPropertyDescriptor pd = BeanUtility.getFastBeanInfo(propertyBean.getClass(), null).getPropertyDescriptor(path[0]);
        Method getter = pd != null ? pd.getReadMethod() : null;
        if (getter != null) {
          // getter exists
          o = getter.invoke(propertyBean);
          found = true;
          if (terminal) {
            return createInputTerminal(o, getter.getReturnType(), bindToken);
          }
          else {
            if (o == null) {
              throw new ProcessingException("input bind " + bindToken + " resolves to null on path element: " + path[0]);
            }
          }
        }
      }
      catch (Exception e) {
        // obviously there is no such property
      }
    }
    //
    if (found) {
      if (terminal) {
        throw new ProcessingException("input bind '" + bindToken.getName() + "' was not recognized as a terminal");
      }
      // continue
      String[] newPath = new String[path.length - 1];
      System.arraycopy(path, 1, newPath, 0, newPath.length);
      return createInputRec(bindToken, newPath, o, nullType);
    }
    else {
      return null;
    }
  }

  private IBindOutput createOutput(IToken bindToken, Object[] bindBases) throws ProcessingException {
    IBindOutput o = null;
    if (bindToken instanceof ValueOutputToken) {
      ValueOutputToken valueOutputToken = (ValueOutputToken) bindToken;
      String[] path = valueOutputToken.getName().split("[.]");
      for (int i = 0; i < bindBases.length; i++) {
        o = createOutputRec(valueOutputToken, path, bindBases[i]);
        if (o != null) {
          break;
        }
      }
      if (o == null) throw new ProcessingException("Cannot find output for '" + valueOutputToken + "' in bind base. When selecting into shared context variables make sure these variables are initialized using CONTEXT.set<i>PropertyName</i>(null)");
    }
    else {
      throw new ProcessingException("Cannot find output for '" + bindToken.getClass());
    }
    return o;
  }

  @SuppressWarnings("unchecked")
  private IBindOutput createOutputRec(ValueOutputToken bindToken, String[] path, final Object bindBase) throws ProcessingException {
    boolean terminal = (path.length == 1);
    Object o = null;
    boolean found = false;
    if (bindBase instanceof Map) {
      // handle all terminal cases for map
      o = ((Map) bindBase).get(path[0]);
      if (o != null) {
        found = true;
      }
      else if (((Map) bindBase).containsKey(path[0])) {
        found = true;
      }
      if (found) {
        // special case: table holder is preemptive terminal
        if (o instanceof ITableHolder) {
          ITableHolder table = (ITableHolder) o;
          return new TableHolderOutput(table, path[1], bindToken);
        }
        else if (o instanceof IBeanArrayHolder) {
          IBeanArrayHolder holder = (IBeanArrayHolder) o;
          return new BeanArrayHolderOutput(holder, path[1], bindToken);
        }
        else if (o instanceof IHolder) {
          if (terminal) {
            return createOutputTerminal((IHolder) o, bindToken);
          }
          else {
            o = ((IHolder) o).getValue();
          }
        }
        else if (o == null) {
          if (terminal) {
            return new MapOutput((Map) bindBase, path[0], bindToken);
          }
          else {
            throw new ProcessingException("output bind " + bindToken + " resolves to null on path element: " + path[0]);
          }
        }
        else {
          if (terminal) {
            return new MapOutput((Map) bindBase, path[0], bindToken);
          }
        }
      }
    }
    else if (bindBase instanceof NVPair) {
      // handle all terminal cases for nvpair
      if (((NVPair) bindBase).getName().equals(path[0])) {
        o = ((NVPair) bindBase).getValue();
        found = true;
        // special case: table holder is preemptive terminal
        if (o instanceof ITableHolder) {
          ITableHolder table = (ITableHolder) o;
          return new TableHolderOutput(table, path[1], bindToken);
        }
        else if (o instanceof IBeanArrayHolder) {
          IBeanArrayHolder holder = (IBeanArrayHolder) o;
          return new BeanArrayHolderOutput(holder, path[1], bindToken);
        }
        else if (o instanceof IHolder) {
          if (terminal) {
            return createOutputTerminal((IHolder) o, bindToken);
          }
          else {
            o = ((IHolder) o).getValue();
          }
        }
        else if (o == null) {
          throw new ProcessingException("output bind " + bindToken + " resolves to null on path element: " + path[0]);
        }
        else {
          if (terminal) {
            throw new ProcessingException("output bind " + bindToken + " is not a valid output container");
          }
        }
      }
    }
    else if (bindBase instanceof ITableHolder) {
      // handle all terminal cases for table holder
      ITableHolder table = (ITableHolder) bindBase;
      try {
        Method m = table.getClass().getMethod("get" + Character.toUpperCase(path[0].charAt(0)) + path[0].substring(1), new Class[]{int.class});
        if (m != null) {
          found = true;
          return new TableHolderOutput(table, path[0], bindToken);
        }
      }
      catch (Throwable t) {
        // nop
        found = false;
      }
    }
    else if (bindBase instanceof IBeanArrayHolder) {
      // handle all terminal cases for BeanArrayHolder
      IBeanArrayHolder holder = (IBeanArrayHolder) bindBase;
      try {
        Method m = holder.getHolderType().getMethod("get" + Character.toUpperCase(path[0].charAt(0)) + path[0].substring(1));
        if (m != null) {
          found = true;
          return new BeanArrayHolderOutput(holder, path[0], bindToken);
        }
      }
      catch (Throwable t1) {
        try {
          Method m = holder.getHolderType().getMethod("is" + Character.toUpperCase(path[0].charAt(0)) + path[0].substring(1));
          if (m != null) {
            found = true;
            return new BeanArrayHolderOutput(holder, path[0], bindToken);
          }
        }
        catch (Throwable t2) {
          found = false;
          // nop
        }
      }
    }
    else/* bean property */{
      // handle all terminal cases for bean property
      try {
        FastPropertyDescriptor pd = BeanUtility.getFastBeanInfo(bindBase.getClass(), null).getPropertyDescriptor(path[0]);
        if (terminal) {
          Method setter = pd != null ? pd.getWriteMethod() : null;
          if (setter != null) {
            found = true;
            return new AbstractBeanPropertyOutput(bindBase.getClass(), path[0], bindToken) {
              @Override
              protected Object[] getFinalBeanArray() {
                return new Object[]{bindBase};
              }
            };
          }
          else {
            Method getter = pd != null ? pd.getReadMethod() : null;
            if (getter != null) {
              o = getter.invoke(bindBase, (Object[]) null);
              if (o instanceof ITableHolder) {
                throw new ProcessingException("output bind '" + bindToken.getName() + "' is a table and should not be a terminal");
              }
              else if (o instanceof IBeanArrayHolder) {
                throw new ProcessingException("output bind '" + bindToken.getName() + "' is a bean array and should not be a terminal");
              }
              else if (o instanceof IHolder) {
                return createOutputTerminal((IHolder) o, bindToken);
              }
              else {
                throw new ProcessingException("output bind '" + bindToken.getName() + "' is not a holder");
              }
            }
          }
        }
        else {
          Method getter = pd != null ? pd.getReadMethod() : null;
          if (getter != null) {
            Object readValue = getter.invoke(bindBase, (Object[]) null);
            o = readValue;
            found = true;
          }
        }
      }
      catch (Exception e) {
        // obviously there is no such property
      }
    }
    //
    if (found) {
      if (terminal) {
        throw new ProcessingException("output bind '" + bindToken.getName() + "' was not recognized as a terminal");
      }
      // continue
      String[] newPath = new String[path.length - 1];
      System.arraycopy(path, 1, newPath, 0, newPath.length);
      return createOutputRec(bindToken, newPath, o);
    }
    else {
      return null;
    }
  }

  private IBindOutput createOutputTerminal(IHolder h, ValueOutputToken bindToken) {
    Class cls = h.getHolderType();
    if (cls.isArray()) {
      // byte[] and char[] are no "arrays"
      if (cls == byte[].class || cls == char[].class) {
        return new SingleHolderOutput(h, bindToken);
      }
      else {
        return new ArrayHolderOutput(h, bindToken);
      }
    }
    else {
      return new SingleHolderOutput(h, bindToken);
    }
  }

  private IBindInput createInputTerminal(Object o, Class nullType, ValueInputToken bindToken) throws ProcessingException {
    if (o == null) {
      return new SingleInput(null, nullType, bindToken);
    }
    else if (o instanceof IHolder) {
      Class cls = ((IHolder) o).getHolderType();
      if (nullType == null) {
        nullType = cls;
      }
      if (cls.isArray()) {
        // byte[] and char[] are no "arrays"
        if (cls == byte[].class || cls == char[].class) {
          return new SingleInput(((IHolder) o).getValue(), nullType, bindToken);
        }
        else {
          return new ArrayInput(((IHolder) o).getValue(), bindToken);
        }
      }
      else if (cls == TriState.class) {
        return new TriStateInput((TriState) ((IHolder) o).getValue(), bindToken);
      }
      else {
        return new SingleInput(((IHolder) o).getValue(), nullType, bindToken);
      }
    }
    else {
      if (o instanceof Collection) {
        return new ArrayInput(((Collection) o).toArray(), bindToken);
      }
      else if (o.getClass().isArray()) {
        Class cls = o.getClass();
        // byte[] and char[] are no "arrays"
        if (cls == byte[].class || cls == char[].class) {
          return new SingleInput(o, nullType, bindToken);
        }
        else {
          return new ArrayInput(o, bindToken);
        }
      }
      else if (o.getClass() == TriState.class) {
        return new TriStateInput((TriState) o, bindToken);
      }
      else {
        return new SingleInput(o, nullType, bindToken);
      }
    }
  }

  protected void processDatabaseSpecificToken(DatabaseSpecificToken t, ISqlStyle sqlStyle) {
    String name = t.getName().toLowerCase();
    if (name.equals("sysdate")) {
      t.setReplaceToken(sqlStyle.getSysdateToken());
    }
    else if (name.equals("upper")) {
      t.setReplaceToken(sqlStyle.getUpperToken());
    }
    else if (name.equals("lower")) {
      t.setReplaceToken(sqlStyle.getLowerToken());
    }
    else if (name.equals("trim")) {
      t.setReplaceToken(sqlStyle.getTrimToken());
    }
    else if (name.equals("nvl")) {
      t.setReplaceToken(sqlStyle.getNvlToken());
    }
    else {
      LOG.warn("used unknown database specific token " + t.getParsedToken());
      t.setReplaceToken(name);
    }
  }

}
