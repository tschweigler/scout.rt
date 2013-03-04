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
package org.eclipse.scout.rt.client.services.common.bookmark.internal;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.CRC32;

import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.CompositeObject;
import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.exception.ProcessingStatus;
import org.eclipse.scout.commons.exception.VetoException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.basic.table.ColumnSet;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.basic.table.ITableRow;
import org.eclipse.scout.rt.client.ui.basic.table.columns.IColumn;
import org.eclipse.scout.rt.client.ui.basic.table.customizer.ITableCustomizer;
import org.eclipse.scout.rt.client.ui.basic.tree.ITreeNode;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPage;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithNodes;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithTable;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.ISearchForm;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.TEXTS;
import org.eclipse.scout.rt.shared.services.common.bookmark.AbstractPageState;
import org.eclipse.scout.rt.shared.services.common.bookmark.Bookmark;
import org.eclipse.scout.rt.shared.services.common.bookmark.NodePageState;
import org.eclipse.scout.rt.shared.services.common.bookmark.TableColumnState;
import org.eclipse.scout.rt.shared.services.common.bookmark.TablePageState;
import org.eclipse.scout.rt.shared.services.common.jdbc.SearchFilter;

public final class BookmarkUtility {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(BookmarkUtility.class);

  private BookmarkUtility() {
  }

  public static IOutline resolveOutline(IOutline[] outlines, String className) {
    if (className == null) {
      return null;
    }
    // pass 1: fully qualified name
    for (IOutline o : outlines) {
      if (o.getClass().getName().equals(className)) {
        return o;
      }
    }
    // pass 2: simple name, not case sensitive
    String simpleClassName = className.replaceAll("^.*\\.", "");
    for (IOutline o : outlines) {
      if (o.getClass().getSimpleName().equalsIgnoreCase(simpleClassName)) {
        return o;
      }
    }
    return null;
  }

  /**
   * @param columns
   *          is the set of available columns to search in
   * @param className
   *          is the columnId, simple class name or class name of the columns to find
   */
  public static IColumn resolveColumn(IColumn[] columns, String identifier) {
    if (identifier == null) {
      return null;
    }
    // pass 1: fully qualified name
    for (IColumn c : columns) {
      if (identifier.equals(c.getClass().getName())) {
        return c;
      }
    }
    // pass 2: columnId
    for (IColumn c : columns) {
      if (identifier.equals(c.getColumnId())) {
        return c;
      }
    }
    // pass 3: simple name, not case sensitive
    String simpleClassName = identifier.replaceAll("^.*\\.", "");
    for (IColumn c : columns) {
      if (simpleClassName.equalsIgnoreCase(c.getClass().getSimpleName())) {
        return c;
      }
    }
    return null;
  }

  public static IPage resolvePage(IPage[] pages, String className, String userPreferenceContext) {
    if (className == null) {
      return null;
    }
    TreeMap<CompositeObject, IPage> sortMap = new TreeMap<CompositeObject, IPage>();
    String simpleClassName = className.replaceAll("^.*\\.", "");
    int index = 0;
    for (IPage p : pages) {
      int classNameScore = 0;
      int userPreferenceContextScore = 0;
      if (p.getClass().getName().equals(className)) {
        classNameScore = -2;
      }
      else if (p.getClass().getSimpleName().equalsIgnoreCase(simpleClassName)) {
        classNameScore = -1;
      }
      if (userPreferenceContext == null || userPreferenceContext.equalsIgnoreCase(p.getUserPreferenceContext())) {
        userPreferenceContextScore = -1;
      }
      if (classNameScore != 0 && userPreferenceContextScore != 0) {
        sortMap.put(new CompositeObject(classNameScore, userPreferenceContextScore, index), p);
      }
      index++;
    }
    if (sortMap.isEmpty()) {
      return null;
    }
    CompositeObject bestMatchingKey = sortMap.firstKey();
    IPage bestMatchingPage = sortMap.remove(bestMatchingKey);
    if (!sortMap.isEmpty()) {
      // check ambiguity
      CompositeObject nextKey = sortMap.firstKey();
      if (CompareUtility.equals(bestMatchingKey.getComponent(0), nextKey.getComponent(0)) && CompareUtility.equals(bestMatchingKey.getComponent(1), nextKey.getComponent(1))) {
        LOG.warn("More than one pages found for page class [" + className + "] and user preference context [" + userPreferenceContext + "]");
      }
    }
    return bestMatchingPage;
  }

