package org.eclipse.scout.rt.client.mobile.ui.form.outline;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.scout.commons.NumberUtility;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.exception.ProcessingStatus;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.mobile.transformation.DeviceTransformationConfig;
import org.eclipse.scout.rt.client.mobile.transformation.DeviceTransformationUtility;
import org.eclipse.scout.rt.client.mobile.transformation.MobileDeviceTransformation;
import org.eclipse.scout.rt.client.mobile.ui.basic.table.AbstractMobileTable;
import org.eclipse.scout.rt.client.mobile.ui.basic.table.DrillDownStyleMap;
import org.eclipse.scout.rt.client.mobile.ui.basic.table.MobileTable;
import org.eclipse.scout.rt.client.mobile.ui.basic.table.columns.IRowSummaryColumn;
import org.eclipse.scout.rt.client.mobile.ui.basic.table.form.TableRowForm;
import org.eclipse.scout.rt.client.mobile.ui.desktop.MobileDesktopUtility;
import org.eclipse.scout.rt.client.mobile.ui.form.AbstractMobileForm;
import org.eclipse.scout.rt.client.mobile.ui.form.IActionFetcher;
import org.eclipse.scout.rt.client.mobile.ui.form.fields.table.AbstractMobileTableField;
import org.eclipse.scout.rt.client.mobile.ui.form.outline.PageForm.MainBox.PageDetailFormField;
import org.eclipse.scout.rt.client.mobile.ui.form.outline.PageForm.MainBox.PageTableGroupBox;
import org.eclipse.scout.rt.client.mobile.ui.form.outline.PageForm.MainBox.PageTableGroupBox.PageTableField;
import org.eclipse.scout.rt.client.ui.basic.table.AbstractTable;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.TableAdapter;
import org.eclipse.scout.rt.client.ui.basic.table.TableEvent;
import org.eclipse.scout.rt.client.ui.basic.table.columns.AbstractStringColumn;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithTable;
import org.eclipse.scout.rt.client.ui.form.AbstractFormHandler;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.client.ui.form.fields.GridData;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.button.IButton;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.wrappedform.AbstractWrappedFormField;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.service.SERVICES;

public class PageForm extends AbstractMobileForm implements IPageForm {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(PageForm.class);
  private List<IButton> m_mainboxButtons;
  private IPage m_page;
  private P_PageTableListener m_pageTableListener;
  private P_PageTableSelectionListener m_pageTableSelectionListener;
  private PageFormConfig m_pageFormConfig;
  private PageFormManager m_pageFormManager;
  private Map<ITableRow, AutoLeafPageWithNodes> m_autoLeafPageMap;
  private boolean m_rowSelectionRequired;

  public PageForm(IPage page, PageFormManager manager, PageFormConfig pageFormConfig) throws ProcessingException {
    super(false);
    m_pageFormManager = manager;
    m_pageFormConfig = pageFormConfig;
    if (m_pageFormConfig == null) {
      m_pageFormConfig = new PageFormConfig();
    }
    m_autoLeafPageMap = new HashMap<ITableRow, AutoLeafPageWithNodes>();

    //Init (order is important)
    setPageInternal(page);
    initMainButtons();
    callInitializer();
    initFields();
  }

  @Override
  public void initForm() throws ProcessingException {
    // form
    initFormInternal();

    // fields
    PageFormInitFieldVisitor v = new PageFormInitFieldVisitor();
    visitFields(v);
    v.handleResult();

    // custom
    execInitForm();
  }

  @Override
  public PageFormConfig getPageFormConfig() {
    return m_pageFormConfig;
  }

  @Override
  protected boolean getConfiguredAskIfNeedSave() {
    return false;
  }

  @Override
  protected int getConfiguredDisplayHint() {
    return DISPLAY_HINT_VIEW;
  }

  @Override
  protected String getConfiguredDisplayViewId() {
    return VIEW_ID_CENTER;
  }

  public PageTableField getPageTableField() {
    return getFieldByClass(PageTableField.class);
  }

  public PageDetailFormField getPageDetailFormField() {
    return getFieldByClass(PageDetailFormField.class);
  }

