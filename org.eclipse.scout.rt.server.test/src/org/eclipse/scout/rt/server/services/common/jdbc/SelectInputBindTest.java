/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.services.common.jdbc;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.eclipse.scout.commons.holders.ITableBeanHolder;
import org.eclipse.scout.commons.holders.ITableHolder;
import org.eclipse.scout.commons.holders.NVPair;
import org.eclipse.scout.commons.holders.TableBeanHolderFilter;
import org.eclipse.scout.commons.holders.TableHolderFilter;
import org.eclipse.scout.rt.server.services.common.jdbc.fixture.ContainerBean;
import org.eclipse.scout.rt.server.services.common.jdbc.fixture.SqlServiceMock;
import org.eclipse.scout.rt.server.services.common.jdbc.fixture.TableFieldBeanData;
import org.eclipse.scout.rt.server.services.common.jdbc.fixture.TableFieldBeanData.TableFieldBeanDataRowData;
import org.eclipse.scout.rt.server.services.common.jdbc.fixture.TableFieldData;
import org.eclipse.scout.rt.server.services.common.jdbc.style.OracleSqlStyle;
import org.junit.Test;

/**
 * Test for {@link ISqlService} (using the mock {@link SqlServiceMock}).
 * Different types of arrays used as input bind.
 */
public class SelectInputBindTest {