  /**
   * intercept objects that are not remoting-capable or not serializable and
   * replace by strings
   */
  public static Object[] makeSerializableKeys(Object[] a, boolean useLegacySupport) {
    return (Object[]) makeSerializableKey(a, useLegacySupport);
  }

  public static Object makeSerializableKey(Object o, boolean useLegacySupport) {
    if (o == null) {
      return o;
    }
    else if (o instanceof Number) {
      return o;
    }
    else if (o instanceof String) {
      return o;
    }
    else if (o instanceof Boolean) {
      return o;
    }
    else if (o instanceof Date) {
      return o;
    }
    else if (o.getClass().isArray()) {
      ArrayList<Integer> dimList = new ArrayList<Integer>();
      Class xc = o.getClass();
      Object xo = o;
      while (xc.isArray()) {
        int len = xo != null ? Array.getLength(xo) : 0;
        dimList.add(len);
        xc = xc.getComponentType();
        if (xo != null && len > 0) {
          xo = Array.get(xo, 0);
        }
      }
      int[] dim = new int[dimList.size()];
      for (int i = 0; i < dim.length; i++) {
        dim[i] = dimList.get(i);
      }
      Object b = Array.newInstance(makeSerializableClass(xc, useLegacySupport), dim);
      for (int i = 0; i < dim[0]; i++) {
        Array.set(b, i, makeSerializableKey(Array.get(o, i), useLegacySupport));
      }
      return b;
    }
    else if (!useLegacySupport && o instanceof Serializable) {
      return o;
    }
    else {
      // check if key object overrides toString()
      if (!useLegacySupport && !(o instanceof String)) {
        if (ConfigurationUtility.isMethodOverwrite(Object.class, "toString", new Class[0], o.getClass())) {
          LOG.warn("Bookmark key is not serializable. Falling back to toString(). Note: keys may not be stable [class=" + o.getClass() + ", string representation: " + o.toString() + "]");
        }
        else {
          LOG.error("Bookmark key is not serializable. Falling back to toString() which is not overriden by the given class [" + o.getClass() + "]");
        }
      }
      return o.toString();
    }
  }

  /**
   * return String.class for classes that are not remoting-capable or not
   * serializable
   */
  public static Class makeSerializableClass(Class c, boolean useLegacySupport) {
    if (c == null) {
      throw new IllegalArgumentException("class must not be null");
    }
    if (c.isArray()) {
      throw new IllegalArgumentException("class must not be an array class");
    }
    if (c.isPrimitive()) {
      return c;
    }
    else if (Number.class.isAssignableFrom(c)) {
      return c;
    }
    else if (String.class.isAssignableFrom(c)) {
      return c;
    }
    else if (Boolean.class.isAssignableFrom(c)) {
      return c;
    }
    else if (Date.class.isAssignableFrom(c)) {
      return c;
    }
    else if (Object.class == c) {
      return c;
    }
    else if (!useLegacySupport && Serializable.class.isAssignableFrom(c)) {
      return c;
    }
    else {
      return String.class;
    }
  }