  public PageTableGroupBox getPageTableGroupBox() {
    return getFieldByClass(PageTableGroupBox.class);
  }

  @Override
  public final IPage getPage() {
    return m_page;
  }

  private void setPageInternal(IPage page) throws ProcessingException {
    m_page = page;
    m_page = (IPage) m_page.getTree().resolveVirtualNode(m_page);

    if (m_pageFormConfig.isDetailFormVisible() && m_page.getDetailForm() == null) {
      TableRowForm autoDetailForm = createAutoDetailForm();
      if (autoDetailForm != null) {
        m_page.setDetailForm(autoDetailForm);
        autoDetailForm.start();
      }
    }

    setTitle(page.getCellForUpdate().getText());
  }

  /**
   * If there is a detail form the table field is visible depending on its content. If there is no detail form the table
   * field always is visible.
   */
  protected void updateTableFieldVisibility() throws ProcessingException {
    ITable table = getPageTableField().getTable();
    boolean hasDetailForm = getPageDetailFormField().getInnerForm() != null;

    if (hasDetailForm) {
      boolean hasTableRows = table != null && table.getRowCount() > 0;
      getPageTableField().setVisible(hasTableRows);
    }

    //If there is no table make sure the table group box is invisible and the detail form grows and takes all the space.
    //If there is a table, the detail form must not grow because the table does
    if (getPageTableField().isVisible() != getPageTableGroupBox().isVisible()) {
      getPageTableGroupBox().setVisible(getPageTableField().isVisible());

      GridData gridData = getPageDetailFormField().getGridDataHints();
      if (!getPageTableField().isVisible()) {
        gridData.weightY = 1;
      }
      else {
        gridData.weightY = 0;
      }
      getPageDetailFormField().setGridDataHints(gridData);
      getRootGroupBox().rebuildFieldGrid();
    }
  }

  /**
   * Creates a {@link TableRowForm} out of the selected table row if the parent page is a {@link IPageWithTable}.
   */
  private TableRowForm createAutoDetailForm() throws ProcessingException {
    ITable table = null;
    IPage parentPage = m_page.getParentPage();
    if (parentPage instanceof IPageWithTable) {
      table = ((IPageWithTable) parentPage).getTable();
    }
    if (table != null) {
      if (table.getSelectedRow() == null) {
        //If the parent page has not been selected before there is no row selected -> select it to create the tableRowForm
        ITableRow row = MobileDesktopUtility.getTableRowFor(m_page.getParentPage(), m_page);
        if (row != null) {
          row.getTable().selectRow(row);
        }
      }
      if (table.getSelectedRow() != null) {
        return new TableRowForm(table.getSelectedRow());
      }

    }

    return null;
  }

  @Override
  protected IActionFetcher createHeaderActionFetcher() {
    return new PageFormHeaderActionFetcher(this);
  }

  @Override
  protected IActionFetcher createFooterActionFetcher() {
    return new PageFormFooterActionFetcher(this);
  }

  private void initMainButtons() throws ProcessingException {
    List<IButton> buttonList = new LinkedList<IButton>();

    //Add buttons of the detail form to the main box
    if (m_page.getDetailForm() != null) {
      IButton[] detailFormCustomButtons = m_page.getDetailForm().getRootGroupBox().getCustomProcessButtons();
      buttonList.addAll(Arrays.asList(detailFormCustomButtons));
    }

    m_mainboxButtons = buttonList;
  }