  /**
   * {@link TableFieldData} is from type {@link ITableHolder} (existing before Luna).
   * Direct batch update.
   */
  @Test
  public void testBatchUpdateFromTableFieldData() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldData tableData = createTableFieldData(false);
    sql.update("UDPATE my_table SET a=:{active}, s=:{state} where n=:{name} ", tableData);
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldData} is from type {@link ITableHolder} (existing before Luna).
   * TableData for batch update is in NVPair bind.
   */
  @Test
  public void testBatchUpdateFromTableFieldDataInNVPair() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldData tableData = createTableFieldData(false);
    sql.update("UDPATE my_table SET a=:{table.active}, s=:{table.state} where n=:{table.name} ", new NVPair("table", tableData));
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldData} is from type {@link ITableHolder} (existing before Luna).
   * TableData for batch update is in Map bind.
   */
  @Test
  public void testBatchUpdateFromTableFieldDataInMap() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldData tableData = createTableFieldData(false);
    Map<String, ?> map = Collections.singletonMap("table", tableData);
    sql.update("UDPATE my_table SET a=:{table.active}, s=:{table.state} where n=:{table.name} ", map);
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldData} is from type {@link ITableHolder} (existing before Luna).
   * TableData for batch update is in a bean (ContainerBean).
   */
  @Test
  public void testBatchUpdateFromTableFieldDataInBean() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldData tableData = createTableFieldData(false);
    ContainerBean bean = new ContainerBean();
    bean.setTableFieldData(tableData);
    sql.update("UDPATE my_table SET a=:{tableFieldData.active}, s=:{tableFieldData.state} where n=:{tableFieldData.name} ", bean);
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldData} in combination with {@link TableHolderFilter} (existing before Luna).
   * Direct batch update.
   */
  @Test
  public void testBatchUpdateFromTableHolderFilter() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldData tableData = createTableFieldData(true);
    TableHolderFilter filter = new TableHolderFilter(tableData, ITableHolder.STATUS_UPDATED);
    sql.update("UDPATE my_table SET a=:{active}, s=:{state} where n=:{name} ", filter);
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldData} in combination with {@link TableHolderFilter} (existing before Luna).
   * TableData for batch update is in NVPair bind.
   */
  @Test
  public void testBatchUpdateFromTableHolderFilterInNVPair() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldData tableData = createTableFieldData(true);
    TableHolderFilter filter = new TableHolderFilter(tableData, ITableHolder.STATUS_UPDATED);
    sql.update("UDPATE my_table SET a=:{filter.active}, s=:{filter.state} where n=:{filter.name} ", new NVPair("filter", filter));
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldData} in combination with {@link TableHolderFilter} (existing before Luna).
   * TableData for batch update is in Map bind.
   */
  @Test
  public void testBatchUpdateFromTableHolderFilterInMap() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldData tableData = createTableFieldData(true);
    TableHolderFilter filter = new TableHolderFilter(tableData, ITableHolder.STATUS_UPDATED);
    Map<String, ?> map = Collections.singletonMap("filter", filter);
    sql.update("UDPATE my_table SET a=:{filter.active}, s=:{filter.state} where n=:{filter.name} ", map);
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldData} in combination with {@link TableHolderFilter} (existing before Luna).
   * TableData for batch update is in a bean (ContainerBean).
   */
  @Test
  public void testBatchUpdateFromTableHolderFilterInBean() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldData tableData = createTableFieldData(true);
    TableHolderFilter filter = new TableHolderFilter(tableData, ITableHolder.STATUS_UPDATED);
    ContainerBean bean = new ContainerBean();
    bean.setTableHolderFilter(filter);
    sql.update("UDPATE my_table SET a=:{TableHolderFilter.active}, s=:{TableHolderFilter.state} where n=:{TableHolderFilter.name} ", bean);
    assertExpectedProtocol(sql);
  }

  private TableFieldData createTableFieldData(boolean withAdditionalRows) {
    TableFieldData tableData = new TableFieldData();
    if (withAdditionalRows) {
      createRow(tableData, ITableHolder.STATUS_INSERTED, false, 6, "xxx");
    }
    createRow(tableData, ITableHolder.STATUS_UPDATED, true, 3, "lorem");
    if (withAdditionalRows) {
      createRow(tableData, ITableHolder.STATUS_DELETED, false, 8, "yyy");
    }
    createRow(tableData, ITableHolder.STATUS_UPDATED, false, 6, "ipsum");
    if (withAdditionalRows) {
      createRow(tableData, ITableHolder.STATUS_INSERTED, true, 2, "zzz");
    }
    return tableData;
  }

  private void createRow(TableFieldData tableData, int rowStatus, Boolean active, Integer state, String name) {
    int row;
    row = tableData.addRow(rowStatus);
    tableData.setActive(row, active);
    tableData.setState(row, state);
    tableData.setName(row, name);
  }

  /**
   * {@link TableFieldBeanData} is from type {@link ITableBeanHolder} (introduced with Luna).
   * Direct batch update.
   */
  @Test
  public void testBatchUpdateFromTableFieldBeanData() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldBeanData tableData = createTableFieldBeanData(false);
    sql.update("UDPATE my_table SET a=:{active}, s=:{state} where n=:{name} ", tableData);
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldBeanData} is from type {@link ITableBeanHolder} (introduced with Luna).
   * TableData for batch update is in NVPair bind.
   */
  @Test
  public void testBatchUpdateFromTableFieldBeanDataInNVPair() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldBeanData tableData = createTableFieldBeanData(false);
    sql.update("UDPATE my_table SET a=:{table.active}, s=:{table.state} where n=:{table.name} ", new NVPair("table", tableData));
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldBeanData} is from type {@link ITableBeanHolder} (introduced with Luna).
   * TableData for batch update is in Map bind.
   */
  @Test
  public void testBatchUpdateFromTableFieldBeanDataInMap() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldBeanData tableData = createTableFieldBeanData(false);
    Map<String, ?> map = Collections.singletonMap("table", tableData);
    sql.update("UDPATE my_table SET a=:{table.active}, s=:{table.state} where n=:{table.name} ", map);
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldBeanData} is from type {@link ITableBeanHolder} (introduced with Luna).
   * TableData for batch update is in a bean (ContainerBean).
   */
  @Test
  public void testBatchUpdateFromTableFieldBeanDataInBean() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldBeanData tableData = createTableFieldBeanData(false);
    ContainerBean bean = new ContainerBean();
    bean.setTableFieldBeanData(tableData);
    sql.update("UDPATE my_table SET a=:{tableFieldBeanData.active}, s=:{tableFieldBeanData.state} where n=:{tableFieldBeanData.name} ", bean);
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldBeanData} in combination with {@link TableBeanHolderFilter} (introduced with Luna).
   * Direct batch update.
   */
  @Test
  public void testBatchUpdateFromTableBeanHolderFilter() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldBeanData tableData = createTableFieldBeanData(true);
    TableBeanHolderFilter filter = new TableBeanHolderFilter(tableData, ITableHolder.STATUS_UPDATED);
    sql.update("UDPATE my_table SET a=:{active}, s=:{state} where n=:{name} ", filter);
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldBeanData} in combination with {@link TableBeanHolderFilter} (introduced with Luna).
   * TableData for batch update is in NVPair bind.
   */
  @Test
  public void testBatchUpdateFromTableBeanHolderFilterInNVPair() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldBeanData tableData = createTableFieldBeanData(true);
    TableBeanHolderFilter filter = new TableBeanHolderFilter(tableData, ITableHolder.STATUS_UPDATED);
    sql.update("UDPATE my_table SET a=:{filter.active}, s=:{filter.state} where n=:{filter.name} ", new NVPair("filter", filter));
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldBeanData} in combination with {@link TableBeanHolderFilter} (introduced with Luna).
   * TableData for batch update is in Map bind.
   */
  @Test
  public void testBatchUpdateFromTableBeanHolderFilterInMap() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldBeanData tableData = createTableFieldBeanData(true);
    TableBeanHolderFilter filter = new TableBeanHolderFilter(tableData, ITableHolder.STATUS_UPDATED);
    Map<String, ?> map = Collections.singletonMap("filter", filter);
    sql.update("UDPATE my_table SET a=:{filter.active}, s=:{filter.state} where n=:{filter.name} ", map);
    assertExpectedProtocol(sql);
  }

  /**
   * {@link TableFieldBeanData} in combination with {@link TableBeanHolderFilter} (introduced with Luna).
   * TableData for batch update is in a bean (ContainerBean).
   */
  @Test
  public void testBatchUpdateFromTableBeanHolderFilterInBean() throws Exception {
    SqlServiceMock sql = createSqlServiceMock();
    TableFieldBeanData tableData = createTableFieldBeanData(true);
    TableBeanHolderFilter filter = new TableBeanHolderFilter(tableData, ITableHolder.STATUS_UPDATED);
    ContainerBean bean = new ContainerBean();
    bean.setTableBeanHolderFilter(filter);
    sql.update("UDPATE my_table SET a=:{TableBeanHolderFilter.active}, s=:{TableBeanHolderFilter.state} where n=:{TableBeanHolderFilter.name} ", bean);
    assertExpectedProtocol(sql);
  }

  private TableFieldBeanData createTableFieldBeanData(boolean withAdditionalRows) {
    TableFieldBeanData tableBeanData = new TableFieldBeanData();
    if (withAdditionalRows) {
      createRow(tableBeanData, ITableHolder.STATUS_INSERTED, false, 6, "xxx");
    }
    createRow(tableBeanData, ITableHolder.STATUS_UPDATED, true, 3, "lorem");
    if (withAdditionalRows) {
      createRow(tableBeanData, ITableHolder.STATUS_DELETED, false, 8, "yyy");
    }
    createRow(tableBeanData, ITableHolder.STATUS_UPDATED, false, 6, "ipsum");
    if (withAdditionalRows) {
      createRow(tableBeanData, ITableHolder.STATUS_INSERTED, true, 2, "zzz");
    }
    return tableBeanData;
  }

  private void createRow(TableFieldBeanData tableBeanData, int rowStatus, Boolean active, Integer state, String name) {
    TableFieldBeanDataRowData row = tableBeanData.addRow(rowStatus);
    row.setActive(active);
    row.setState(state);
    row.setName(name);
  }

  private static SqlServiceMock createSqlServiceMock() {
    SqlServiceMock sql = new SqlServiceMock();
    sql.setSqlStyle(new OracleSqlStyle());
    sql.clearProtocol();
    return sql;
  }

  private static final String EXPECTED_PROTOCOL = "Connection.prepareStatement(UDPATE my_table SET a = ?, s = ? where n = ?)\n" +
      "PreparedStatement.setObject(1, 1, 4)\n" +
      "PreparedStatement.setObject(2, 3, 4)\n" +
      "PreparedStatement.setObject(3, lorem, 12)\n" +
      "Connection.prepareStatement(UDPATE my_table SET a = ?, s = ? where n = ?)\n" +
      "PreparedStatement.setObject(1, 0, 4)\n" +
      "PreparedStatement.setObject(2, 6, 4)\n" +
      "PreparedStatement.setObject(3, ipsum, 12)\n";

  private static void assertExpectedProtocol(SqlServiceMock sql) {
    assertEquals(EXPECTED_PROTOCOL, sql.getProtocol().toString());
  }

}