  public static void activateBookmark(IDesktop desktop, Bookmark bm, boolean forceReload) throws ProcessingException {
    if (bm.getOutlineClassName() == null) {
      return;
    }
    IOutline outline = BookmarkUtility.resolveOutline(desktop.getAvailableOutlines(), bm.getOutlineClassName());
    if (outline == null) {
      throw new ProcessingException("outline '" + bm.getOutlineClassName() + "' was not found");
    }
    if (!(outline.isVisible() && outline.isEnabled())) {
      throw new VetoException(TEXTS.get("BookmarkActivationFailedOutlineNotAvailable", outline.getTitle()));
    }
    desktop.setOutline(outline);
    try {
      outline.setTreeChanging(true);
      //
      IPage parentPage = outline.getRootPage();
      boolean pathFullyRestored = true;
      List<AbstractPageState> path = bm.getPath();
      AbstractPageState parentPageState = path.get(0);
      boolean resetViewAndWarnOnFail = bm.getId() != 0;
      for (int i = 1; i < path.size(); i++) {
        // try to find correct child page (next parentPage)
        IPage childPage = null;
        AbstractPageState childState = path.get(i);
        if (parentPageState instanceof TablePageState) {
          TablePageState tablePageState = (TablePageState) parentPageState;
          if (parentPage instanceof IPageWithTable) {
            IPageWithTable tablePage = (IPageWithTable) parentPage;
            childPage = bmLoadTablePage(tablePage, tablePageState, false, resetViewAndWarnOnFail);
          }
        }
        else if (parentPageState instanceof NodePageState) {
          NodePageState nodePageState = (NodePageState) parentPageState;
          if (parentPage instanceof IPageWithNodes) {
            IPageWithNodes nodePage = (IPageWithNodes) parentPage;
            childPage = bmLoadNodePage(nodePage, nodePageState, childState, resetViewAndWarnOnFail);
          }
        }
        // next
        if (childPage != null) {
          parentPage = childPage;
          parentPageState = childState;
        }
        else if (i < path.size()) {
          pathFullyRestored = false;
          break;
        }
      }
      if (pathFullyRestored) {
        if (parentPageState instanceof TablePageState && parentPage instanceof IPageWithTable) {
          bmLoadTablePage((IPageWithTable) parentPage, (TablePageState) parentPageState, true, resetViewAndWarnOnFail);
        }
        else if (parentPage instanceof IPageWithNodes) {
          bmLoadNodePage((IPageWithNodes) parentPage, (NodePageState) parentPageState, null, resetViewAndWarnOnFail);
        }
      }
      /*
       * Expansions of the restored tree path
       */
      IPage p = parentPage;
      // last element
      if (pathFullyRestored && parentPageState.isExpanded() != null) {
        p.setExpanded(parentPageState.isExpanded());
      }
      else {
        if (!(p instanceof IPageWithTable)) {
          p.setExpanded(true);
        }
      }
      // ancestor elements
      p = p.getParentPage();
      while (p != null) {
        p.setExpanded(true);
        p = p.getParentPage();
      }
      outline.selectNode(parentPage, false);
    }
    finally {
      outline.setTreeChanging(false);
    }
  }

  /**
   * Constructs a list of {@link TableColumnState} objects which
   * describe the set of columns of the given {@link ITable}.
   * 
   * @param table
   *          The table with the columns to back-up.
   * @return A {@link List} of {@link TableColumnState} objects that
   *         can be restored via {@link #restoreTableColumns(ITable, List)}
   */
  public static List<TableColumnState> backupTableColumns(ITable table) {
    ArrayList<TableColumnState> allColumns = new ArrayList<TableColumnState>();
    ColumnSet columnSet = table.getColumnSet();
    //add all columns but in user order
    for (IColumn<?> c : columnSet.getAllColumnsInUserOrder()) {
      TableColumnState colState = new TableColumnState();
      colState.setColumnClassName(c.getColumnId());
      colState.setDisplayable(c.isDisplayable());
      colState.setVisible(c.isDisplayable() && c.isVisible());
      colState.setWidth(c.getWidth());
      if (columnSet.isUserSortColumn(c) && c.isSortExplicit()) {
        int sortOrder = columnSet.getSortColumnIndex(c);
        if (sortOrder >= 0) {
          colState.setSortOrder(sortOrder);
          colState.setSortAscending(c.isSortAscending());
        }
        else {
          colState.setSortOrder(-1);
        }
      }
      if (table.getColumnFilterManager() != null && c.isColumnFilterActive()) {
        colState.setColumnFilterData(table.getColumnFilterManager().getSerializedFilter(c));
      }
      allColumns.add(colState);
    }
    return allColumns;
  }

