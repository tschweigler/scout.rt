/*******************************************************************************
 * Copyright (c) 2010,2012 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.client.ui.action;

import java.security.Permission;

import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.ConfigPropertyValue;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.annotations.Replace;
import org.eclipse.scout.commons.beans.AbstractPropertyObserver;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.action.tree.IActionNode;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.common.security.IAccessControlService;
import org.eclipse.scout.service.SERVICES;

public abstract class AbstractAction extends AbstractPropertyObserver implements IAction {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractAction.class);

  private boolean m_initialized;
  private final EventListenerList m_listenerList = new EventListenerList();
  private final IActionUIFacade m_uiFacade;
  private boolean m_inheritAccessibility;
  // enabled is defined as: enabledGranted && enabledProperty && enabledProcessing
  private boolean m_enabledGranted;
  private boolean m_enabledProperty;
  private boolean m_enabledProcessingAction;
  private boolean m_visibleProperty;
  private boolean m_visibleGranted;
  private boolean m_singleSelectionAction;
  private boolean m_multiSelectionAction;
  private boolean m_emptySpaceAction;
  private boolean m_toggleAction;

  public AbstractAction() {
    this(true);
  }

  public AbstractAction(boolean callInitializer) {
    m_uiFacade = createUIFacade();
    m_enabledGranted = true;
    m_enabledProcessingAction = true;
    m_visibleGranted = true;
    if (callInitializer) {
      callInitializer();
    }
  }

  protected void callInitializer() {
    if (!m_initialized) {
      initConfig();
      try {
        execInitAction();
      }
      catch (Throwable t) {
        LOG.warn("Action " + getClass().getName(), t);
      }
      m_initialized = true;
    }
  }

  /*
   * Configuration
   */
  @ConfigProperty(ConfigProperty.ICON_ID)
  @Order(30)
  @ConfigPropertyValue("null")
  protected String getConfiguredIconId() {
    return null;
  }

  @ConfigProperty(ConfigProperty.TEXT)
  @Order(40)
  @ConfigPropertyValue("null")
  protected String getConfiguredText() {
    return null;
  }

  @ConfigProperty(ConfigProperty.TEXT)
  @Order(50)
  @ConfigPropertyValue("null")
  protected String getConfiguredTooltipText() {
    return null;
  }

  @ConfigProperty(ConfigProperty.STRING)
  @Order(55)
  @ConfigPropertyValue("null")
  protected String getConfiguredKeyStroke() {
    return null;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(10)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredEnabled() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(20)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredVisible() {
    return true;
  }

  /**
   * @return true if {@link #prepareAction()} should in addition consider the
   *         context of the action to decide for visibility and enabled.<br>
   *         For example a menu of a table field with {@link #isInheritAccessibility()}==true is invisible when the
   *         table
   *         field is disabled or invisible
   */
  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(22)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredInheritAccessibility() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(25)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredToggleAction() {
    return false;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(60)
  @ConfigPropertyValue("true")
  protected boolean getConfiguredSingleSelectionAction() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(70)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredMultiSelectionAction() {
    return false;
  }

  /**
   * @deprecated obsolete, not used anymore. Will be removed in Release 3.10.
   */
  @Deprecated
  protected boolean getConfiguredNonSelectionAction() {
    return false;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(90)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredEmptySpaceAction() {
    return false;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(100)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredSeparator() {
    return false;
  }

  @ConfigProperty(ConfigProperty.DOC)
  @Order(110)
  @ConfigPropertyValue("null")
  protected String getConfiguredDoc() {
    return null;
  }

  /**
   * called by constructor<br>
   * this way a menu can for example add/remove custom child menus
   */
  @ConfigOperation
  @Order(10)
  protected void execInitAction() throws ProcessingException {
  }

  /**
   * called by prepareAction before action is added to list or used<br>
   * this way a menu can be made dynamically visible / enabled
   */
  @ConfigOperation
  @Order(20)
  protected void execPrepareAction() throws ProcessingException {
  }

  /**
   * called when action is performed
   */
  @ConfigOperation
  @Order(30)
  protected void execAction() throws ProcessingException {
  }

  /**
   * called whenever the selection (of a toggle-action) is changed
   */
  @ConfigOperation
  @Order(31)
  protected void execToggleAction(boolean selected) throws ProcessingException {
  }

  protected void initConfig() {
    setIconId(getConfiguredIconId());
    setText(getConfiguredText());
    setTooltipText(getConfiguredTooltipText());
    setKeyStroke(getConfiguredKeyStroke());
    setInheritAccessibility(getConfiguredInheritAccessibility());
    setEnabled(getConfiguredEnabled());
    setVisible(getConfiguredVisible());
    setToggleAction(getConfiguredToggleAction());
    setSingleSelectionAction(getConfiguredSingleSelectionAction());
    setMultiSelectionAction(getConfiguredMultiSelectionAction());
    setEmptySpaceAction(getConfiguredEmptySpaceAction());
    setSeparator(getConfiguredSeparator());
    if (isSingleSelectionAction() || isMultiSelectionAction() || isEmptySpaceAction()) {
      // ok
    }
    else {
      // legacy case of implicit new menu
      setEmptySpaceAction(true);
    }
  }

  protected IActionUIFacade createUIFacade() {
    return new P_UIFacade();
  }

  @Override
  public int acceptVisitor(IActionVisitor visitor) {
    switch (visitor.visit(this)) {
      case IActionVisitor.CANCEL:
        return IActionVisitor.CANCEL;
      case IActionVisitor.CANCEL_SUBTREE:
        return IActionVisitor.CONTINUE;
      case IActionVisitor.CONTINUE_BRANCH:
        return IActionVisitor.CANCEL;
      default:
        return IActionVisitor.CONTINUE;
    }
  }

  @Override
  public Object getProperty(String name) {
    return propertySupport.getProperty(name);
  }

  @Override
  public void setProperty(String name, Object value) {
    propertySupport.setProperty(name, value);
  }

  @Override
  public boolean hasProperty(String name) {
    return propertySupport.hasProperty(name);
  }

  @Override
  public String getActionId() {
    Class<?> c = getClass();
    while (c.isAnnotationPresent(Replace.class)) {
      c = c.getSuperclass();
    }
    String s = c.getName();
    int i = Math.max(s.lastIndexOf('$'), s.lastIndexOf('.'));
    s = s.substring(i + 1);
    return s;
  }

  @Override
  public void doAction() throws ProcessingException {
    if (isEnabled() && isVisible()) {
      try {
        setEnabledProcessingAction(false);

        execAction();
      }
      finally {
        setEnabledProcessingAction(true);
      }
    }
  }

  @Override
  public String getIconId() {
    return propertySupport.getPropertyString(PROP_ICON_ID);
  }

  @Override
  public void setIconId(String iconId) {
    propertySupport.setPropertyString(PROP_ICON_ID, iconId);
  }

  @Override
  public String getText() {
    return propertySupport.getPropertyString(PROP_TEXT);
  }

  @Override
  public void setText(String text) {
    if (text != null) {
      propertySupport.setPropertyString(PROP_TEXT, StringUtility.removeMnemonic(text));
      propertySupport.setProperty(PROP_MNEMONIC, StringUtility.getMnemonic(text));
    }
    else {
      propertySupport.setPropertyString(PROP_TEXT, null);
      propertySupport.setProperty(PROP_MNEMONIC, (char) 0x0);
    }
  }

  @Override
  public String getKeyStroke() {
    return propertySupport.getPropertyString(PROP_KEYSTROKE);
  }

  @Override
  public void setKeyStroke(String k) {
    // normalize key stroke format
    if (k != null) {
      k = k.toLowerCase();
      boolean shift = false;
      boolean ctrl = false;
      boolean alt = false;
      String key = null;
      if (k.endsWith(" ")) {
        key = " ";
      }
      for (String s : k.trim().split("[ -]")) {
        if (s.equals("shift")) {
          shift = true;
        }
        else if (s.equals("control")) {
          ctrl = true;
        }
        else if (s.equals("ctrl")) {
          ctrl = true;
        }
        else if (s.equals("strg")) {
          ctrl = true;
        }
        else if (s.equals("alt")) {
          alt = true;
        }
        else if (s.equals("alternate")) {
          alt = true;
        }
        else {
          key = s;
        }
      }
      if (key != null) {
        k = (shift ? "shift-" : "") + (ctrl ? "control-" : "") + (alt ? "alternate-" : "") + key;
      }
      else {
        k = null;
      }
    }
    propertySupport.setPropertyString(PROP_KEYSTROKE, k);
  }

  @Override
  public String getTooltipText() {
    return propertySupport.getPropertyString(PROP_TOOLTIP_TEXT);
  }

  @Override
  public void setTooltipText(String text) {
    propertySupport.setPropertyString(PROP_TOOLTIP_TEXT, text);
  }

  @Override
  public boolean isEnabled() {
    return propertySupport.getPropertyBool(PROP_ENABLED);
  }

  @Override
  public boolean isThisAndParentsEnabled() {
    if (!isEnabled()) {
      return false;
    }
    IAction temp = this;
    while (temp instanceof IActionNode) {
      temp = ((IActionNode) temp).getParent();
      if (temp == null) {
        return true;
      }
      if (!temp.isEnabled()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void setEnabled(boolean b) {
    m_enabledProperty = b;
    setEnabledInternal();
  }

  @Override
  public boolean isSelected() {
    return propertySupport.getPropertyBool(PROP_SELECTED);
  }

  @Override
  public void setSelected(boolean b) {
    boolean changed = propertySupport.setPropertyBool(PROP_SELECTED, b);
    if (changed) {
      try {
        execToggleAction(b);
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
    }
  }

  @Override
  public boolean isToggleAction() {
    return m_toggleAction;
  }

  @Override
  public void setToggleAction(boolean b) {
    m_toggleAction = b;
  }

  @Override
  public boolean isVisible() {
    return propertySupport.getPropertyBool(PROP_VISIBLE);
  }

  @Override
  public boolean isThisAndParentsVisible() {
    if (!isVisible()) {
      return false;
    }
    IAction temp = this;
    while (temp instanceof IActionNode) {
      temp = ((IActionNode) temp).getParent();
      if (temp == null) {
        return true;
      }
      if (!temp.isVisible()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void setVisible(boolean b) {
    m_visibleProperty = b;
    setVisibleInternal();
  }

  @Override
  public boolean isInheritAccessibility() {
    return m_inheritAccessibility;
  }

  @Override
  public void setInheritAccessibility(boolean b) {
    m_inheritAccessibility = b;
  }

  /**
   * Access control<br>
   * when false, overrides isEnabled with false
   */
  @Override
  public void setEnabledPermission(Permission p) {
    boolean b;
    if (p != null) {
      b = SERVICES.getService(IAccessControlService.class).checkPermission(p);
    }
    else {
      b = true;
    }
    setEnabledGranted(b);
  }

  @Override
  public boolean isEnabledGranted() {
    return m_enabledGranted;
  }

  /**
   * Access control<br>
   * when false, overrides isEnabled with false
   */
  @Override
  public void setEnabledGranted(boolean b) {
    m_enabledGranted = b;
    setEnabledInternal();
  }

  @Override
  public boolean isEnabledProcessingAction() {
    return m_enabledProcessingAction;
  }

  @Override
  public void setEnabledProcessingAction(boolean b) {
    m_enabledProcessingAction = b;
    setEnabledInternal();
  }

  private void setEnabledInternal() {
    propertySupport.setPropertyBool(PROP_ENABLED, m_enabledGranted && m_enabledProperty && m_enabledProcessingAction);
  }

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
    setVisibleInternal();
  }

  private void setVisibleInternal() {
    propertySupport.setPropertyBool(PROP_VISIBLE, m_visibleGranted && m_visibleProperty);
  }

  @Override
  public boolean isSeparator() {
    return propertySupport.getPropertyBool(PROP_SEPARATOR);
  }

  @Override
  public void setSeparator(boolean b) {
    propertySupport.setPropertyBool(PROP_SEPARATOR, b);
  }

  @Override
  public boolean isSingleSelectionAction() {
    return m_singleSelectionAction;
  }

  @Override
  public void setSingleSelectionAction(boolean b) {
    m_singleSelectionAction = b;
  }

  @Override
  public boolean isMultiSelectionAction() {
    return m_multiSelectionAction;
  }

  @Override
  public void setMultiSelectionAction(boolean b) {
    m_multiSelectionAction = b;
  }

  @Override
  public boolean isEmptySpaceAction() {
    return m_emptySpaceAction;
  }

  @Override
  public void setEmptySpaceAction(boolean b) {
    m_emptySpaceAction = b;
  }

  @Override
  public char getMnemonic() {
    Character c = (Character) propertySupport.getProperty(PROP_MNEMONIC);
    return c != null ? c.charValue() : 0x00;
  }

  @Override
  public final void prepareAction() {
    try {
      prepareActionInternal();
      execPrepareAction();
    }
    catch (Throwable t) {
      LOG.warn("Action " + getClass().getName(), t);
    }
  }

  @Override
  public IActionUIFacade getUIFacade() {
    return m_uiFacade;
  }

  /**
   * do not use this method, it is used internally by subclasses
   */
  protected void prepareActionInternal() throws ProcessingException {
  }

  protected class P_UIFacade implements IActionUIFacade {
    @Override
    public void fireActionFromUI() {
      try {
        if (isThisAndParentsEnabled() && isThisAndParentsVisible()) {
          doAction();
        }
      }
      catch (ProcessingException e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(e);
      }
      catch (Throwable e) {
        SERVICES.getService(IExceptionHandlerService.class).handleException(new ProcessingException("Unexpected exception", e));
      }
    }

    @Override
    public void setSelectedFromUI(boolean b) {
      setSelected(b);
    }
  }
}