  private void initFields() throws ProcessingException {
    if (m_pageFormConfig.isDetailFormVisible()) {
      getPageDetailFormField().setInnerForm(m_page.getDetailForm());
    }

    //Don't display detail form field if there is no detail form -> saves space
    boolean hasDetailForm = getPageDetailFormField().getInnerForm() != null;
    getPageDetailFormField().setVisible(hasDetailForm);

    ITable pageTable = MobileDesktopUtility.getPageTable(m_page);

    //Make sure the preview form does only contain folder pages.
    if (!m_pageFormConfig.isTablePageAllowed() && m_page instanceof IPageWithTable) {
      pageTable = new PlaceholderTable(m_page);
      pageTable.initTable();
      pageTable.addRowByArray(new Object[]{TEXTS.get("MobilePlaceholderTableTitle")});
      pageTable.setDefaultIconId(m_page.getCell().getIconId());
    }

    AbstractMobileTable.setAutoCreateRowForm(pageTable, false);
    getPageTableField().setTable(pageTable, true);
    getPageTableField().setTableStatusVisible(m_pageFormConfig.isTableStatusVisible());
    addTableListener();
    updateTableFieldVisibility();

    if (getPageTableGroupBox().isVisible() && !hasDetailForm) {
      //If there is a table but no detail form, don't display a border -> make the table as big as the form.
      //If there is a table and a detail form, display a border to make it look better.
      getPageTableGroupBox().setBorderVisible(false);

      //If there is just the table, the form itself does not need to be scrollable because the table already is
      DeviceTransformationConfig config = DeviceTransformationUtility.getDeviceTransformationConfig();
      if (config != null) {
        config.excludeFieldTransformation(getRootGroupBox(), MobileDeviceTransformation.MAKE_MAINBOX_SCROLLABLE);
      }
    }
  }

  @Override
  protected void execDisposeForm() throws ProcessingException {
    removeTableListener();
    for (AutoLeafPageWithNodes autoLeafPage : m_autoLeafPageMap.values()) {
      disposeAutoLeafPage(autoLeafPage);
    }
  }

  private void updateDrillDownStyle() {
    ITable table = getPageTableField().getTable();
    if (table != null) {
      setTableRowDrillDownStyle(table, table.getRows());
    }
  }

  private void setTableRowDrillDownStyle(ITable table, ITableRow[] rows) {
    if (rows == null) {
      return;
    }

    DrillDownStyleMap drillDownMap = MobileTable.getDrillDownStyleMap(table);
    if (drillDownMap == null) {
      drillDownMap = new DrillDownStyleMap();
      AbstractMobileTable.setDrillDownStyleMap(table, drillDownMap);
    }

    for (ITableRow row : rows) {
      if (!isDrillDownRow(row)) {
        drillDownMap.put(row, IRowSummaryColumn.DRILL_DOWN_STYLE_NONE);
      }
      else {
        drillDownMap.put(row, IRowSummaryColumn.DRILL_DOWN_STYLE_ICON);
      }
    }

  }

  private boolean isDrillDownRow(ITableRow tableRow) {
    if (!m_pageFormConfig.isKeepSelection()) {
      return true;
    }

    return PageFormManager.isDrillDownPage(MobileDesktopUtility.getPageFor(getPage(), tableRow));
  }

  public void formAddedNotify() throws ProcessingException {
    LOG.debug(this + " added");

    //Clear selection if form gets visible again. It must not happen earlier, since the actions typically depend on the selected row.
    clearTableSelectionIfNecessary();

    //Make sure the rows display the correct drill down style
    updateDrillDownStyle();

    if (!m_page.isSelectedNode()) {
      selectChildPageTableRowIfNecessary();

      //Make sure the page which belongs to the form is active when the form is shown
      m_page.getOutline().getUIFacade().setNodeSelectedAndExpandedFromUI(m_page);
    }

    addTableSelectionListener();
    processSelectedTableRow();
  }

  @Override
  public void pageSelectedNotify() throws ProcessingException {
    if (m_rowSelectionRequired) {
      selectPageTableRowIfNecessary(getPageTableField().getTable());
      m_rowSelectionRequired = false;
    }
  }

  private void clearTableSelectionIfNecessary() {
    if (getPageTableField().getTable() == null) {
      return;
    }

    ITableRow selectedRow = getPageTableField().getTable().getSelectedRow();
    if (selectedRow != null && isDrillDownRow(selectedRow)) {
      LOG.debug("Clearing row for table " + getPageTableField().getTable());

      getPageTableField().getTable().selectRow(null);
    }
  }

  public void formRemovedNotify() throws ProcessingException {
    removeTableSelectionListener();
  }

