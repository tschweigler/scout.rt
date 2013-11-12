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
package org.eclipse.scout.rt.client.ui.form.fields.listbox;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.TriState;
import org.eclipse.scout.commons.TypeCastUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.services.lookup.FormFieldProvisioningContext;
import org.eclipse.scout.rt.client.services.lookup.ILookupCallProvisioningService;
import org.eclipse.scout.rt.client.ui.basic.cell.Cell;
import org.eclipse.scout.rt.client.ui.basic.cell.ICell;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTableRowBuilder;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRowFilter;
import org.eclipse.scout.rt.client.ui.basic.table.TableAdapter;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;
import org.eclipse.scout.rt.client.ui.basic.table.TableRow;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractBooleanColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractStringColumn;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.eclipse.scout.rt.client.ui.form.IFormFieldVisitor;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractValueField;
import org.eclipse.scout.rt.client.ui.form.fields.GridData;
import org.eclipse.scout.rt.client.ui.form.fields.ICompositeField;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.data.basic.FontSpec;
import org.eclipse.scout.rt.shared.data.form.ValidationRule;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.lookup.CodeLookupCall;
import org.eclipse.scout.rt.shared.services.lookup.LookupCall;
import org.eclipse.scout.rt.shared.services.lookup.LookupRow;
import org.eclipse.scout.service.SERVICES;