  /**
   * Restores a tables columns from the given list of {@link TableColumnState} objects.
   * 
   * @param table
   *          The table to be restored.
   * @param oldColumns
   *          A {@link List} of {@link TableColumnState} objects to
   *          restore. Such can be retrieved by the {@link #backupTableColumns(ITable)} method.
   */
  public static void restoreTableColumns(ITable table, List<TableColumnState> oldColumns) throws ProcessingException {
    if (oldColumns != null && oldColumns.size() > 0 && table != null) {
      ColumnSet columnSet = table.getColumnSet();
      // visible columns and width
      ArrayList<IColumn> visibleColumns = new ArrayList<IColumn>();
      for (TableColumnState colState : oldColumns) {
        //legacy support: null=true
        if (colState.getVisible() == null || colState.getVisible()) {
          IColumn col = resolveColumn(columnSet.getDisplayableColumns(), colState.getClassName());
          if (col != null && col.isDisplayable()) {
            if (colState.getWidth() > 0) {
              col.setWidth(colState.getWidth());
            }
            visibleColumns.add(col);
          }
        }
      }
      List<IColumn> existingVisibleCols = Arrays.asList(columnSet.getVisibleColumns());
      if (!existingVisibleCols.equals(visibleColumns)) {
        columnSet.setVisibleColumns(visibleColumns.toArray(new IColumn[0]));
      }
      // filters
      if (table.getColumnFilterManager() != null) {
        // If a column filter was set, but did not have one before
        // the change, it will not be reset.
        // So reset all column filters beforehand:
        table.getColumnFilterManager().reset();
        for (TableColumnState colState : oldColumns) {
          if (colState.getColumnFilterData() != null) {
            IColumn col = BookmarkUtility.resolveColumn(columnSet.getColumns(), colState.getClassName());
            if (col != null) {
              table.getColumnFilterManager().setSerializedFilter(colState.getColumnFilterData(), col);
            }
          }
        }
      }
      //sort order (only respect visible and user-sort columns)
      boolean userSortValid = true;
      TreeMap<Integer, IColumn> sortColMap = new TreeMap<Integer, IColumn>();
      HashMap<IColumn, Boolean> sortColAscMap = new HashMap<IColumn, Boolean>();
      for (TableColumnState colState : oldColumns) {
        if (colState.getSortOrder() >= 0) {
          IColumn col = BookmarkUtility.resolveColumn(columnSet.getColumns(), colState.getClassName());
          if (col != null) {
            sortColMap.put(colState.getSortOrder(), col);
            sortColAscMap.put(col, colState.isSortAscending());
            if (col.getSortIndex() != colState.getSortOrder()) {
              userSortValid = false;
            }
            if (col.isSortAscending() != colState.isSortAscending()) {
              userSortValid = false;
            }
          }
        }
      }
      HashSet<IColumn<?>> existingExplicitUserSortCols = new HashSet<IColumn<?>>();
      for (IColumn<?> c : columnSet.getUserSortColumns()) {
        if (c.isSortExplicit()) {
          existingExplicitUserSortCols.add(c);
        }
      }
      if (!sortColMap.values().containsAll(existingExplicitUserSortCols)) {
        userSortValid = false;
      }
      if (!userSortValid) {
        columnSet.clearSortColumns();
        for (IColumn col : sortColMap.values()) {
          columnSet.addSortColumn(col, sortColAscMap.get(col));
        }
        table.sort();
      }
    }
  }

  public static Bookmark createBookmark(IDesktop desktop) throws ProcessingException {
    IOutline outline = desktop.getOutline();
    if (outline == null) {
      return null;
    }

    IPage activePage = outline.getActivePage();
    return createBookmark(activePage);
  }

