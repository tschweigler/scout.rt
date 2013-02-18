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
package org.eclipse.scout.rt.ui.swt.form.fields.tabbox;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.scout.commons.OptimisticLock;
import org.eclipse.scout.commons.RunnableWithData;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.IGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.tabbox.ITabBox;
import org.eclipse.scout.rt.ui.swt.form.fields.SwtScoutFieldComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * <h3>SwtScoutTabBox</h3> ...
 * 
 * @since 1.0.0 04.04.2008
 */
public class SwtScoutTabBox extends SwtScoutFieldComposite<ITabBox> implements ISwtScoutTabBox {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(SwtScoutTabBox.class);

  private HashMap<CTabItem, SwtScoutTabItem> m_tabs;
  private Listener m_uiTabFocusListener;
  private P_TabListener m_tabListener = new P_TabListener();
  private OptimisticLock m_selectedTabLock;

  public SwtScoutTabBox() {
    m_selectedTabLock = new OptimisticLock();
  }

  @Override
  protected void initializeSwt(Composite parent) {
    CTabFolder tabFolder = getEnvironment().getFormToolkit().createTabFolder(parent);
    tabFolder.marginHeight = 8;
    setSwtLabel(null);
    setSwtField(tabFolder);
    setSwtContainer(tabFolder);
    // set up tabs
    for (IGroupBox box : getScoutObject().getGroupBoxes()) {
      box.addPropertyChangeListener(m_tabListener);
    }
    buildItems();
    if (m_uiTabFocusListener == null) {
      m_uiTabFocusListener = new P_SwtFocusListener();
    }
    tabFolder.addListener(SWT.Selection, m_uiTabFocusListener);
    tabFolder.addListener(SWT.FocusIn, m_uiTabFocusListener);
    tabFolder.addListener(SWT.FocusOut, m_uiTabFocusListener);
  }

  protected void buildItems() {
    try {
      getSwtContainer().setRedraw(false);
      m_tabs = new HashMap<CTabItem, SwtScoutTabItem>();
      for (IGroupBox box : getScoutObject().getGroupBoxes()) {
        if (box.isVisible()) {
          // XXX make tabitem exchangeable... extension point and provide a ITabItemInterface
          SwtScoutTabItem item = new SwtScoutTabItem();
          item.createField(getSwtField(), box, getEnvironment());
          m_tabs.put(item.getTabItem(), item);
        }
      }
      SwtScoutTabItem selectedTabItem = getTabItem(getScoutObject().getSelectedTab());
      if (selectedTabItem != null) {
        getSwtField().setSelection(selectedTabItem.getTabItem());
      }
    }
    finally {
      getSwtContainer().setRedraw(true);
    }

  }

  @Override
  public ITabBox getScoutObject() {
    return super.getScoutObject();
  }

  @Override
  public CTabFolder getSwtField() {
    return (CTabFolder) super.getSwtField();
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    setSelectedTabFromScout();
  }

  @Override
  protected void detachScout() {
    for (IGroupBox b : getScoutObject().getGroupBoxes()) {
      b.removePropertyChangeListener(m_tabListener);
    }
    super.detachScout();
  }

  @Override
  protected void setEnabledFromScout(boolean b) {
    // void here
  }

  protected void setSelectedTabFromSwt() {
    try {
      if (m_selectedTabLock.acquire()) {
        CTabItem comp = getSwtField().getSelection();
        SwtScoutTabItem tabComposite = m_tabs.get(comp);
        if (tabComposite != null) {
          final IGroupBox box = tabComposite.getScoutObject();
          if (box != null) {
            Runnable j = new Runnable() {
              @Override
              public void run() {
                getScoutObject().getUIFacade().setSelectedTabFromUI(box);
              }
            };
            getEnvironment().invokeScoutLater(j, 0);
          }
        }
      }
    }
    finally {
      m_selectedTabLock.release();
    }
  }

  /**
   * scout settings
   */
  protected void setSelectedTabFromScout() {
    try {
      m_selectedTabLock.acquire();
      //
      IGroupBox selectedTab = getScoutObject().getSelectedTab();
      CTabItem foundItem = null;
      for (Map.Entry<CTabItem, SwtScoutTabItem> e : m_tabs.entrySet()) {
        IGroupBox test = e.getValue().getScoutObject();
        if (test == selectedTab) {
          foundItem = e.getKey();
          break;
        }
      }
      if (foundItem != null) {
        getSwtField().setSelection(foundItem);
      }
    }
    finally {
      m_selectedTabLock.release();
    }
  }

  /**
   * scout property observer
   */
  @Override
  protected void handleScoutPropertyChange(String name, Object newValue) {
    super.handleScoutPropertyChange(name, newValue);
    if (name.equals(ITabBox.PROP_SELECTED_TAB)) {
      setSelectedTabFromScout();
    }
  }

  private class P_SwtFocusListener implements Listener {
    private SwtScoutTabItem m_focusedItem;

    @Override
    public void handleEvent(Event event) {
      SwtScoutTabItem item = m_tabs.get(getSwtField().getSelection());
      switch (event.type) {
        case SWT.Selection:
          // selection changed
          setSelectedTabFromSwt();
          // focus
          if (m_focusedItem == item) {
            return;
          }
          else {
            if (m_focusedItem != null) {
              m_focusedItem.setUiFocus(false);
            }
            m_focusedItem = item;
            m_focusedItem.setUiFocus(true);
          }
          break;
        case SWT.FocusOut:
          if (m_focusedItem != null) {
            m_focusedItem.setUiFocus(false);
            m_focusedItem = null;
          }
          break;
        case SWT.FocusIn:
          if (m_focusedItem != null) {
            m_focusedItem.setUiFocus(false);
          }
          m_focusedItem = item;
          if (m_focusedItem != null) {
            m_focusedItem.setUiFocus(true);
          }
          break;
        default:
          break;
      }
    }
  }

  private class P_TabListener implements PropertyChangeListener {
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      if (evt.getPropertyName().equals(IGroupBox.PROP_VISIBLE)) {
        RunnableWithData t = new RunnableWithData() {
          @Override
          public void run() {
            IGroupBox groupBox = (IGroupBox) evt.getSource();
            if (groupBox.isVisible()) {
              showGroupBox(groupBox);
            }
            else {
              hideGroupBox(groupBox);
            }
          }
        };

        getEnvironment().invokeSwtLater(t);
      }
    }
  }

  private void showGroupBox(IGroupBox groupBox) {
    try {
      getSwtContainer().setRedraw(false);
      SwtScoutTabItem item = getTabItem(groupBox);
      if (item == null || item.isDisposed()) {
        item = new SwtScoutTabItem();
        item.createField(getSwtField(), groupBox, getEnvironment());
        m_tabs.put(item.getTabItem(), item);

      }
    }
    finally {
      getSwtContainer().setRedraw(true);
    }
  }

  private void hideGroupBox(IGroupBox groupBox) {
    try {
      getSwtContainer().setRedraw(false);
      SwtScoutTabItem item = getTabItem(groupBox);
      m_tabs.remove(item.getTabItem());
      if (item != null && item.isInitialized() && !item.isDisposed()) {
        item.getTabItem().dispose();
        item.dispose();
      }
    }
    finally {
      getSwtContainer().setRedraw(true);
    }
  }

  private SwtScoutTabItem getTabItem(IGroupBox groupBox) {
    if (groupBox == null) {
      return null;
    }
    for (SwtScoutTabItem item : m_tabs.values()) {
      if (item.getScoutObject() == groupBox) {
        return item;
      }
    }
    return null;
  }

}
