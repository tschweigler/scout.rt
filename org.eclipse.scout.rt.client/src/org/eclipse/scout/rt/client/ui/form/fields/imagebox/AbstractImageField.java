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
package org.eclipse.scout.rt.client.ui.form.fields.imagebox;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.dnd.TransferObject;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.ui.action.menu.IMenu;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractFormField;
import org.eclipse.scout.rt.shared.data.basic.AffineTransformSpec;
import org.eclipse.scout.rt.shared.data.basic.BoundsSpec;

public abstract class AbstractImageField extends AbstractFormField implements IImageField {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractImageField.class);

  private IImageFieldUIFacade m_uiFacade;
  private final EventListenerList m_listenerList = new EventListenerList();
  private IMenu[] m_menus;
  private double m_zoomDelta;
  private double m_panDelta;
  private double m_rotateDelta;

  public AbstractImageField() {
    this(true);
  }

  public AbstractImageField(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  protected int getConfiguredVerticalAlignment() {
    return 0;
  }

  @Override
  protected int getConfiguredHorizontalAlignment() {
    return 0;
  }

  @ConfigProperty(ConfigProperty.ICON_ID)
  @Order(300)
  protected String getConfiguredImageId() {
    return null;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(310)
  protected boolean getConfiguredFocusVisible() {
    return true;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(320)
  protected boolean getConfiguredAutoFit() {
    return false;
  }

  @ConfigProperty(ConfigProperty.DOUBLE)
  @Order(330)
  protected double getConfiguredZoomDelta() {
    return 1.25;
  }

  @ConfigProperty(ConfigProperty.DOUBLE)
  @Order(340)
  protected double getConfiguredPanDelta() {
    return 10;
  }

  /**
   * in degrees 0..360
   */
  @ConfigProperty(ConfigProperty.DOUBLE)
  @Order(350)
  protected double getConfiguredRotateDelta() {
    return 10;
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(360)
  protected boolean getConfiguredScrollBarEnabled() {
    return false;
  }

  @ConfigProperty(ConfigProperty.DRAG_AND_DROP_TYPE)
  @Order(400)
  protected int getConfiguredDropType() {
    return 0;
  }

  @ConfigProperty(ConfigProperty.DRAG_AND_DROP_TYPE)
  @Order(410)
  protected int getConfiguredDragType() {
    return 0;
  }

  @ConfigOperation
  @Order(500)
  protected TransferObject execDragRequest() throws ProcessingException {
    return null;
  }

  @ConfigOperation
  @Order(510)
  protected void execDropRequest(TransferObject transferObject) throws ProcessingException {
  }

  private Class<? extends IMenu>[] getConfiguredMenus() {
    Class[] dca = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    Class[] filtered = ConfigurationUtility.filterClasses(dca, IMenu.class);
    Class<IMenu>[] foca = ConfigurationUtility.sortFilteredClassesByOrderAnnotation(filtered, IMenu.class);
    return ConfigurationUtility.removeReplacedClasses(foca);
  }

  @Override
  protected void initConfig() {
    m_uiFacade = new P_UIFacade();
    super.initConfig();
    setImageTransform(new AffineTransformSpec());
    setAutoFit(getConfiguredAutoFit());
    setFocusVisible(getConfiguredFocusVisible());
    setImageId(getConfiguredImageId());
    setPanDelta(getConfiguredPanDelta());
    setRotateDelta(getConfiguredRotateDelta());
    setZoomDelta(getConfiguredZoomDelta());
    setDragType(getConfiguredDragType());
    setDropType(getConfiguredDropType());
    setScrollBarEnabled(getConfiguredScrollBarEnabled());
    // menus
    ArrayList<IMenu> menuList = new ArrayList<IMenu>();
    Class<? extends IMenu>[] a = getConfiguredMenus();
    for (int i = 0; i < a.length; i++) {
      try {
        IMenu menu = ConfigurationUtility.newInnerInstance(this, a[i]);
        menuList.add(menu);
      }
      catch (Exception e) {
        LOG.warn(null, e);
      }
    }
    try {
      injectMenusInternal(menuList);
    }
    catch (Exception e) {
      LOG.error("error occured while dynamically contributing menus.", e);
    }
    m_menus = menuList.toArray(new IMenu[0]);
  }

  /**
   * Override this internal method only in order to make use of dynamic menus<br>
   * Used to manage menu list and add/remove menus
   * 
   * @param menuList
   *          live and mutable list of configured menus
   */
  protected void injectMenusInternal(List<IMenu> menuList) {
  }

  /*
   * Runtime
   */

  /**
   * model observer
   */

  @Override
  public void addImageFieldListener(ImageFieldListener listener) {
    m_listenerList.add(ImageFieldListener.class, listener);
  }

  @Override
  public void removeImageFieldListener(ImageFieldListener listener) {
    m_listenerList.remove(ImageFieldListener.class, listener);
  }

  private void fireZoomRectangle(BoundsSpec r) {
    fireImageBoxEventInternal(new ImageFieldEvent(this, ImageFieldEvent.TYPE_ZOOM_RECTANGLE, r));
  }

  private void fireAutoFit() {
    fireImageBoxEventInternal(new ImageFieldEvent(this, ImageFieldEvent.TYPE_AUTO_FIT));
  }

  private IMenu[] firePopup() {
    ImageFieldEvent e = new ImageFieldEvent(this, ImageFieldEvent.TYPE_POPUP);
    // single observer for table-owned menus
    IMenu[] a = getMenus();
    for (int i = 0; i < a.length; i++) {
      IMenu m = a[i];
      m.prepareAction();
      if (m.isVisible()) {
        e.addPopupMenu(m);
      }
    }
    fireImageBoxEventInternal(e);
    return e.getPopupMenus();
  }

  private void fireImageBoxEventInternal(ImageFieldEvent e) {
    EventListener[] a = m_listenerList.getListeners(ImageFieldListener.class);
    if (a != null) {
      for (int i = 0; i < a.length; i++) {
        ((ImageFieldListener) a[i]).imageFieldChanged(e);
      }
    }
  }

  @Override
  public Object getImage() {
    return propertySupport.getProperty(PROP_IMAGE);
  }

  @Override
  public void setImage(Object imgObj) {
    propertySupport.setProperty(PROP_IMAGE, imgObj);
  }

  @Override
  public String getImageId() {
    return propertySupport.getPropertyString(PROP_IMAGE_ID);
  }

  @Override
  public void setImageId(String imageId) {
    propertySupport.setPropertyString(PROP_IMAGE_ID, imageId);
  }

  @Override
  public IMenu[] getMenus() {
    return m_menus;
  }

  @Override
  public double getZoomDeltaValue() {
    return m_zoomDelta;
  }

  @Override
  public void setZoomDelta(double d) {
    m_zoomDelta = d;
  }

  @Override
  public double getPanDelta() {
    return m_panDelta;
  }

  @Override
  public void setPanDelta(double d) {
    m_panDelta = d;
  }

  @Override
  public double getRotateDelta() {
    return m_rotateDelta;
  }

  @Override
  public void setRotateDelta(double deg) {
    m_rotateDelta = deg;
  }

  @Override
  public void setRotateDeltaInRadians(double rad) {
    setRotateDelta(Math.toDegrees(rad));
  }

  @Override
  public AffineTransformSpec getImageTransform() {
    return new AffineTransformSpec((AffineTransformSpec) propertySupport.getProperty(PROP_IMAGE_TRANSFORM));
  }

  @Override
  public void setImageTransform(AffineTransformSpec t) {
    propertySupport.setProperty(PROP_IMAGE_TRANSFORM, new AffineTransformSpec(t));
  }

  @Override
  public boolean isFocusVisible() {
    return propertySupport.getPropertyBool(PROP_FOCUS_VISIBLE);
  }

  @Override
  public void setFocusVisible(boolean b) {
    propertySupport.setPropertyBool(PROP_FOCUS_VISIBLE, b);
  }

  @Override
  public BoundsSpec getAnalysisRectangle() {
    return (BoundsSpec) propertySupport.getProperty(PROP_ANALYSIS_RECTANGLE);
  }

  @Override
  public void setAnalysisRectangle(BoundsSpec rect) {
    propertySupport.setProperty(PROP_ANALYSIS_RECTANGLE, rect);
  }

  @Override
  public void setAnalysisRectangle(int x, int y, int w, int h) {
    setAnalysisRectangle(new BoundsSpec(x, y, w, h));
  }

  @Override
  public boolean isAutoFit() {
    return propertySupport.getPropertyBool(PROP_AUTO_FIT);
  }

  @Override
  public void setAutoFit(boolean b) {
    propertySupport.setPropertyBool(PROP_AUTO_FIT, b);
  }

  @Override
  public boolean isScrollBarEnabled() {
    return propertySupport.getPropertyBool(PROP_SCROLL_BAR_ENABLED);
  }

  @Override
  public void setScrollBarEnabled(boolean b) {
    propertySupport.setPropertyBool(PROP_SCROLL_BAR_ENABLED, b);
  }

  @Override
  public void setDragType(int dragType) {
    propertySupport.setPropertyInt(PROP_DRAG_TYPE, dragType);
  }

  @Override
  public int getDragType() {
    return propertySupport.getPropertyInt(PROP_DRAG_TYPE);
  }

  @Override
  public void setDropType(int dropType) {
    propertySupport.setPropertyInt(PROP_DROP_TYPE, dropType);
  }

  @Override
  public int getDropType() {
    return propertySupport.getPropertyInt(PROP_DROP_TYPE);
  }

  @Override
  public byte[] getByteArrayValue() {
    Object value = getImage();
    byte[] b = null;
    if (value instanceof byte[]) {
      b = (byte[]) value;
    }
    return b;
  }

  @Override
  public void doAutoFit() {
    fireAutoFit();
  }

  @Override
  public void doZoomRectangle(int x, int y, int w, int h) {
    fireZoomRectangle(new BoundsSpec(x, y, w, h));
  }

  @Override
  public void doPan(double dx, double dy) {
    AffineTransformSpec t = getImageTransform();
    t.dx = dx;
    t.dy = dy;
    setImageTransform(t);
  }

  @Override
  public void doRelativePan(double dx, double dy) {
    AffineTransformSpec t = getImageTransform();
    t.dx = t.dx + dx;
    t.dy = t.dy + dy;
    setImageTransform(t);
  }

  @Override
  public void doZoom(double fx, double fy) {
    AffineTransformSpec t = getImageTransform();
    t.sx = fx;
    t.sy = fy;
    setImageTransform(t);
  }

  @Override
  public void doRelativeZoom(double fx, double fy) {
    AffineTransformSpec t = getImageTransform();
    t.sx = t.sx * fx;
    t.sy = t.sy * fy;
    setImageTransform(t);
  }

  @Override
  public void doRotate(double angle) {
    AffineTransformSpec t = getImageTransform();
    t.angle = angle;
    setImageTransform(t);
  }

  @Override
  public void doRelativeRotate(double angleInDegrees) {
    AffineTransformSpec t = getImageTransform();
    t.angle = t.angle + Math.toRadians(angleInDegrees);
    setImageTransform(t);
  }

  /*
   * UI accessible
   */
  @Override
  public IImageFieldUIFacade getUIFacade() {
    return m_uiFacade;
  }

  private class P_UIFacade implements IImageFieldUIFacade {

    @Override
    public void setImageTransformFromUI(AffineTransformSpec t) {
      setImageTransform(t);
    }

    @Override
    public IMenu[] firePopupFromUI() {
      return firePopup();
    }

    @Override
    public TransferObject fireDragRequestFromUI() {
      TransferObject t = null;
      try {
        t = execDragRequest();
      }
      catch (ProcessingException e) {
        LOG.warn(null, e);
      }
      return t;
    }

    @Override
    public void fireDropActionFromUi(TransferObject scoutTransferable) {
      try {
        execDropRequest(scoutTransferable);
      }
      catch (ProcessingException e) {
        LOG.warn(null, e);
      }
    }

  }// end private class

}