  public static Bookmark createBookmark(IPage page) throws ProcessingException {
    if (page == null || page.getOutline() == null) {
      return null;
    }

    IOutline outline = page.getOutline();
    Bookmark b = new Bookmark();
    b.setIconId(page.getCell().getIconId());
    // outline
    b.setOutlineClassName(outline.getClass().getName());
    ArrayList<IPage> path = new ArrayList<IPage>();
    ArrayList<String> titleSegments = new ArrayList<String>();
    while (page != null) {
      path.add(0, page);
      String s = page.getCell().getText();
      if (s != null) {
        titleSegments.add(0, s);
      }
      // next
      page = (IPage) page.getParentNode();
    }
    if (outline.getTitle() != null) {
      titleSegments.add(0, outline.getTitle());
    }
    // title
    int len = 0;
    if (titleSegments.size() > 0) {
      len += titleSegments.get(0).length();
    }
    if (titleSegments.size() > 1) {
      len += titleSegments.get(titleSegments.size() - 1).length();
    }
    for (int i = titleSegments.size() - 1; i > 0; i--) {
      if (len > 200) {
        titleSegments.remove(i);
      }
      else if (len + titleSegments.get(i).length() <= 200) {
        len += titleSegments.get(i).length();
      }
      else {
        titleSegments.set(i, "...");
        len = 201;
      }
    }
    StringBuilder buf = new StringBuilder();
    for (String s : titleSegments) {
      if (buf.length() > 0) {
        buf.append(" - ");
      }
      buf.append(s);
    }
    b.setTitle(buf.toString());
    // text
    StringBuffer text = new StringBuffer();
    // add constraints texts
    String prefix = "";
    for (int i = 0; i < path.size(); i++) {
      page = path.get(i);
      if (i > 0 || outline.isRootNodeVisible()) {
        text.append(prefix + page.getCell().getText());
        text.append("\n");
        if (page instanceof IPageWithTable) {
          IPageWithTable tablePage = (IPageWithTable) page;
          SearchFilter search = tablePage.getSearchFilter();
          if (search != null) {
            for (String s : search.getDisplayTexts()) {
              if (s != null) {
                String indent = prefix + "  ";
                s = s.trim().replaceAll("[\\n]", "\n" + indent);
                if (s.length() > 0) {
                  text.append(indent + s);
                  text.append("\n");
                }
              }
            }
          }
        }
        prefix += "  ";
      }
    }
    b.setText(text.toString().trim());
    // path
    for (int i = 0; i < path.size(); i++) {
      page = path.get(i);
      if (page instanceof IPageWithTable) {
        IPageWithTable tablePage = (IPageWithTable) page;
        IPage childPage = null;
        if (i + 1 < path.size()) {
          childPage = path.get(i + 1);
        }
        b.addPathElement(bmStoreTablePage(tablePage, childPage));
      }
      else if (page instanceof IPageWithNodes) {
        IPageWithNodes nodePage = (IPageWithNodes) page;
        b.addPathElement(bmStoreNodePage(nodePage));
      }
    }
    return b;
  }

