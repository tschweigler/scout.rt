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
package org.eclipse.scout.rt.client.ui.basic.table.columns;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.TypeCastUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.ConfigPropertyValue;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.annotations.Replace;
import org.eclipse.scout.commons.beans.AbstractPropertyObserver;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.ClientUIPreferences;
import org.eclipse.scout.rt.client.ui.basic.cell.Cell;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.client.ui.basic.table.ColumnSet;
import org.eclipse.scout.rt.client.ui.basic.table.HeaderCell;
import org.eclipse.scout.rt.client.ui.basic.table.IHeaderCell;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.columnfilter.ITableColumnFilterManager;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.AbstractPageWithTable;
import org.eclipse.scout.rt.client.ui.form.AbstractForm;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractValueField;
import org.eclipse.scout.rt.client.ui.form.fields.GridData;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.IValueField;
import org.eclipse.scout.rt.client.ui.form.fields.ParsingFailedStatus;
import org.eclipse.scout.rt.client.ui.form.fields.tablefield.AbstractTableField;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.data.basic.FontSpec;
import org.eclipse.scout.rt.shared.data.form.AbstractFormData;
import org.eclipse.scout.rt.shared.services.common.security.IAccessControlService;
import org.eclipse.scout.service.SERVICES;

public abstract class AbstractColumn<T> extends AbstractPropertyObserver implements IColumn<T> {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractColumn.class);

  // DO NOT init members, this has the same effect as if they were set AFTER
  // initConfig()
  private ITable m_table;
  private final HeaderCell m_headerCell;
  private boolean m_primaryKey;
  private boolean m_summary;
  /**
   * A column is presented to the user when it is displayable AND visible this
   * column is visible to the user only used when displayable=true
   */
  private boolean m_visibleProperty;
  private boolean m_visibleGranted;
  private int m_initialWidth;
  private boolean m_initialVisible;
  private int m_initialSortIndex;
  private boolean m_initialSortAscending;
  private boolean m_initialAlwaysIncludeSortAtBegin;
  private boolean m_initialAlwaysIncludeSortAtEnd;
  /**
   * Used for mutable tables to keep last valid value per row and column.
   */
  private Map<ITableRow, T> m_validatedValues;

  public AbstractColumn() {
    m_headerCell = new HeaderCell();
    initConfig();
    propertySupport.addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        // force decoration of rows on property change.
        // This is important to recalculate editability of editable cells.
        ITable table = getTable();
        if (table != null) {
          table.updateAllRows();
        }
      }
    });

    clearValidatedValues();
  }

  public final void clearValidatedValues() {
    m_validatedValues = new HashMap<ITableRow, T>();
  }

  protected Map<String, Object> getPropertiesMap() {
    return propertySupport.getPropertiesMap();
  }

  /*
   * Configuration
   */

  /**
   * Configures the visibility of this column. If the column must be visible for the user, it must be displayable too
   * (see {@link #getConfiguredDisplayable()}).
   * <p>
   * Subclasses can override this method. Default is {@code true}.
   * 
   * @return {@code true} if this column is visible, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(10)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredVisible() {
    return true;
  }

  /**
   * Configures the header text of this column.
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return Header text of this column.
   */
  @ConfigProperty(ConfigProperty.TEXT)
  @Order(20)
  @ConfigPropertyValue("null")
  protected String getConfiguredHeaderText() {
    return null;
  }

  /**
   * Configures the header tooltip of this column.
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return Tooltip of this column.
   */
  @ConfigProperty(ConfigProperty.TEXT)
  @Order(30)
  @ConfigPropertyValue("null")
  protected String getConfiguredHeaderTooltipText() {
    return null;
  }

  /**
   * Configures the color of this column header text. The color is represented by the HEX value (e.g. FFFFFF).
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return Foreground color HEX value of this column header text.
   */
  @ConfigProperty(ConfigProperty.COLOR)
  @Order(40)
  @ConfigPropertyValue("null")
  protected String getConfiguredHeaderForegroundColor() {
    return null;
  }

  /**
   * Configures the background color of this column header. The color is represented by the HEX value (e.g. FFFFFF).
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return Background color HEX value of this column header.
   */
  @ConfigProperty(ConfigProperty.COLOR)
  @Order(50)
  @ConfigPropertyValue("null")
  protected String getConfiguredHeaderBackgroundColor() {
    return null;
  }

  /**
   * Configures the font of this column header text. See {@link FontSpec#parse(String)} for the appropriate format.
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return Font of this column header text.
   */
  @ConfigProperty(ConfigProperty.STRING)
  @Order(60)
  @ConfigPropertyValue("null")
  protected String getConfiguredHeaderFont() {
    return null;
  }

  /**
   * Configures the width of this column. The width of a column is represented by an {@code int}. If the table's auto
   * resize flag is set (see {@link AbstractTable#getConfiguredAutoResizeColumns()} ), the ratio of the column widths
   * determines the real column width. If the flag is not set, the column's width is represented by the configured
   * width.
   * <p>
   * Subclasses can override this method. Default is {@code 60}.
   * 
   * @return Width of this column.
   */
  @ConfigProperty(ConfigProperty.INTEGER)
  @Order(70)
  @ConfigPropertyValue("60")
  protected int getConfiguredWidth() {
    return 60;
  }

  /**
   * Configures whether the column width is fixed, meaning that it is not changed by resizing/auto-resizing
   * and cannot be resized by the user.
   * If <code>true</code>, the configured width is fixed.
   * Defaults to <code>false</code>.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(75)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredFixedWidth() {
    return false;
  }

  /**
   * Configures whether the column is displayable or not. A non-displayable column is always invisible for the user. A
   * displayable column may be visible for a user, depending on {@link #getConfiguredVisible()}.
   * <p>
   * Subclasses can override this method. Default is {@code true}.
   * 
   * @return {@code true} if this column is displayable, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(80)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredDisplayable() {
    return true;
  }

  /**
   * Configures whether this column value belongs to the primary key of the surrounding table. The table's primary key
   * might consist of several columns. The primary key can be used to find the appropriate row by calling
   * {@link AbstractTable#findRowByKey(Object[])}.
   * <p>
   * Subclasses can override this method. Default is {@code false}.
   * 
   * @return {@code true} if this column value belongs to the primary key of the surrounding table, {@code false}
   *         otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(90)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredPrimaryKey() {
    return false;
  }

  /**
   * Configures whether this column is editable or not. A user might directly modify the value of an editable column. A
   * non-editable column is read-only.
   * <p>
   * Subclasses can override this method. Default is {@code false}.
   * 
   * @return {@code true} if this column is editable, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(95)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredEditable() {
    return false;
  }

  /**
   * Configures whether this column is a summary column. Summary columns are used in case of a table with children. The
   * label of the child node is based on the value of the summary columns. See {@link ITable#getSummaryCell(ITableRow)}
   * for more information.
   * <p>
   * Subclasses can override this method. Default is {@code false}.
   * 
   * @return {@code true} if this column is a summary column, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(100)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredSummary() {
    return false;
  }

  /**
   * Configures the color of this column text (except color of header text, see
   * {@link #getConfiguredHeaderForegroundColor()}). The color is represented by the HEX value (e.g. FFFFFF).
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return Foreground color HEX value of this column text.
   */
  @ConfigProperty(ConfigProperty.COLOR)
  @Order(110)
  @ConfigPropertyValue("null")
  protected String getConfiguredForegroundColor() {
    return null;
  }

  /**
   * Configures the background color of this column (except background color of header, see
   * {@link #getConfiguredHeaderBackgroundColor()}. The color is represented by the HEX value (e.g. FFFFFF).
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return Background color HEX value of this column.
   */
  @ConfigProperty(ConfigProperty.COLOR)
  @Order(120)
  @ConfigPropertyValue("null")
  protected String getConfiguredBackgroundColor() {
    return null;
  }

  /**
   * Configures the font of this column text (except header text, see {@link #getConfiguredHeaderFont()}). See
   * {@link FontSpec#parse(String)} for the appropriate format.
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return Font of this column text.
   */
  @ConfigProperty(ConfigProperty.STRING)
  @Order(130)
  @ConfigPropertyValue("null")
  protected String getConfiguredFont() {
    return null;
  }

  /**
   * Configures the sort index of this column. A sort index {@code < 0} means that the column is not considered for
   * sorting. For a column to be considered for sorting, the sort index must be {@code >= 0}. Several columns
   * might have set a sort index. Sorting starts with the column having the the lowest sort index ({@code >= 0}).
   * <p>
   * Subclasses can override this method. Default is {@code -1}.
   * 
   * @return Sort index of this column.
   */
  @ConfigProperty(ConfigProperty.INTEGER)
  @Order(140)
  @ConfigPropertyValue("-1")
  protected int getConfiguredSortIndex() {
    return -1;
  }

  /**
   * Configures the view order of this column. The view order determines the order in which the columns appear. The view
   * order of column with no view order configured ({@code < 0}) is initialized based on the order annotation of the
   * column class.
   * <p>
   * Subclasses can override this method. Default is {@code -1}.
   * 
   * @return View order of this column.
   */
  @ConfigProperty(ConfigProperty.DOUBLE)
  @Order(145)
  @ConfigPropertyValue("-1")
  protected double getConfiguredViewOrder() {
    return -1;
  }

  /**
   * Configures whether this column is sorted ascending or descending. For a column to be sorted at all, a sort index
   * must be set (see {@link #getConfiguredSortIndex()}).
   * <p>
   * Subclasses can override this method. Default is {@code true}.
   * 
   * @return {@code true} if this column is sorted ascending, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(150)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredSortAscending() {
    return true;
  }

  /**
   * Configures whether this column is always included for sort at begin, independent of a sort change by the user. If
   * set to {@code true}, the sort index (see {@link #getConfiguredSortIndex()}) must be set.
   * <p>
   * Subclasses can override this method. Default is {@code false}.
   * 
   * @return {@code true} if this column is always included for sort at begin, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(160)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredAlwaysIncludeSortAtBegin() {
    return false;
  }

  /**
   * Configures whether this column is always included for sort at end, independent of a sort change by the user. If set
   * to {@code true}, the sort index (see {@link #getConfiguredSortIndex()}) must be set.
   * <p>
   * Subclasses can override this method. Default is {@code false}.
   * 
   * @return {@code true} if this column is always included for sort at end, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(170)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredAlwaysIncludeSortAtEnd() {
    return false;
  }

  /**
   * Configures the horizontal alignment of text inside this column (including header text).
   * <p>
   * Subclasses can override this method. Default is {@code -1} (left alignment).
   * 
   * @return {@code -1} for left, {@code 0} for center and {@code 1} for right alignment.
   */
  @ConfigProperty(ConfigProperty.INTEGER)
  @Order(180)
  @ConfigPropertyValue("-1")
  protected int getConfiguredHorizontalAlignment() {
    return -1;
  }

  /**
   * Configures whether the column width is auto optimized. If true: Whenever the table content changes, the optimized
   * column width is automatically calculated so that all column content is displayed without cropping.
   * <p>
   * This may display a horizontal scroll bar on the table.
   * <p>
   * This feature is not supported in SWT and RWT since SWT does not offer such an API method.
   * <p>
   * Subclasses can override this method. Default is {@code false}.
   * 
   * @return {@code true} if this column width is auto optimized, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(190)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredAutoOptimizeWidth() {
    return false;
  }

  /**
   * Provides a documentation text or description of this column. The text is intended to be included in external
   * documentation. This method is typically processed by a documentation generation tool or similar.
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return a documentation text, suitable to be included in external documents
   */
  @ConfigProperty(ConfigProperty.DOC)
  @Order(200)
  @ConfigPropertyValue("null")
  protected String getConfiguredDoc() {
    return null;
  }

  /**
   * Configures whether this column value is mandatory / required. This only affects editable columns (see
   * {@link #getConfiguredEditable()} ).
   * <p>
   * Subclasses can override this method. Default is {@code false}.
   * 
   * @return {@code true} if this column value is mandatory, {@code false} otherwise.
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(210)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredMandatory() {
    return false;
  }

  /**
   * Called after this column has been added to the column set of the surrounding table. This method may execute
   * additional initialization for this column (e.g. register listeners).
   * <p>
   * Do not load table data here, this should be done lazily in {@link AbstractPageWithTable#execLoadTableData()},
   * {@link AbstractTableField#reloadTableData()} or via {@link AbstractForm#importFormData(AbstractFormData)}.
   * <p>
   * Subclasses can override this method. The default does nothing.
   * 
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(10)
  protected void execInitColumn() throws ProcessingException {
  }

  /**
   * Called when the surrounding table is disposed. This method may execute additional cleanup.
   * <p>
   * Subclasses can override this method. The default does nothing.
   * 
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(15)
  protected void execDisposeColumn() throws ProcessingException {
  }

  /**
   * Parse is the process of transforming an arbitrary object to the correct type or throwing an exception.
   * <p>
   * see also {@link #execValidateValue(ITableRow, Object)}
   * <p>
   * Subclasses can override this method. The default calls {@link #parseValueInternal(ITableRow, Object)}.
   * 
   * @param row
   *          Table row for which to parse the raw value.
   * @param rawValue
   *          Raw value to parse.
   * @return Value in correct type, derived from rawValue.
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(20)
  protected T/* validValue */execParseValue(ITableRow row, Object rawValue) throws ProcessingException {
    return parseValueInternal(row, rawValue);
  }

  /**
   * Validate is the process of checking range, domain, bounds, correctness etc. of an already correctly typed value or
   * throwing an exception.
   * <p>
   * see also {@link #execParseValue(ITableRow, Object)}
   * <p>
   * Subclasses can override this method. The default calls {@link #validateValueInternal(ITableRow, Object)}.
   * 
   * @param row
   *          Table row for which to validate the raw value.
   * @param rawValue
   *          Already parsed raw value to validate.
   * @return Validated value
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(30)
  protected T/* validValue */execValidateValue(ITableRow row, T rawValue) throws ProcessingException {
    return validateValueInternal(row, rawValue);
  }

  /**
   * Called when decorating the table cell. This method may add additional decorations to the table cell.
   * <p>
   * Subclasses can override this method. The default does nothing.
   * 
   * @param cell
   *          Cell to decorate.
   * @param row
   *          Table row of cell.
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(40)
  protected void execDecorateCell(Cell cell, ITableRow row) throws ProcessingException {
  }

  /**
   * Called when decorating the table header cell. This method may add additional decorations to the table header cell.
   * <p>
   * Subclasses can override this method. The default does nothing.
   * 
   * @param cell
   *          Header cell to decorate.
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(50)
  protected void execDecorateHeaderCell(HeaderCell cell) throws ProcessingException {
  }

  /**
   * Only called if {@link #getConfiguredEditable()} is true and cell, row and table are enabled. Use this method only
   * for dynamic checks of editablility, otherwise use {@link #getConfiguredEditable()}.
   * <p>
   * Subclasses can override this method. Default is {@code true}.
   * 
   * @param row
   *          for which to determine editability dynamically.
   * @return {@code true} if the cell (row, column) is editable, {@code false} otherwise.
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(60)
  protected boolean execIsEditable(ITableRow row) throws ProcessingException {
    return true;
  }

  /**
   * Prepares the editing of a cell in the table.
   * <p>
   * Cell editing is canceled (normally by typing escape) or saved (normally by clicking another cell, typing enter).
   * <p>
   * When saved, the method {@link #completeEdit(ITableRow, IFormField)} /
   * {@link #execCompleteEdit(ITableRow, IFormField)} is called on this column.
   * <p>
   * Subclasses can override this method. The default returns an appropriate field based on the column data type.
   * 
   * @param row
   *          on which editing occurs
   * @return a field for editing or null to install an empty cell editor.
   */
  @SuppressWarnings("unchecked")
  @ConfigOperation
  @Order(61)
  protected IFormField execPrepareEdit(ITableRow row) throws ProcessingException {
    IFormField f = prepareEditInternal(row);
    if (f != null) {
      f.setLabelVisible(false);
      GridData gd = f.getGridDataHints();
      // apply horizontal alignment of column to respective editor field
      gd.horizontalAlignment = getHorizontalAlignment();
      f.setGridDataHints(gd);
      if (f instanceof IValueField<?>) {
        ((IValueField<T>) f).setValue(getValue(row));
      }
      f.markSaved();
    }
    return f;
  }

  /**
   * Completes editing of a cell.
   * <p>
   * Subclasses can override this method. The default calls {@link #applyValueInternal(ITableRow, Object)} and delegates
   * to {@link #execParseValue(ITableRow, Object)} and {@link #execValidateValue(ITableRow, Object)}.
   * 
   * @param row
   *          on which editing occurred.
   * @param editingField
   *          Field which was used to edit cell value (as returned by {@link #execPrepareEdit(ITableRow)}).
   * @throws ProcessingException
   */
  @ConfigOperation
  @Order(62)
  protected void execCompleteEdit(ITableRow row, IFormField editingField) throws ProcessingException {
    if (editingField instanceof IValueField) {
      IValueField v = (IValueField) editingField;
      if (v.isSaveNeeded() || editingField.getErrorStatus() != null || row.getCell(this).getErrorStatus() != null) {
        T parsedValue = parseValue(row, v.getValue());
        applyValueInternal(row, parsedValue);
        validateColumnValue(row, editingField);
        if (editingField.isContentValid()) {
          m_validatedValues.put(row, parsedValue);
        }
        persistRowChange(row);
      }
    }
  }

  /**
   * <p>
   * Updates the value of the cell with the given value.
   * </p>
   * <p>
   * Thereby, if sorting is enabled on table, it is temporarily suspended to prevent rows from scampering.
   * </p>
   * 
   * @param row
   * @param newValue
   * @throws ProcessingException
   */
  protected void applyValueInternal(ITableRow row, T newValue) throws ProcessingException {
    if (!getTable().isSortEnabled()) {
      setValue(row, newValue);
    }
    else {
      // suspend sorting to prevent rows from scampering
      try {
        getTable().setSortEnabled(false);
        setValue(row, newValue);
      }
      finally {
        getTable().setSortEnabled(true);
      }
    }
  }

  protected void initConfig() {
    setAutoOptimizeWidth(getConfiguredAutoOptimizeWidth());
    m_visibleGranted = true;
    m_headerCell.setText(getConfiguredHeaderText());
    if (getConfiguredHeaderTooltipText() != null) {
      m_headerCell.setTooltipText(getConfiguredHeaderTooltipText());
    }
    if (getConfiguredHeaderForegroundColor() != null) {
      m_headerCell.setForegroundColor((getConfiguredHeaderForegroundColor()));
    }
    if (getConfiguredHeaderBackgroundColor() != null) {
      m_headerCell.setBackgroundColor((getConfiguredHeaderBackgroundColor()));
    }
    if (getConfiguredHeaderFont() != null) {
      m_headerCell.setFont(FontSpec.parse(getConfiguredHeaderFont()));
    }
    m_headerCell.setHorizontalAlignment(getConfiguredHorizontalAlignment());
    setHorizontalAlignment(getConfiguredHorizontalAlignment());

    setDisplayable(getConfiguredDisplayable());
    setVisible(getConfiguredVisible());

    setInitialWidth(getConfiguredWidth());
    setInitialVisible(getConfiguredVisible());
    setInitialSortIndex(getConfiguredSortIndex());
    setInitialSortAscending(getConfiguredSortAscending());
    setInitialAlwaysIncludeSortAtBegin(getConfiguredAlwaysIncludeSortAtBegin());
    setInitialAlwaysIncludeSortAtEnd(getConfiguredAlwaysIncludeSortAtEnd());
    //
    double viewOrder = getConfiguredViewOrder();
    if (viewOrder < 0) {
      if (getClass().isAnnotationPresent(Order.class)) {
        Order order = (Order) getClass().getAnnotation(Order.class);
        viewOrder = order.value();
      }
    }
    setViewOrder(viewOrder);
    //
    setWidth(getConfiguredWidth());
    setFixedWidth(getConfiguredFixedWidth());
    m_primaryKey = getConfiguredPrimaryKey();
    m_summary = getConfiguredSummary();
    setEditable(getConfiguredEditable());
    setVisibleColumnIndexHint(-1);
    if (getConfiguredForegroundColor() != null) {
      setForegroundColor((getConfiguredForegroundColor()));
    }
    if (getConfiguredBackgroundColor() != null) {
      setBackgroundColor((getConfiguredBackgroundColor()));
    }
    if (getConfiguredFont() != null) {
      setFont(FontSpec.parse(getConfiguredFont()));
    }
  }

  /*
   * Runtime
   */

  @Override
  public void initColumn() throws ProcessingException {
    ClientUIPreferences env = ClientUIPreferences.getInstance();
    setVisible(env.getTableColumnVisible(this, m_visibleProperty));
    setWidth(env.getTableColumnWidth(this, getWidth()));
    setVisibleColumnIndexHint(env.getTableColumnViewIndex(this, getVisibleColumnIndexHint()));
    //
    execInitColumn();
  }

  @Override
  public void disposeColumn() throws ProcessingException {
    execDisposeColumn();
  }

  @Override
  public void setMandatory(boolean mandatory) {
    propertySupport.setPropertyBool(IFormField.PROP_MANDATORY, mandatory);
    validateColumnValues();
  }

  @Override
  public boolean isMandatory() {
    return propertySupport.getPropertyBool(IFormField.PROP_MANDATORY);
  }

  @Override
  public boolean isInitialVisible() {
    return m_initialVisible;
  }

  @Override
  public void setInitialVisible(boolean b) {
    m_initialVisible = b;
  }

  @Override
  public int getInitialSortIndex() {
    return m_initialSortIndex;
  }

  @Override
  public void setInitialSortIndex(int i) {
    m_initialSortIndex = i;
  }

  @Override
  public boolean isInitialSortAscending() {
    return m_initialSortAscending;
  }

  @Override
  public void setInitialSortAscending(boolean b) {
    m_initialSortAscending = b;
  }

  @Override
  public boolean isInitialAlwaysIncludeSortAtBegin() {
    return m_initialAlwaysIncludeSortAtBegin;
  }

  @Override
  public void setInitialAlwaysIncludeSortAtBegin(boolean b) {
    m_initialAlwaysIncludeSortAtBegin = b;
  }

  @Override
  public boolean isInitialAlwaysIncludeSortAtEnd() {
    return m_initialAlwaysIncludeSortAtEnd;
  }

  @Override
  public void setInitialAlwaysIncludeSortAtEnd(boolean b) {
    m_initialAlwaysIncludeSortAtEnd = b;
  }

  /**
   * controls the displayable property of the column
   */
  @Override
  public void setVisiblePermission(Permission p) {
    boolean b;
    if (p != null) {
      b = SERVICES.getService(IAccessControlService.class).checkPermission(p);
    }
    else {
      b = true;
    }
    setVisibleGranted(b);
  }

  @Override
  public boolean isVisibleGranted() {
    return m_visibleGranted;
  }

  @Override
  public void setVisibleGranted(boolean b) {
    m_visibleGranted = b;
    calculateVisible();
  }

  @Override
  public ITable getTable() {
    return m_table;
  }

  /**
   * do not use this internal method
   */
  public void setTableInternal(ITable table) {
    m_table = table;
  }

  @Override
  public int getColumnIndex() {
    return m_headerCell.getColumnIndex();
  }

  @Override
  public String getColumnId() {
    Class<?> c = getClass();
    while (c.isAnnotationPresent(Replace.class)) {
      c = c.getSuperclass();
    }
    String s = c.getSimpleName();
    if (s.endsWith("Column")) {
      s = s.replaceAll("Column$", "");
    }
    //do not remove other suffixes
    return s;
  }

  @Override
  public T getValue(ITableRow r) {
    T validatedValue = m_validatedValues.get(r);
    if (validatedValue == null) {
      validatedValue = getValueInternal(r);
    }
    m_validatedValues.put(r, validatedValue);
    return validatedValue;
  }

  @SuppressWarnings("unchecked")
  protected T getValueInternal(ITableRow r) {
    return (r != null) ? (T) r.getCellValue(getColumnIndex()) : null;
  }

  @Override
  public T getValue(int rowIndex) {
    return getValue(getTable().getRow(rowIndex));
  }

  @Override
  public void setValue(int rowIndex, T rawValue) throws ProcessingException {
    setValue(getTable().getRow(rowIndex), rawValue);
  }

  @Override
  public void setValue(ITableRow r, T rawValue) throws ProcessingException {
    T newValue = validateValue(r, rawValue);
    r.setCellValue(getColumnIndex(), newValue);
  }

  @Override
  public void fill(T rawValue) throws ProcessingException {
    ITableRow[] rows = getTable().getRows();
    for (ITableRow row : rows) {
      setValue(row, rawValue);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<T> getDataType() {
    return TypeCastUtility.getGenericsParameterClass(getClass(), IColumn.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public T[] getValues() {
    T[] values = (T[]) Array.newInstance(getDataType(), m_table.getRowCount());
    for (int i = 0, ni = m_table.getRowCount(); i < ni; i++) {
      values[i] = getValue(m_table.getRow(i));
    }
    return values;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T[] getValues(ITableRow[] rows) {
    T[] values = (T[]) Array.newInstance(getDataType(), rows.length);
    for (int i = 0; i < rows.length; i++) {
      values[i] = getValue(rows[i]);
    }
    return values;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T[] getSelectedValues() {
    ITableRow[] rows = m_table.getSelectedRows();
    T[] values = (T[]) Array.newInstance(getDataType(), rows.length);
    for (int i = 0; i < rows.length; i++) {
      values[i] = getValue(rows[i]);
    }
    return values;
  }

  @Override
  public T getSelectedValue() {
    ITableRow row = m_table.getSelectedRow();
    if (row != null) {
      return getValue(row);
    }
    else {
      return null;
    }
  }

  @Override
  public String getDisplayText(ITableRow r) {
    return r.getCell(getColumnIndex()).getText();
  }

  @Override
  public String[] getDisplayTexts() {
    String[] values = new String[m_table.getRowCount()];
    for (int i = 0, ni = m_table.getRowCount(); i < ni; i++) {
      values[i] = getDisplayText(m_table.getRow(i));
    }
    return values;
  }

  @Override
  public String getSelectedDisplayText() {
    ITableRow row = m_table.getSelectedRow();
    if (row != null) {
      return getDisplayText(row);
    }
    else {
      return null;
    }
  }

  @Override
  public String[] getSelectedDisplayTexts() {
    ITableRow[] rows = m_table.getSelectedRows();
    String[] values = new String[rows.length];
    for (int i = 0; i < rows.length; i++) {
      values[i] = getDisplayText(rows[i]);
    }
    return values;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T[] getInsertedValues() {
    ITableRow[] rows = m_table.getInsertedRows();
    T[] values = (T[]) Array.newInstance(getDataType(), rows.length);
    for (int i = 0; i < rows.length; i++) {
      values[i] = getValue(rows[i]);
    }
    return values;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T[] getUpdatedValues() {
    ITableRow[] rows = m_table.getUpdatedRows();
    T[] values = (T[]) Array.newInstance(getDataType(), rows.length);
    for (int i = 0; i < rows.length; i++) {
      values[i] = getValue(rows[i]);
    }
    return values;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T[] getDeletedValues() {
    ITableRow[] rows = m_table.getDeletedRows();
    T[] values = (T[]) Array.newInstance(getDataType(), rows.length);
    for (int i = 0; i < rows.length; i++) {
      values[i] = getValue(rows[i]);
    }
    return values;
  }

  @Override
  public ITableRow[] findRows(T[] values) {
    ArrayList<ITableRow> rowList = new ArrayList<ITableRow>();
    if (values != null) {
      for (int i = 0; i < values.length; i++) {
        ITableRow row = findRow(values[i]);
        if (row != null) {
          rowList.add(row);
        }
      }
    }
    return rowList.toArray(new ITableRow[0]);
  }

  @Override
  public ITableRow[] findRows(T value) {
    ArrayList<ITableRow> rowList = new ArrayList<ITableRow>();
    for (int i = 0, ni = m_table.getRowCount(); i < ni; i++) {
      ITableRow row = m_table.getRow(i);
      if (CompareUtility.equals(value, getValue(row))) {
        rowList.add(row);
      }
    }
    return rowList.toArray(new ITableRow[0]);
  }

  @Override
  public ITableRow findRow(T value) {
    for (int i = 0, ni = m_table.getRowCount(); i < ni; i++) {
      ITableRow row = m_table.getRow(i);
      if (CompareUtility.equals(value, getValue(row))) {
        return row;
      }
    }
    return null;
  }

  @Override
  public boolean contains(T value) {
    for (int i = 0, ni = m_table.getRowCount(); i < ni; i++) {
      ITableRow row = m_table.getRow(i);
      if (CompareUtility.equals(value, getValue(row))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsDuplicateValues() {
    return new HashSet<T>(Arrays.asList(getValues())).size() < getValues().length;
  }

  @Override
  public boolean isEmpty() {
    if (m_table != null) {
      for (int i = 0, ni = m_table.getRowCount(); i < ni; i++) {
        Object value = getValue(m_table.getRow(i));
        if (value != null) {
          return false;
        }
      }
    }
    return true;
  }

  public void setColumnIndexInternal(int index) {
    m_headerCell.setColumnIndexInternal(index);
  }

  @Override
  public boolean isSortActive() {
    return getHeaderCell().isSortActive();
  }

  @Override
  public boolean isSortExplicit() {
    return getHeaderCell().isSortExplicit();
  }

  @Override
  public boolean isSortAscending() {
    return getHeaderCell().isSortAscending();
  }

  @Override
  public boolean isSortPermanent() {
    return getHeaderCell().isSortPermanent();
  }

  @Override
  public int getSortIndex() {
    ITable table = getTable();
    if (table != null) {
      ColumnSet cs = table.getColumnSet();
      if (cs != null) {
        return cs.getSortColumnIndex(this);
      }
    }
    return -1;
  }

  @Override
  public boolean isColumnFilterActive() {
    ITable table = getTable();
    if (table != null) {
      ITableColumnFilterManager m = table.getColumnFilterManager();
      if (m != null) {
        return m.getFilter(this) != null;
      }
    }
    return false;
  }

  /**
   * sorting of rows based on this column<br>
   * default: compare objects by Comparable interface or use value
   */
  @Override
  @SuppressWarnings("unchecked")
  public int compareTableRows(ITableRow r1, ITableRow r2) {
    int c;
    T o1 = getValue(r1);
    T o2 = getValue(r2);
    if (o1 == null && o2 == null) {
      c = 0;
    }
    else if (o1 == null) {
      c = -1;
    }
    else if (o2 == null) {
      c = 1;
    }
    else if ((o1 instanceof Comparable) && (o2 instanceof Comparable)) {
      c = ((Comparable) o1).compareTo(o2);
    }
    else {
      c = StringUtility.compareIgnoreCase(o1.toString(), o2.toString());
    }
    return c;
  }

  @Override
  public final T/* validValue */parseValue(ITableRow row, Object rawValue) throws ProcessingException {
    T parsedValue = execParseValue(row, rawValue);
    return validateValue(row, parsedValue);
  }

  /**
   * do not use or override this internal method<br>
   * subclasses perform specific value validations here and set the
   * default textual representation of the value
   */
  protected T/* validValue */parseValueInternal(ITableRow row, Object rawValue) throws ProcessingException {
    return TypeCastUtility.castValue(rawValue, getDataType());
  }

  @Override
  public T/* validValue */validateValue(ITableRow row, T rawValue) throws ProcessingException {
    return execValidateValue(row, rawValue);
  }

  /**
   * do not use or override this internal method<br>
   * subclasses perform specific value validations here and set the
   * default textual representation of the value
   */
  protected T/* validValue */validateValueInternal(ITableRow row, T rawValue) throws ProcessingException {
    return rawValue;
  }

  @Override
  public final IFormField prepareEdit(ITableRow row) throws ProcessingException {
    ITable table = getTable();
    if (table == null || !this.isCellEditable(row)) {
      return null;
    }
    IFormField f = execPrepareEdit(row);
    if (f != null) {
      f.setLabelVisible(false);
      GridData gd = f.getGridDataHints();
      gd.weightY = 1;
      f.setGridDataHints(gd);
    }
    return f;
  }

  /**
   * do not use or override this internal method
   */
  protected IFormField prepareEditInternal(ITableRow row) throws ProcessingException {
    AbstractValueField<T> f = new AbstractValueField<T>() {
      @Override
      protected void initConfig() {
        super.initConfig();
        propertySupport.putPropertiesMap(AbstractColumn.this.propertySupport.getPropertiesMap());
      }
    };
    return f;
  }

  /**
   * Complete editing of a cell
   * <p>
   * By default this calls {@link #setValue(ITableRow, Object)} and delegates to
   * {@link #execParseValue(ITableRow, Object)} and {@link #execValidateValue(ITableRow, Object)}.
   */
  @Override
  public final void completeEdit(ITableRow row, IFormField editingField) throws ProcessingException {
    ITable table = getTable();
    if (table == null || !table.isCellEditable(row, this)) {
      return;
    }
    execCompleteEdit(row, editingField);
  }

  @Override
  public void decorateCell(ITableRow row) {
    Cell cell = row.getCellForUpdate(getColumnIndex());
    if (cell.getErrorStatus() == null) {
      decorateCellInternal(cell, row);
    }
    try {
      execDecorateCell(cell, row);
    }
    catch (ProcessingException e) {
      LOG.warn(null, e);
    }
    catch (Throwable t) {
      LOG.warn(null, t);
    }
  }

  /**
   * do not use or override this internal method
   */
  protected void decorateCellInternal(Cell cell, ITableRow row) {
    if (getForegroundColor() != null) {
      cell.setForegroundColor(getForegroundColor());
    }
    if (getBackgroundColor() != null) {
      cell.setBackgroundColor(getBackgroundColor());
    }
    if (getFont() != null) {
      cell.setFont(getFont());
    }
    cell.setHorizontalAlignment(getHorizontalAlignment());
    cell.setEditableInternal(isCellEditable(row));
  }

  @Override
  public void decorateHeaderCell() {
    HeaderCell cell = m_headerCell;
    decorateHeaderCellInternal(cell);
    try {
      execDecorateHeaderCell(cell);
    }
    catch (ProcessingException e) {
      LOG.warn(null, e);
    }
    catch (Throwable t) {
      LOG.warn(null, t);
    }
  }

  /**
   * do not use or override this internal method
   */
  protected void decorateHeaderCellInternal(HeaderCell cell) {
  }

  @Override
  public IHeaderCell getHeaderCell() {
    return m_headerCell;
  }

  @Override
  public int getVisibleColumnIndexHint() {
    return propertySupport.getPropertyInt(PROP_VIEW_COLUMN_INDEX_HINT);
  }

  @Override
  public void setVisibleColumnIndexHint(int index) {
    int oldIndex = getVisibleColumnIndexHint();
    if (oldIndex != index) {
      propertySupport.setPropertyInt(PROP_VIEW_COLUMN_INDEX_HINT, index);
    }
  }

  @Override
  public int getInitialWidth() {
    return m_initialWidth;
  }

  @Override
  public void setInitialWidth(int w) {
    m_initialWidth = w;
  }

  @Override
  public double getViewOrder() {
    return propertySupport.getPropertyDouble(PROP_VIEW_ORDER);
  }

  @Override
  public void setViewOrder(double order) {
    propertySupport.setPropertyDouble(PROP_VIEW_ORDER, order);
  }

  @Override
  public int getWidth() {
    return propertySupport.getPropertyInt(PROP_WIDTH);
  }

  @Override
  public void setWidth(int w) {
    propertySupport.setPropertyInt(PROP_WIDTH, w);
  }

  @Override
  public void setWidthInternal(int w) {
    propertySupport.setPropertyNoFire(PROP_WIDTH, w);
  }

  @Override
  public boolean isFixedWidth() {
    return propertySupport.getPropertyBool(PROP_FIXED_WIDTH);
  }

  @Override
  public void setFixedWidth(boolean fixedWidth) {
    propertySupport.setPropertyBool(PROP_FIXED_WIDTH, fixedWidth);
  }

  @Override
  public void setHorizontalAlignment(int hAglin) {
    propertySupport.setPropertyInt(PROP_HORIZONTAL_ALIGNMENT, hAglin);
  }

  @Override
  public int getHorizontalAlignment() {
    return propertySupport.getPropertyInt(PROP_HORIZONTAL_ALIGNMENT);
  }

  @Override
  public boolean isDisplayable() {
    return propertySupport.getPropertyBool(PROP_DISPLAYABLE);
  }

  @Override
  public void setDisplayable(boolean b) {
    propertySupport.setPropertyBool(PROP_DISPLAYABLE, b);
    calculateVisible();
  }

  @Override
  public boolean isVisible() {
    return propertySupport.getPropertyBool(PROP_VISIBLE);
  }

  @Override
  public void setVisible(boolean b) {
    m_visibleProperty = b;
    calculateVisible();
  }

  private void calculateVisible() {
    propertySupport.setPropertyBool(PROP_VISIBLE, m_visibleGranted && isDisplayable() && m_visibleProperty);
  }

  @Override
  public boolean isVisibleInternal() {
    return m_visibleProperty;
  }

  @Override
  public boolean isPrimaryKey() {
    return m_primaryKey;
  }

  @Override
  public boolean isSummary() {
    return m_summary;
  }

  @Override
  public boolean isEditable() {
    return propertySupport.getPropertyBool(PROP_EDITABLE);
  }

  @Override
  public void setEditable(boolean b) {
    propertySupport.setPropertyBool(PROP_EDITABLE, b);
  }

  @Override
  public boolean isCellEditable(ITableRow row) {
    if (getTable() != null && getTable().isEnabled() && this.isEditable() && row != null && row.isEnabled() && row.getCell(this).isEnabled()) {
      try {
        return execIsEditable(row);
      }
      catch (Throwable t) {
        LOG.error("checking row " + row, t);
        return false;
      }
    }
    return false;
  }

  @Override
  public String getForegroundColor() {
    return (String) propertySupport.getProperty(PROP_FOREGROUND_COLOR);
  }

  @Override
  public void setForegroundColor(String c) {
    propertySupport.setProperty(PROP_FOREGROUND_COLOR, c);
  }

  @Override
  public String getBackgroundColor() {
    return (String) propertySupport.getProperty(PROP_BACKGROUND_COLOR);
  }

  @Override
  public void setBackgroundColor(String c) {
    propertySupport.setProperty(PROP_BACKGROUND_COLOR, c);
  }

  @Override
  public FontSpec getFont() {
    return (FontSpec) propertySupport.getProperty(PROP_FONT);
  }

  @Override
  public void setFont(FontSpec f) {
    propertySupport.setProperty(PROP_FONT, f);
  }

  /**
   * true: Whenever table content changes, automatically calculate optimized column width so that all column content is
   * displayed without
   * cropping.
   * <p>
   * This may display a horizontal scroll bar on the table.
   */
  @Override
  public boolean isAutoOptimizeWidth() {
    return propertySupport.getPropertyBool(PROP_AUTO_OPTIMIZE_WIDTH);
  }

  @Override
  public void setAutoOptimizeWidth(boolean optimize) {
    propertySupport.setPropertyBool(PROP_AUTO_OPTIMIZE_WIDTH, optimize);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + getHeaderCell().getText() + " width=" + getWidth() + (isPrimaryKey() ? " primaryKey" : "") + (isSummary() ? " summary" : "") + " viewIndexHint=" + getVisibleColumnIndexHint() + "]";
  }

  /**
   * Called when a value has been changed in an editable cell.
   * Can be used to persist data directly after a value has been modified in a cell editor.
   * CAUTION: This method is called even when an invalid value has been entered in the cell editor.
   * In this case the last valid value is retrieved while {@link #getValue(ITableRow)} is called.
   * @param row The row changed in the table.
   * 
   * @throws ProcessingException
   */
  protected void persistRowChange(ITableRow row) throws ProcessingException {
  }

  public void validateColumnValues() {
    if (getTable() == null) {
      return;
    }
    for (ITableRow row : getTable().getRows()) {
      validateColumnValue(row, null);
    }
  }

  public void validateColumnValue(ITableRow row, IFormField editor) {
    if (row == null) {
      LOG.error("validateColumnValue called with row=null");
      return;
    }
    if (isCellEditable(row)) {
      try {
        if (editor == null) {
          editor = prepareEdit(row);
        }
        if (editor != null) {
          IProcessingStatus errorStatus = editor.getErrorStatus();
          boolean editorValid = editor.isContentValid() && errorStatus == null;
          Cell cell = row.getCellForUpdate(this);
          if (!editorValid) {
            if (isDisplayable() && !isVisible()) {
              //column should become visible
              setVisible(true);
            }
            if (errorStatus != null) {
              cell.setErrorStatus(errorStatus);
              if (errorStatus instanceof ParsingFailedStatus) {
                cell.setText(((ParsingFailedStatus) errorStatus).getParseInputString());
              }

            }
            else {
              cell.setErrorStatus(ScoutTexts.get("FormEmptyMandatoryFieldsMessage"));
            }
          }
          else {
            cell.clearErrorStatus();
            decorateCellInternal(cell, row);
            ITable table = getTable();
            if (table instanceof AbstractTable) {
              ((AbstractTable) table).wasEverValid(row);
            }
          }
        }
      }
      catch (Throwable t) {
        LOG.error("validating " + getTable().getClass().getSimpleName() + " for new row for column " + getClass().getSimpleName(), t);
      }
    }

  }

}