public abstract class AbstractListBox<T> extends AbstractValueField<T[]> implements IListBox<T> {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractListBox.class);

  private ITable m_table;
  private IListBoxUIFacade m_uiFacade;
  private LookupCall m_lookupCall;
  private Class<? extends ICodeType> m_codeTypeClass;
  private boolean m_valueTableSyncActive;
  private ITableRowFilter m_checkedRowsFilter;
  private ITableRowFilter m_activeRowsFilter;
  // children
  private IFormField[] m_fields;

  public AbstractListBox() {
    this(true);
  }

  public AbstractListBox(boolean callInitializer) {
    super(callInitializer);
  }

  /*
   * Configuration
   */
  @ConfigProperty(ConfigProperty.LOOKUP_CALL)
  @Order(240)
  @ValidationRule(ValidationRule.LOOKUP_CALL)
  protected Class<? extends LookupCall> getConfiguredLookupCall() {
    return null;
  }

  @ConfigProperty(ConfigProperty.CODE_TYPE)
  @Order(250)
  @ValidationRule(ValidationRule.CODE_TYPE)
  protected Class<? extends ICodeType> getConfiguredCodeType() {
    return null;
  }

  @ConfigProperty(ConfigProperty.ICON_ID)
  @Order(230)
  protected String getConfiguredIconId() {
    return null;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(260)
  protected boolean getConfiguredAutoLoad() {
    return true;
  }

  /**
   * @return true: a filter is added to the listbox table that only accepts rows
   *         that are active or checked.<br>
   *         Default is true<br>
   *         Affects {@link ITable#getFilteredRows()}
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(270)
  protected boolean getConfiguredFilterActiveRows() {
    return false;
  }

  /**
   * @return true: a filter is added to the listbox table that only accepts
   *         checked rows<br>
   *         Default is false<br>
   *         Affects {@link ITable#getFilteredRows()}<br>
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(280)
  protected boolean getConfiguredFilterCheckedRows() {
    return false;
  }

  @Override
  protected double getConfiguredGridWeightY() {
    return 1.0;
  }

  private Class<? extends IFormField>[] getConfiguredFields() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    return ConfigurationUtility.sortFilteredClassesByOrderAnnotation(dca, IFormField.class);
  }

  /**
   * called before any lookup is performed
   */
  @ConfigOperation
  @Order(250)
  protected void execPrepareLookup(LookupCall call) throws ProcessingException {
  }

  /**
   * @param call
   *          that produced this result
   * @param result
   *          live list containing the result rows. Add, remove, set, replace
   *          and clear of entries in this list is supported
   */
  @ConfigOperation
  @Order(260)
  protected void execFilterLookupResult(LookupCall call, List<LookupRow> result) throws ProcessingException {
  }

  @ConfigOperation
  @Order(230)
  protected LookupRow[] execLoadTableData() throws ProcessingException {
    LookupRow[] data;
    // (1) get data by service
    if (getLookupCall() != null) {
      LookupCall call = SERVICES.getService(ILookupCallProvisioningService.class).newClonedInstance(getLookupCall(), new FormFieldProvisioningContext(AbstractListBox.this));
      prepareLookupCall(call);
      data = call.getDataByAll();
      data = filterLookupResult(call, data);
    }
    // (b) get data direct
    else {
      data = new LookupRow[0];
      data = filterLookupResult(null, data);
    }
    return data;
  }

  /**
   * interceptor is called after data was fetched from LookupCall and is adding
   * a table row for every LookupRow using IListBoxTable.createTableRow(row) and
   * ITable.addRows()
   * <p>
   * For most cases the override of just {@link #execLoadTableData()} is sufficient
   * 
   * <pre>
   * LookupRow[] data=execLoadTableData();
   * ITableRow[] rows=new ITableRow[data!=null? data.length : 0];
   * if(data!=null){
   *   for(int i=0; i{@code<}data.length; i++){
   *     rows[i]=createTableRow(data[i]);
   *   }
   * }
   * getTable().replaceRows(rows);
   * </pre>
   */
  @ConfigOperation
  @Order(240)
  protected void execPopulateTable() throws ProcessingException {
    LookupRow[] data = null;
    //sle Ticket 92'893: Listbox Master required. only run loadTable when master value is set
    if (!isMasterRequired() || getMasterValue() != null) {
      data = execLoadTableData();
    }
    ITableRow[] rows = new ITableRow[data != null ? data.length : 0];
    if (data != null) {
      for (int i = 0; i < data.length; i++) {
        rows[i] = getTableRowBuilder().createTableRow(data[i]);
      }
    }
    getTable().replaceRows(rows);
  }

  private Class<? extends ITable> getConfiguredTable() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    Class<? extends ITable>[] f = ConfigurationUtility.filterClasses(dca, ITable.class);
    if (f.length == 1) {
      return f[0];
    }
    else {
      for (Class<? extends ITable> c : f) {
        if (c.getDeclaringClass() != AbstractListBox.class) {
          return c;
        }
      }
      return null;
    }
  }

  @Override
  protected void execChangedMasterValue(Object newMasterValue) throws ProcessingException {
    setValue(null);
    loadListBoxData();
  }

  @Override
  protected void initConfig() {
    m_uiFacade = createUIFacade();
    m_fields = new IFormField[0];
    super.initConfig();
    setFilterActiveRows(getConfiguredFilterActiveRows());
    setFilterActiveRowsValue(TriState.TRUE);
    setFilterCheckedRows(getConfiguredFilterCheckedRows());
    setFilterCheckedRowsValue(getConfiguredFilterCheckedRows());
    try {
      m_table = ConfigurationUtility.newInnerInstance(this, getConfiguredTable());
      if (m_table instanceof AbstractTable) {
        ((AbstractTable) m_table).setContainerInternal(this);
      }
      updateActiveRowsFilter();
      updateCheckedRowsFilter();
      m_table.addTableListener(new TableAdapter() {
        @Override
        public void tableChanged(TableEvent e) {
          switch (e.getType()) {
            case TableEvent.TYPE_ROWS_SELECTED: {
              if (!getTable().isCheckable()) {
                syncTableToValue();
              }
              break;
            }
            case TableEvent.TYPE_ROWS_UPDATED: {
              if (getTable().isCheckable()) {
                syncTableToValue();
              }
              break;
            }
          }
        }
      });
      // default icon
      if (m_table.getDefaultIconId() == null && this.getConfiguredIconId() != null) {
        m_table.setDefaultIconId(this.getConfiguredIconId());
      }
      m_table.setEnabled(isEnabled());
    }
    catch (Exception e) {
      LOG.warn(null, e);
    }
    // lookup call
    if (getConfiguredLookupCall() != null) {
      try {
        LookupCall call = getConfiguredLookupCall().newInstance();
        setLookupCall(call);
      }
      catch (Exception e) {
        LOG.warn(null, e);
      }
    }
    // code type
    if (getConfiguredCodeType() != null) {
      setCodeTypeClass(getConfiguredCodeType());
    }
    // local property listener
    addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        if (m_table != null) {
          String name = e.getPropertyName();
          if (PROP_ENABLED.equals(name)) {
            m_table.setEnabled(isEnabled());
          }
          else if (PROP_FILTER_CHECKED_ROWS_VALUE.equals(name)) {
            updateCheckedRowsFilter();
          }
          else if (PROP_FILTER_ACTIVE_ROWS_VALUE.equals(name)) {
            updateActiveRowsFilter();
          }
        }
      }
    });
    // add fields
    ArrayList<IFormField> fieldList = new ArrayList<IFormField>();
    Class<? extends IFormField>[] fieldArray = getConfiguredFields();
    for (int i = 0; i < fieldArray.length; i++) {
      IFormField f;
      try {
        f = ConfigurationUtility.newInnerInstance(this, fieldArray[i]);
        fieldList.add(f);
      }// end try
      catch (Throwable t) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("field: " + fieldArray[i].getName(), t));
      }
    }
    for (IFormField f : fieldList) {
      f.setParentFieldInternal(this);
    }
    m_fields = fieldList.toArray(new IFormField[0]);
  }

  @SuppressWarnings("unchecked")
  public ListBoxFilterBox getListBoxFilterBox() {
    return getFieldByClass(ListBoxFilterBox.class);
  }

  @Override
  protected void initFieldInternal() throws ProcessingException {
    getTable().initTable();
    if (getConfiguredAutoLoad()) {
      try {
        setValueChangeTriggerEnabled(false);
        //
        loadListBoxData();
      }
      finally {
        setValueChangeTriggerEnabled(true);
      }
    }
    super.initFieldInternal();
  }

  @Override
  protected void disposeFieldInternal() {
    super.disposeFieldInternal();
    getTable().disposeTable();
  }

  public AbstractTableRowBuilder getTableRowBuilder() {
    return new P_TableRowBuilder();
  }

  protected IListBoxUIFacade createUIFacade() {
    return new P_ListBoxUIFacade();
  }

  @Override
  public IListBoxUIFacade getUIFacade() {
    return m_uiFacade;
  }

  @Override
  public final ITable getTable() {
    return m_table;
  }

  @Override
  public boolean isFilterCheckedRows() {
    return propertySupport.getPropertyBool(PROP_FILTER_CHECKED_ROWS);
  }

  @Override
  public void setFilterCheckedRows(boolean b) {
    propertySupport.setPropertyBool(PROP_FILTER_CHECKED_ROWS, b);
  }

  @Override
  public boolean getFilterCheckedRowsValue() {
    return propertySupport.getPropertyBool(PROP_FILTER_CHECKED_ROWS_VALUE);
  }

  @Override
  public void setFilterCheckedRowsValue(boolean b) {
    propertySupport.setPropertyBool(PROP_FILTER_CHECKED_ROWS_VALUE, b);
  }

  @Override
  public boolean isFilterActiveRows() {
    return propertySupport.getPropertyBool(PROP_FILTER_ACTIVE_ROWS);
  }

  @Override
  public void setFilterActiveRows(boolean b) {
    propertySupport.setPropertyBool(PROP_FILTER_ACTIVE_ROWS, b);
  }

  @Override
  public TriState getFilterActiveRowsValue() {
    return (TriState) propertySupport.getProperty(PROP_FILTER_ACTIVE_ROWS_VALUE);
  }

  @Override
  public void setFilterActiveRowsValue(TriState t) {
    if (t == null) {
      t = TriState.TRUE;
    }
    propertySupport.setProperty(PROP_FILTER_ACTIVE_ROWS_VALUE, t);
  }

  private void updateActiveRowsFilter() {
    try {
      getTable().setTableChanging(true);
      //
      if (m_activeRowsFilter != null) {
        getTable().removeRowFilter(m_activeRowsFilter);
        m_activeRowsFilter = null;
      }
      m_activeRowsFilter = new ActiveOrCheckedRowsFilter(getActiveColumnInternal(), getFilterActiveRowsValue());
      getTable().addRowFilter(m_activeRowsFilter);
    }
    finally {
      getTable().setTableChanging(false);
    }
  }

  private void updateCheckedRowsFilter() {
    try {
      getTable().setTableChanging(true);
      //
      if (m_checkedRowsFilter != null) {
        getTable().removeRowFilter(m_checkedRowsFilter);
        m_checkedRowsFilter = null;
      }
      if (getFilterCheckedRowsValue()) {
        m_checkedRowsFilter = new CheckedRowsFilter();
        getTable().addRowFilter(m_checkedRowsFilter);
      }
    }
    finally {
      getTable().setTableChanging(false);
    }
  }

  @Override
  public void loadListBoxData() throws ProcessingException {
    if (getTable() != null) {
      try {
        m_valueTableSyncActive = true;
        getTable().setTableChanging(true);
        //
        execPopulateTable();
      }
      finally {
        getTable().setTableChanging(false);
        m_valueTableSyncActive = false;
      }
      syncValueToTable();
    }
  }

  /**
   * do not use this internal method directly
   */
  @Override
  public final void prepareLookupCall(LookupCall call) throws ProcessingException {
    prepareLookupCallInternal(call);
    execPrepareLookup(call);
  }

  private LookupRow[] filterLookupResult(LookupCall call, LookupRow[] data) throws ProcessingException {
    ArrayList<LookupRow> result;
    if (data != null) {
      result = new ArrayList<LookupRow>(Arrays.asList(data));
    }
    else {
      result = new ArrayList<LookupRow>();
    }
    execFilterLookupResult(call, result);
    int len = 0;
    for (LookupRow r : result) {
      if (r != null) {
        len++;
      }
    }
    LookupRow[] a = new LookupRow[len];
    int index = 0;
    for (LookupRow r : result) {
      if (r != null) {
        a[index] = r;
        index++;
      }
    }
    return a;
  }

  /**
   * do not use this internal method directly
   */
  private void prepareLookupCallInternal(LookupCall call) {
    call.setActive(TriState.UNDEFINED);
    //when there is a master value defined in the original call, don't set it to null when no master value is available
    if (getMasterValue() != null || getLookupCall() == null || getLookupCall().getMaster() == null) {
      call.setMaster(getMasterValue());
    }
  }

  @Override
  public final LookupCall getLookupCall() {
    return m_lookupCall;
  }

  @Override
  public void setLookupCall(LookupCall call) {
    m_lookupCall = call;
  }

  @Override
  public Class<? extends ICodeType> getCodeTypeClass() {
    return m_codeTypeClass;
  }

  @Override
  public void setCodeTypeClass(Class<? extends ICodeType> codeTypeClass) {
    m_codeTypeClass = codeTypeClass;
    // create lookup service call
    m_lookupCall = null;
    if (m_codeTypeClass != null) {
      m_lookupCall = CodeLookupCall.newInstanceByService(m_codeTypeClass);
    }
  }

  @Override
  protected void valueChangedInternal() {
    super.valueChangedInternal();
    syncValueToTable();
  }

  @Override
  protected String formatValueInternal(T[] validValue) {
    if (validValue == null || validValue.length == 0) {
      return "";
    }
    StringBuffer b = new StringBuffer();
    ITableRow[] rows = getKeyColumnInternal().findRows(validValue);
    for (int i = 0; i < rows.length; i++) {
      if (i > 0) {
        b.append(", ");
      }
      b.append(getTextColumnInternal().getValue(rows[i]));
    }
    return b.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected T[] validateValueInternal(T[] rawValue) throws ProcessingException {
    T[] validValue = rawValue;
    //
    if (validValue != null && validValue.length == 0) {
      validValue = null;
    }
    ITable table = getTable();
    if (table != null && validValue != null) {
      if ((table.isCheckable() && !table.isMultiCheck()) || (!table.isCheckable() && !table.isMultiSelect())) {
        //only single value
        if (validValue.length > 1) {
          LOG.warn(getClass().getName() + " only accepts a single value. Got " + Arrays.toString(validValue) + ". Using only first value.");
          T[] newArray = (T[]) Array.newInstance(validValue.getClass().getComponentType(), 1);
          newArray[0] = validValue[0];
          validValue = newArray;
        }
      }
    }
    return validValue;
  }

  @Override
  public T getSingleValue() {
    T[] a = getValue();
    if (a != null && a.length > 0) {
      return a[0];
    }
    else {
      return null;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setSingleValue(T value) {
    if (value == null) {
      setValue(null);
    }
    else {
      T[] array = (T[]) Array.newInstance(getHolderType().getComponentType(), 1);
      array[0] = value;
      setValue(array);
    }
  }

  @Override
  public int getCheckedKeyCount() {
    T[] keys = getValue();
    if (keys != null) {
      return keys.length;
    }
    else {
      return 0;
    }
  }

  @Override
  public T getCheckedKey() {
    T[] a = getCheckedKeys();
    if (a != null && a.length > 0) {
      return a[0];
    }
    else {
      return null;
    }
  }

  @Override
  public T[] getCheckedKeys() {
    return getValue();
  }

  @Override
  public LookupRow getCheckedLookupRow() {
    LookupRow[] a = getCheckedLookupRows();
    if (a != null && a.length > 0) {
      return a[0];
    }
    else {
      return null;
    }
  }

  @Override
  public LookupRow[] getCheckedLookupRows() {
    LookupRow[] lookupRows = null;
    ITableRow[] tableRows = getTable().getCheckedRows();
    if (tableRows != null) {
      lookupRows = new LookupRow[tableRows.length];
      for (int i = 0; i < tableRows.length; i++) {
        ICell cell = tableRows[i].getCell(1);
        lookupRows[i] = new LookupRow(tableRows[i].getCellValue(0), cell.getText(), cell.getIconId(), cell.getTooltipText(), cell.getBackgroundColor(), cell.getForegroundColor(), cell.getFont(), cell.isEnabled());
      }
    }
    return lookupRows;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void checkKey(T key) {
    if (key == null) {
      checkKeys(null);
    }
    else {
      T[] array = (T[]) Array.newInstance(getHolderType().getComponentType(), 1);
      array[0] = key;
      checkKeys(array);
    }
  }

  @Override
  public void checkKeys(T[] keys) {
    setValue(keys);
  }

  @Override
  public void uncheckAllKeys() {
    checkKeys(null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public T[] getUncheckedKeys() {
    HashSet<T> set = new HashSet<T>();
    T[] a = getInitValue();
    if (a != null) {
      set.addAll(Arrays.asList(a));
    }
    a = getCheckedKeys();
    if (a != null) {
      set.removeAll(Arrays.asList(a));
    }
    T[] array = (T[]) Array.newInstance(getHolderType().getComponentType(), 0);
    a = set.toArray(array);
    if (a != null && a.length == 0) {
      a = null;
    }
    return a;
  }

  @Override
  public void checkAllKeys() {
    checkKeys(getKeyColumnInternal().getValues());
  }

  @Override
  public void checkAllActiveKeys() {
    checkKeys(getKeyColumnInternal().getValues(getActiveColumnInternal().findRows(true)));
  }

  @Override
  public void uncheckAllInactiveKeys() {
    checkKeys(getKeyColumnInternal().getValues(getActiveColumnInternal().findRows(false)));
  }

  @SuppressWarnings("unchecked")
  private IColumn<T> getKeyColumnInternal() {
    return getTable().getColumnSet().getColumn(0);
  }

  @SuppressWarnings("unchecked")
  private IColumn<String> getTextColumnInternal() {
    return getTable().getColumnSet().getColumn(1);
  }

  @SuppressWarnings("unchecked")
  private IColumn<Boolean> getActiveColumnInternal() {
    return getTable().getColumnSet().getColumn(2);
  }

  private void syncValueToTable() {
    if (m_valueTableSyncActive) {
      return;
    }
    try {
      m_valueTableSyncActive = true;
      getTable().setTableChanging(true);
      //
      T[] checkedKeys = getCheckedKeys();
      ITableRow[] checkedRows = getKeyColumnInternal().findRows(checkedKeys);
      for (ITableRow row : getTable().getRows()) {
        row.setChecked(false);
      }
      for (ITableRow row : checkedRows) {
        row.setChecked(true);
      }
      if (!getTable().isCheckable()) {
        getTable().selectRows(checkedRows, false);
      }
    }
    finally {
      getTable().setTableChanging(false);
      m_valueTableSyncActive = false;
    }
  }

  private void syncTableToValue() {
    if (m_valueTableSyncActive) {
      return;
    }
    try {
      m_valueTableSyncActive = true;
      m_table.setTableChanging(true);
      //
      ITableRow[] checkedRows;
      if (getTable().isCheckable()) {
        checkedRows = getTable().getCheckedRows();
      }
      else {
        checkedRows = getTable().getSelectedRows();
      }
      checkKeys(getKeyColumnInternal().getValues(checkedRows));
      if (!getTable().isCheckable()) {
        //checks follow selection
        for (ITableRow row : m_table.getRows()) {
          row.setChecked(row.isSelected());
        }
      }
    }
    finally {
      getTable().setTableChanging(false);
      m_valueTableSyncActive = false;
    }
    // check if row filter needs to change
    if (!m_table.getUIFacade().isUIProcessing()) {
      updateActiveRowsFilter();
    }
    updateCheckedRowsFilter();
  }

  /*
   * Implementation of ICompositeField
   */

  @Override
  @SuppressWarnings("unchecked")
  public <F extends IFormField> F getFieldByClass(final Class<F> c) {
    final Holder<IFormField> found = new Holder<IFormField>(IFormField.class);
    IFormFieldVisitor v = new IFormFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        if (field.getClass() == c) {
          found.setValue(field);
        }
        return found.getValue() == null;
      }
    };
    visitFields(v, 0);
    return (F) found.getValue();
  }

  @Override
  public IFormField getFieldById(final String id) {
    final Holder<IFormField> found = new Holder<IFormField>(IFormField.class);
    IFormFieldVisitor v = new IFormFieldVisitor() {
      @Override
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        if (field.getFieldId().equals(id)) {
          found.setValue(field);
        }
        return found.getValue() == null;
      }
    };
    visitFields(v, 0);
    return found.getValue();
  }

  @Override
  public <X extends IFormField> X getFieldById(final String id, final Class<X> type) {
    final Holder<X> found = new Holder<X>(type);
    IFormFieldVisitor v = new IFormFieldVisitor() {
      @Override
      @SuppressWarnings("unchecked")
      public boolean visitField(IFormField field, int level, int fieldIndex) {
        if (type.isAssignableFrom(field.getClass()) && field.getFieldId().equals(id)) {
          found.setValue((X) field);
        }
        return found.getValue() == null;
      }
    };
    visitFields(v, 0);
    return found.getValue();
  }

  @Override
  public int getFieldCount() {
    return m_fields.length;
  }

  @Override
  public int getFieldIndex(IFormField f) {
    for (int i = 0; i < m_fields.length; i++) {
      if (m_fields[i] == f) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public IFormField[] getFields() {
    IFormField[] a = new IFormField[m_fields.length];
    System.arraycopy(m_fields, 0, a, 0, a.length);
    return a;
  }

  @Override
  public boolean visitFields(IFormFieldVisitor visitor, int startLevel) {
    // myself
    if (!visitor.visitField(this, startLevel, 0)) {
      return false;
    }
    // children
    int index = 0;
    IFormField[] f = m_fields;
    for (int i = 0; i < f.length; i++) {
      if (f[i] instanceof ICompositeField) {
        if (!((ICompositeField) f[i]).visitFields(visitor, startLevel + 1)) {
          return false;
        }
      }
      else {
        if (!visitor.visitField(f[i], startLevel, index)) {
          return false;
        }
      }
      index++;
    }
    return true;
  }

  @Override
  public final int getGridColumnCount() {
    return 1;
  }

  @Override
  public final int getGridRowCount() {
    return 1;
  }

  @Override
  public void rebuildFieldGrid() {
    GridData gd = getListBoxFilterBox().getGridDataHints();
    gd.x = 0;
    gd.y = 0;
    getListBoxFilterBox().setGridDataInternal(gd);
  }

  @Order(1)
  public class ListBoxFilterBox extends AbstractListBoxFilterBox {
    @Override
    protected IListBox getListBox() {
      return AbstractListBox.this;
    }
  }

  public class DefaultListBoxTable extends AbstractTable {
    @Override
    protected boolean getConfiguredAutoResizeColumns() {
      return true;
    }

    @Override
    protected boolean getConfiguredHeaderVisible() {
      return false;
    }

    @Override
    protected boolean getConfiguredMultiSelect() {
      return false;
    }

    @Override
    protected boolean getConfiguredCheckable() {
      return true;
    }

    @SuppressWarnings("unchecked")
    public KeyColumn getKeyColumn() {
      return getColumnSet().getColumnByClass(KeyColumn.class);
    }

    @SuppressWarnings("unchecked")
    public TextColumn getTextColumn() {
      return getColumnSet().getColumnByClass(TextColumn.class);
    }

    @SuppressWarnings("unchecked")
    public ActiveColumn getActiveColumn() {
      return getColumnSet().getColumnByClass(ActiveColumn.class);
    }

    @Order(1)
    public class KeyColumn extends AbstractColumn<T> {
      @Override
      protected boolean getConfiguredPrimaryKey() {
        return true;
      }

      @Override
      protected boolean getConfiguredDisplayable() {
        return false;
      }

      @Override
      @SuppressWarnings("unchecked")
      public Class<T> getDataType() {
        return TypeCastUtility.getGenericsParameterClass(AbstractListBox.this.getClass(), IListBox.class);
      }
    }

    @Order(2)
    public class TextColumn extends AbstractStringColumn {

    }

    @Order(3)
    public class ActiveColumn extends AbstractBooleanColumn {
      @Override
      protected boolean getConfiguredDisplayable() {
        return false;
      }
    }
  }

  /*
   * UI Notifications
   */
  protected class P_ListBoxUIFacade implements IListBoxUIFacade {
  }

  private class P_TableRowBuilder extends AbstractTableRowBuilder {

    @Override
    protected ITableRow createEmptyTableRow() {
      return new TableRow(getTable().getColumnSet());
    }

    @SuppressWarnings("unchecked")
    @Override
    public ITableRow createTableRow(LookupRow dataRow) throws ProcessingException {
      TableRow tableRow = (TableRow) super.createTableRow(dataRow);
      // fill values to tableRow
      getKeyColumnInternal().setValue(tableRow, (T) dataRow.getKey());
      getTextColumnInternal().setValue(tableRow, dataRow.getText());
      getActiveColumnInternal().setValue(tableRow, dataRow.isActive());

      //enable/disabled row
      Cell cell = tableRow.getCellForUpdate(1);
      cell.setEnabled(dataRow.isEnabled());

      // hint for inactive codes
      if (!dataRow.isActive()) {
        if (cell.getFont() == null) {
          cell.setFont(FontSpec.parse("italic"));
        }
        getTextColumnInternal().setValue(tableRow, dataRow.getText() + " (" + ScoutTexts.get("InactiveState") + ")");
      }
      return tableRow;
    }
  }
}