  private static IPage bmLoadTablePage(IPageWithTable tablePage, TablePageState tablePageState, boolean leafState, boolean resetViewAndWarnOnFail) throws ProcessingException {
    ITable table = tablePage.getTable();
    if (tablePageState.getTableCustomizerData() != null && tablePage.getTable().getTableCustomizer() != null) {
      byte[] newData = tablePageState.getTableCustomizerData();
      ITableCustomizer tc = tablePage.getTable().getTableCustomizer();
      byte[] curData = tc.getSerializedData();
      if (!CompareUtility.equals(curData, newData)) {
        tc.removeAllColumns();
        tc.setSerializedData(tablePageState.getTableCustomizerData());
        tablePage.getTable().resetColumnConfiguration();
        tablePage.setChildrenLoaded(false);
      }
    }
    // starts search form
    tablePage.getSearchFilter();
    // setup table
    try {
      table.setTableChanging(true);
      //legacy support
      @SuppressWarnings("deprecation")
      List<TableColumnState> allColumns = tablePageState.getVisibleColumns();
      if (allColumns == null || allColumns.size() == 0) {
        allColumns = tablePageState.getAvailableColumns();
      }
      restoreTableColumns(tablePage.getTable(), allColumns);
    }
    finally {
      table.setTableChanging(false);
    }
    // setup search
    if (tablePageState.getSearchFormState() != null) {
      ISearchForm searchForm = tablePage.getSearchFormInternal();
      if (searchForm != null) {
        boolean doSearch = true;
        String newSearchFilterState = tablePageState.getSearchFilterState();
        String oldSearchFilterState = "" + createSearchFilterCRC(searchForm.getSearchFilter());
        if (CompareUtility.equals(oldSearchFilterState, newSearchFilterState)) {
          String newSearchFormState = tablePageState.getSearchFormState();
          String oldSearchFormState = searchForm.getXML("UTF-8");
          if (CompareUtility.equals(oldSearchFormState, newSearchFormState)) {
            doSearch = false;
          }
        }
        // in case search form is in correct state, but no search has been executed, force search
        if (tablePage.getTable().getRowCount() == 0) {
          doSearch = true;
        }
        if (doSearch) {
          searchForm.setXML(tablePageState.getSearchFormState());
          if (tablePageState.isSearchFilterComplete()) {
            searchForm.doSaveWithoutMarkerChange();
          }
        }
      }
    }
    IPage childPage = null;
    boolean loadChildren = !leafState;
    if (tablePage.isChildrenDirty() || tablePage.isChildrenVolatile()) {
      loadChildren = true;
      tablePage.setChildrenLoaded(false);
    }
    if (loadChildren) {
      tablePage.ensureChildrenLoaded();
      tablePage.setChildrenDirty(false);
      CompositeObject childPk = tablePageState.getExpandedChildPrimaryKey();
      if (childPk != null) {
        for (int r = 0; r < table.getRowCount(); r++) {
          CompositeObject testPkLegacy = new CompositeObject(BookmarkUtility.makeSerializableKeys(table.getRowKeys(r), true));
          CompositeObject testPk = new CompositeObject(BookmarkUtility.makeSerializableKeys(table.getRowKeys(r), false));
          if (testPk.equals(childPk) || testPkLegacy.equals(childPk)) {
            if (r < tablePage.getChildNodeCount()) {
              childPage = tablePage.getChildPage(r);
            }
            break;
          }
        }
      }
      else {
        ITreeNode[] filteredChildNodes = tablePage.getFilteredChildNodes();
        if (filteredChildNodes.length > 0) {
          childPage = (IPage) filteredChildNodes[0];
        }
        else if (tablePage.getChildNodeCount() > 0) {
          childPage = tablePage.getChildPage(0);
        }
      }
    }
    // load selections
    if (leafState) {
      if (tablePageState.getSelectedChildrenPrimaryKeys().size() > 0) {
        tablePage.ensureChildrenLoaded();
        HashSet<CompositeObject> selectionSet = new HashSet<CompositeObject>(tablePageState.getSelectedChildrenPrimaryKeys());
        ArrayList<ITableRow> rowList = new ArrayList<ITableRow>();
        for (ITableRow row : table.getRows()) {
          CompositeObject testPkLegacy = new CompositeObject(BookmarkUtility.makeSerializableKeys(row.getKeyValues(), true));
          CompositeObject testPk = new CompositeObject(BookmarkUtility.makeSerializableKeys(row.getKeyValues(), false));
          if (selectionSet.contains(testPk) || selectionSet.contains(testPkLegacy)) {
            //row must not be filtered out
            if (row.isFilterAccepted()) {
              rowList.add(row);
            }
          }
        }
        if (rowList.size() > 0) {
          table.selectRows(rowList.toArray(new ITableRow[0]));
        }
      }

      return childPage;
    }

    // check, whether table column filter must be reset
    if (resetViewAndWarnOnFail) {
      if (childPage == null
          || (!childPage.isFilterAccepted() && table.getColumnFilterManager() != null && table.getColumnFilterManager().isEnabled())) {
        table.getColumnFilterManager().reset();
        tablePage.setPagePopulateStatus(new ProcessingStatus(ScoutTexts.get("BookmarkResetColumnFilters"), ProcessingStatus.WARNING));
      }
    }

    // child page is not available or filtered out
    if (childPage == null || !childPage.isFilterAccepted()) {
      if (resetViewAndWarnOnFail) {
        // set appropriate warning
        if (tablePage.isSearchActive() && tablePage.getSearchFormInternal() != null) {
          tablePage.setPagePopulateStatus(new ProcessingStatus(ScoutTexts.get("BookmarkResolutionCanceledCheckSearchCriteria"), ProcessingStatus.WARNING));
        }
        else {
          tablePage.setPagePopulateStatus(new ProcessingStatus(ScoutTexts.get("BookmarkResolutionCanceled"), ProcessingStatus.WARNING));
        }
      }
      childPage = null;
    }

    return childPage;
  }