  private void addTableListener() {
    if (m_pageTableListener != null) {
      return;
    }
    m_pageTableListener = new P_PageTableListener();

    ITable table = getPageTableField().getTable();
    if (table != null) {
      table.addTableListener(m_pageTableListener);
    }
  }

  private void removeTableListener() {
    if (m_pageTableListener == null) {
      return;
    }
    ITable table = getPageTableField().getTable();
    if (table != null) {
      table.removeTableListener(m_pageTableListener);
    }
    m_pageTableListener = null;
  }

  private void addTableSelectionListener() {
    if (m_pageTableSelectionListener != null) {
      return;
    }
    m_pageTableSelectionListener = new P_PageTableSelectionListener();

    ITable table = getPageTableField().getTable();
    if (table != null) {
      table.addTableListener(m_pageTableSelectionListener);
    }
  }

  private void removeTableSelectionListener() {
    if (m_pageTableSelectionListener == null) {
      return;
    }
    ITable table = getPageTableField().getTable();
    if (table != null) {
      table.removeTableListener(m_pageTableSelectionListener);
    }
    m_pageTableSelectionListener = null;
  }

  private void processSelectedTableRow() throws ProcessingException {
    if (!m_pageFormConfig.isKeepSelection()) {
      return;
    }

    ITable pageTable = MobileDesktopUtility.getPageTable(getPage());
    if (pageTable == null) {
      return;
    }

    ITableRow selectedRow = pageTable.getSelectedRow();
    if (!PageFormManager.isDrillDownPage(MobileDesktopUtility.getPageFor(getPage(), selectedRow))) {
      if (selectedRow != null) {
        handleTableRowSelected(pageTable, selectedRow);
      }
      else {
        selectPageTableRowIfNecessary(pageTable);
      }
    }
  }

  @Override
  public boolean isDirty() {
    if (m_pageFormConfig.isDetailFormVisible()) {
      if (m_page.getDetailForm() != getPageDetailFormField().getInnerForm()) {
        return true;
      }
    }
    if (m_pageFormConfig.isTablePageAllowed() && m_page instanceof IPageWithTable) {
      ITable pageTable = ((IPageWithTable) m_page).getTable();
      if (pageTable != getPageTableField().getTable()) {
        return true;
      }
    }

    return false;
  }

  @Order(10.0f)
  public class MainBox extends AbstractGroupBox {

    @Override
    protected boolean getConfiguredBorderVisible() {
      return false;
    }

    @Override
    protected int getConfiguredGridColumnCount() {
      return 1;
    }

    @Override
    protected void injectFieldsInternal(List<IFormField> fieldList) {
      if (m_mainboxButtons != null) {
        fieldList.addAll(m_mainboxButtons);
      }

      super.injectFieldsInternal(fieldList);
    }

    @Order(5.0f)
    public class PageDetailFormField extends AbstractWrappedFormField<IForm> {

      @Override
      protected int getConfiguredGridW() {
        return 2;
      }

      @Override
      protected int getConfiguredGridH() {
        return 2;
      }

      @Override
      protected double getConfiguredGridWeightY() {
        return 0;
      }

    }

    @Order(10.0f)
    public class PageTableGroupBox extends AbstractGroupBox {

      @Order(10.0f)
      public class PageTableField extends AbstractMobileTableField<ITable> {

        @Override
        protected boolean getConfiguredLabelVisible() {
          return false;
        }

        @Override
        protected boolean getConfiguredTableStatusVisible() {
          return false;
        }

        @Override
        protected boolean getConfiguredGridUseUiHeight() {
          //If there is a detail form make the table as height as necessary to avoid a second scrollbar.
          //If there is no detail form make the table itself scrollable.
          return m_pageFormConfig.isDetailFormVisible() && m_page.getDetailForm() != null;
        }

        @Override
        protected void execUpdateTableStatus() {
          execUpdatePageTableStatus();
        }

        @Override
        public String createDefaultTableStatus() {
          return createDefaultPageTableStatus(getTable());
        }
      }

    }
  }

