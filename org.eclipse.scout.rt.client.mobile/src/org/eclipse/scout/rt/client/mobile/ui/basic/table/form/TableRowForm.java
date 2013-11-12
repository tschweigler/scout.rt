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
package org.eclipse.scout.rt.client.mobile.ui.basic.table.form;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.mobile.ui.basic.table.form.fields.ColumnFieldBuilder;
import org.eclipse.scout.rt.client.mobile.ui.form.AbstractMobileForm;
import org.eclipse.scout.rt.client.mobile.ui.form.IActionFetcher;
import org.eclipse.scout.rt.client.mobile.ui.form.fields.button.AbstractBackButton;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.TableAdapter;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;
import org.eclipse.scout.rt.client.ui.basic.table.TableRowMapper;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.eclipse.scout.rt.client.ui.form.AbstractFormHandler;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.IValueField;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.service.SERVICES;

/**
 * Form which displays a {@link ITableRow} as fields. Only the fields belonging to editable columns are enabled.
 * 
 * @since 3.9.0
 */
public class TableRowForm extends AbstractMobileForm implements ITableRowForm {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(TableRowForm.class);

  private ITable m_table;
  private ITableRow m_row;
  private IFormField m_rowField;
  private ColumnFieldBuilder m_columnFieldBuilder;
  private Map<IColumn<?>, IFormField> m_columnFields;
  private P_TableListener m_tableListener;
  private TableRowMapper m_rowMapper;

  public TableRowForm(ITableRow row) throws ProcessingException {
    this(row, null);
  }

  /**
   * @param rowField
   *          if set this field will be displayed instead of the auto generated column fields.
   */
  public TableRowForm(ITableRow row, IFormField rowField) throws ProcessingException {
    super(false);
    m_row = row;
    m_rowField = rowField;
    m_table = row.getTable();
    m_columnFields = new HashMap<IColumn<?>, IFormField>();
    if (m_rowField == null) {
      m_columnFieldBuilder = createColumnFieldBuilder();
      m_columnFields = m_columnFieldBuilder.build(getTable().getColumns(), row);
    }
    callInitializer();

    m_tableListener = new P_TableListener();
    getTable().addTableListener(m_tableListener);
  }

  @Override
  protected void execDisposeForm() throws ProcessingException {
    getTable().removeTableListener(m_tableListener);
  }

  protected ColumnFieldBuilder createColumnFieldBuilder() {
    return new ColumnFieldBuilder();
  }

  public ITable getTable() {
    return m_table;
  }

  public ITableRow getRow() {
    return m_row;
  }

  @Override
  protected int getConfiguredDisplayHint() {
    return DISPLAY_HINT_VIEW;
  }

  @Override
  protected String getConfiguredDisplayViewId() {
    return VIEW_ID_PAGE_DETAIL;
  }

  @Override
  protected IActionFetcher createHeaderActionFetcher() {
    return new TableRowFormHeaderActionFetcher(this, getTable());
  }

  @Order(10.0f)
  public class MainBox extends AbstractGroupBox {

    @Override
    protected void injectFieldsInternal(List<IFormField> fieldList) {
      if (m_rowField != null) {
        fieldList.add(m_rowField);
      }
      else {
        fieldList.add(new P_ColumnFieldsGroupBox());
      }
      super.injectFieldsInternal(fieldList);
    }

    @Order(5)
    public class BackButton extends AbstractBackButton {

    }

  }

  @Override
  public void start() throws ProcessingException {
    startInternal(new FormHandler());
  }

  @Order(10.0f)
  public class FormHandler extends AbstractFormHandler {

    @SuppressWarnings("unchecked")
    @Override
    protected void execLoad() throws ProcessingException {
      m_rowMapper = new TableRowMapper(getRow());
      for (IColumn column : m_columnFields.keySet()) {
        IFormField field = m_columnFields.get(column);
        if (field instanceof IValueField) {
          IValueField<?> valueField = (IValueField<?>) field;
          m_rowMapper.addMapping(column, valueField);
          valueField.addPropertyChangeListener(new P_ValueFieldListener(column));
        }
      }
      m_rowMapper.exportRowData();
    }

  }

  private void handleRowDeleted() {
    try {
      doClose();
    }
    catch (ProcessingException e) {
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }
  }

  private void handleRowUpdated() {
    m_rowMapper.exportRowData();
  }

  /**
   * Listener to inform the column about the completion of an edit.
   */
  private class P_ValueFieldListener implements PropertyChangeListener {
    private IColumn<?> m_column;

    public P_ValueFieldListener(IColumn<?> column) {
      m_column = column;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      try {
        if (!m_column.isEditable()) {
          return;
        }

        IValueField<?> field = (IValueField) evt.getSource();
        if (IValueField.PROP_VALUE.equals(evt.getPropertyName())) {
          m_column.completeEdit(getRow(), field);
        }
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
    }

  }

  private class P_TableListener extends TableAdapter {

    @Override
    public void tableChanged(TableEvent e) {
      ITableRow[] rows = e.getRows();
      if (rows == null) {
        return;
      }

      for (ITableRow row : rows) {
        if (!getRow().equals(row)) {
          continue;
        }

        switch (e.getType()) {
          case TableEvent.TYPE_ALL_ROWS_DELETED:
          case TableEvent.TYPE_ROWS_DELETED:
            handleRowDeleted();
            break;
          case TableEvent.TYPE_ROWS_UPDATED:
            handleRowUpdated();
            break;
        }
      }
    }

  }

  private class P_ColumnFieldsGroupBox extends AbstractGroupBox {

    @Override
    protected void injectFieldsInternal(List<IFormField> fieldList) {
      for (IColumn column : getTable().getColumns()) {
        IFormField field = m_columnFields.get(column);
        if (field != null) {
          fieldList.add(field);
        }
      }

      super.injectFieldsInternal(fieldList);
    }

  }

}