  private static IPage bmLoadNodePage(IPageWithNodes nodePage, NodePageState nodePageState, AbstractPageState childState, boolean resetViewAndWarnOnFail) throws ProcessingException {
    IPage childPage = null;
    if (childState != null) {
      nodePage.ensureChildrenLoaded();
      IPage p = BookmarkUtility.resolvePage(nodePage.getChildPages(), childState.getPageClassName(), childState.getBookmarkIdentifier());
      if (p != null) {
        ITable table = nodePage.getInternalTable();
        // reset table column filter if requested
        if (resetViewAndWarnOnFail && !p.isFilterAccepted() && table.getColumnFilterManager() != null && table.getColumnFilterManager().isEnabled()) {
          table.getColumnFilterManager().reset();
        }

        // check table column filter
        if (p.isFilterAccepted()) {
          childPage = p;
        }
      }
      // set appropriate warning if child page is not available or filtered out
      if (childPage == null && resetViewAndWarnOnFail) {
        nodePage.setPagePopulateStatus(new ProcessingStatus(ScoutTexts.get("BookmarkResolutionCanceled"), ProcessingStatus.WARNING));
      }
    }
    return childPage;
  }

  private static TablePageState bmStoreTablePage(IPageWithTable page, IPage childPage) throws ProcessingException {
    ITable table = page.getTable();
    TablePageState state = new TablePageState();
    state.setPageClassName(page.getClass().getName());
    state.setBookmarkIdentifier(page.getUserPreferenceContext());
    state.setLabel(page.getCell().getText());
    state.setExpanded(page.isExpanded());
    IForm searchForm = page.getSearchFormInternal();
    if (searchForm != null) {
      state.setSearchFormState(searchForm.getXML("UTF-8"));
      state.setSearchFilterState(searchForm.getSearchFilter().isCompleted(), "" + createSearchFilterCRC(searchForm.getSearchFilter()));
    }
    if (page.getTable().getTableCustomizer() != null) {
      state.setTableCustomizerData(page.getTable().getTableCustomizer().getSerializedData());
    }
    List<TableColumnState> allColumns = backupTableColumns(page.getTable());
    state.setAvailableColumns(allColumns);
    //
    ArrayList<CompositeObject> pkList = new ArrayList<CompositeObject>();
    for (ITableRow row : table.getSelectedRows()) {
      pkList.add(new CompositeObject(BookmarkUtility.makeSerializableKeys(row.getKeyValues(), false)));
    }
    state.setSelectedChildrenPrimaryKeys(pkList);
    //
    if (childPage != null) {
      for (int j = 0; j < table.getRowCount(); j++) {
        if (page.getChildNode(j) == childPage) {
          ITableRow childRow = table.getRow(j);
          state.setExpandedChildPrimaryKey(new CompositeObject(BookmarkUtility.makeSerializableKeys(childRow.getKeyValues(), false)));
          break;
        }
      }
    }
    return state;
  }

  private static NodePageState bmStoreNodePage(IPageWithNodes page) throws ProcessingException {
    NodePageState state = new NodePageState();
    state.setPageClassName(page.getClass().getName());
    state.setBookmarkIdentifier(page.getUserPreferenceContext());
    state.setLabel(page.getCell().getText());
    state.setExpanded(page.isExpanded());
    return state;
  }

  private static long createSearchFilterCRC(SearchFilter filter) {
    if (filter == null) {
      return 0L;
    }
    try {
      CRC32 crc = new CRC32();
      ByteArrayOutputStream bo = new ByteArrayOutputStream();
      ObjectOutputStream oo = new ObjectOutputStream(bo);
      oo.writeObject(filter);
      oo.close();
      crc.update(bo.toByteArray());
      return crc.getValue();
    }
    catch (Throwable t) {
      // nop
      return -1L;
    }
  }

}