  protected void execUpdatePageTableStatus() {
    if (!m_pageFormConfig.isTableStatusVisible()) {
      return;
    }
    if (getPage() instanceof IPageWithTable<?>) {
      //popuplate status
      IPageWithTable<?> tablePage = (IPageWithTable<?>) getPage();
      IProcessingStatus populateStatus = tablePage.getPagePopulateStatus();
      getPageTableField().setTablePopulateStatus(populateStatus);
      //selection status
      if (tablePage.isSearchActive() && tablePage.getSearchFilter() != null && (!tablePage.getSearchFilter().isCompleted()) && tablePage.isSearchRequired()) {
        getPageTableField().setTableSelectionStatus(null);
      }
      else if (populateStatus != null && populateStatus.getSeverity() == IProcessingStatus.WARNING) {
        getPageTableField().setTableSelectionStatus(null);
      }
      else {
        getPageTableField().setTableSelectionStatus(new ProcessingStatus(getPageTableField().createDefaultTableStatus(), IProcessingStatus.INFO));
      }
    }
    else {
      getPageTableField().setTablePopulateStatus(null);
      getPageTableField().setTableSelectionStatus(null);
    }
  }

  protected String createDefaultPageTableStatus(ITable table) {
    StringBuilder statusText = new StringBuilder();
    if (table != null) {
      int nTotal = table.getFilteredRowCount();
      if (nTotal == 1) {
        statusText.append(ScoutTexts.get("OneRow"));
      }
      else {
        statusText.append(ScoutTexts.get("XRows", NumberUtility.format(nTotal)));
      }
    }
    if (statusText.length() == 0) {
      return null;
    }
    return statusText.toString();
  }

  @Override
  public Object computeExclusiveKey() throws ProcessingException {
    return m_page;
  }

  @Override
  public void start() throws ProcessingException {
    startInternalExclusive(new FormHandler());
  }

  @Order(10.0f)
  public class FormHandler extends AbstractFormHandler {

    @Override
    protected boolean getConfiguredOpenExclusive() {
      return true;
    }

  }

  private void handleTableRowSelected(ITable table, ITableRow tableRow) throws ProcessingException {
    LOG.debug("Table row selected: " + tableRow);

    // If children are not loaded rowPage cannot be estimated.
    //This is the case when the rows get replaced which restores the selection before the children are loaded (e.g. executed by a search).
    if (!m_page.isLeaf() && !m_page.isChildrenLoaded()) {
      if (tableRow == null) {
        //Postpone the row selection since it cannot be done if the row page cannot be estimated
        m_rowSelectionRequired = true;
      }
      return;
    }

    if (tableRow == null) {
      //Make sure there always is a selected row. if NodePageSwitch is enabled the same page and therefore the same table is on different pageForms
      selectPageTableRowIfNecessary(table);
      return;
    }

    IPage rowPage = null;
    if (table instanceof PlaceholderTable) {
      rowPage = ((PlaceholderTable) table).getActualPage();
    }
    else if (m_autoLeafPageMap.containsKey(tableRow)) {
      rowPage = m_autoLeafPageMap.get(tableRow);
    }
    else {
      rowPage = MobileDesktopUtility.getPageFor(m_page, tableRow);
    }
    if (rowPage == null) {
      //Create auto leaf page including an outline and activate it.
      //Adding to a "real" outline is not possible because the page to row maps in AbstractPageWithTable resp. AbstractPageWithNodes can only be modified by the page itself.
      AutoLeafPageWithNodes autoPage = new AutoLeafPageWithNodes(tableRow, m_page);
      AutoOutline autoOutline = new AutoOutline(autoPage);
      autoOutline.selectNode(autoPage);
      m_autoLeafPageMap.put(tableRow, autoPage);

      rowPage = autoPage;
    }

    m_pageFormManager.pageSelectedNotify(this, rowPage);
  }

  private void handleTableRowsDeleted(ITable table, ITableRow[] tableRows) throws ProcessingException {
    if (tableRows == null) {
      return;
    }

    for (ITableRow tableRow : tableRows) {
      AutoLeafPageWithNodes autoPage = m_autoLeafPageMap.remove(tableRow);
      if (autoPage != null) {
        disposeAutoLeafPage(autoPage);

        m_pageFormManager.pageRemovedNotify(this, autoPage);
      }
    }
  }

  private void disposeAutoLeafPage(AutoLeafPageWithNodes page) {
    if (page == null || page.getOutline() == null) {
      return;
    }

    IOutline outline = page.getOutline();
    outline.removeAllChildNodes(outline.getRootNode());
    outline.disposeTree();
  }

  private void handleTableRowsInserted(ITable table, ITableRow[] tableRows) throws ProcessingException {
    setTableRowDrillDownStyle(table, tableRows);
  }

  protected void selectPageTableRowIfNecessary(final ITable pageDetailTable) throws ProcessingException {
    if (!m_pageFormConfig.isKeepSelection() || pageDetailTable == null || pageDetailTable.getRowCount() == 0) {
      return;
    }

    IPage pageToSelect = MobileDesktopUtility.getPageFor(m_page, pageDetailTable.getRow(0));
    if (pageDetailTable.getSelectedRow() == null) {
      if (!PageFormManager.isDrillDownPage(pageToSelect)) {
        pageDetailTable.selectFirstRow();
      }
    }

  }

  /**
   * If the currently selected page is a child page belonging to this form, make sure the table reflects that -> select
   * the child page in the table
   */
  private void selectChildPageTableRowIfNecessary() {
    if (!m_pageFormConfig.isKeepSelection()) {
      return;
    }

    IPage selectedPage = (IPage) m_page.getOutline().getSelectedNode();
    if (selectedPage != null && selectedPage.getParentPage() == m_page) {
      ITableRow row = MobileDesktopUtility.getTableRowFor(m_page, selectedPage);
      if (row != null && !isDrillDownRow(row)) {
        row.getTable().selectRow(row);
      }
    }
  }

  private class PlaceholderTable extends AbstractTable {
    private IPage m_actualPage;

    public PlaceholderTable(IPage page) {
      m_actualPage = page;
    }

    public IPage getActualPage() {
      return m_actualPage;
    }

    @Override
    protected boolean getConfiguredSortEnabled() {
      return false;
    }

    @Override
    protected boolean getConfiguredAutoResizeColumns() {
      return true;
    }

    @Override
    protected boolean getConfiguredMultiSelect() {
      return false;
    }

    public LabelColumn getLabelColumn() {
      return getColumnSet().getColumnByClass(LabelColumn.class);
    }

    @Order(1)
    public class LabelColumn extends AbstractStringColumn {

      @Override
      protected String getConfiguredHeaderText() {
        return ScoutTexts.get("Folders");
      }

    }
  }

  private class P_PageTableListener extends TableAdapter {
    @Override
    public void tableChanged(TableEvent event) {
      try {
        switch (event.getType()) {
          case TableEvent.TYPE_ALL_ROWS_DELETED:
          case TableEvent.TYPE_ROWS_DELETED: {
            handleTableRowDeleted(event);
            break;
          }
          case TableEvent.TYPE_ROWS_INSERTED:
            handleTableRowsInserted(event);
        }
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
    }

    private void handleTableRowDeleted(TableEvent event) throws ProcessingException {
      PageForm.this.handleTableRowsDeleted(event.getTable(), event.getRows());
      updateTableFieldVisibility();
    }

    private void handleTableRowsInserted(TableEvent event) throws ProcessingException {
      PageForm.this.handleTableRowsInserted(event.getTable(), event.getRows());
      updateTableFieldVisibility();
    }

  }

  private class P_PageTableSelectionListener extends TableAdapter {
    @Override
    public void tableChanged(TableEvent event) {
      try {
        switch (event.getType()) {
          case TableEvent.TYPE_ROWS_SELECTED: {
            handleTableRowSelected(event);
            break;
          }
        }
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
    }

    private void handleTableRowSelected(TableEvent event) throws ProcessingException {
      if (event.isConsumed()) {
        return;
      }

      ITableRow tableRow = event.getFirstRow();
      PageForm.this.handleTableRowSelected(event.getTable(), tableRow);
    }

  }

  @Override
  public String toString() {
    return super.toString() + " with page " + m_page;
  }
}
