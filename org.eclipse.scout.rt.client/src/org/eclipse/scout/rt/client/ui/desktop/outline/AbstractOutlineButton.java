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
package org.eclipse.scout.rt.client.ui.desktop.outline;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.ui.desktop.DesktopEvent;
import org.eclipse.scout.rt.client.ui.desktop.DesktopListener;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractButton;

/**
 * An outline button is associated with an {@link IOutline} instance, a click on the
 * button activates the outline on the desktop.
 */
public abstract class AbstractOutlineButton extends AbstractButton {
  private IOutline m_outline;

  public AbstractOutlineButton() {
    this(true);
  }

  public AbstractOutlineButton(boolean callInitializer) {
    super(callInitializer);
  }

  /**
   * Configuration: an outline button is a toggle button.
   * 
   * @return {@code IButton.DISPLAY_STYLE_TOGGLE}
   */
  @Override
  protected int getConfiguredDisplayStyle() {
    return DISPLAY_STYLE_TOGGLE;
  }

  /**
   * Configuration: an outline button is not a process button.
   * 
   * @return {@code false}
   */
  @Override
  protected boolean getConfiguredProcessButton() {
    return false;
  }

  /**
   * Configures the outline associated with this outline button.
   * <p>
   * Subclasses can override this method. Default is {@code null}.
   * 
   * @return a type token defining an outline
   * @see IOutline
   */
  @ConfigProperty(ConfigProperty.OUTLINE)
  protected Class<? extends IOutline> getConfiguredOutline() {
    return null;
  }

  /**
   * Initializes this outline button.
   * <p>
   * This implementation does the following:
   * <ul>
   * <li>find an instance of {@code IOutline} on the desktop consistent with the configured outline of this button, this
   * becomes the associated outline instance for this button
   * <li>icon and label for this button are taken from the outline
   * <li>a property change listener is registered with the outline such that this button can react on dynamic changes of
   * its associated outline (label, icon, visible, enabled etc.)
   * </ul>
   * 
   * @throws ProcessingException
   *           if initialization fails
   */
  @Override
  protected void execInitField() throws ProcessingException {
    final IDesktop desktop = ClientSyncJob.getCurrentSession().getDesktop();
    for (IOutline o : desktop.getAvailableOutlines()) {
      if (o.getClass() == getConfiguredOutline()) {
        m_outline = o;
        break;
      }
    }
    if (m_outline != null) {
      setVisible(m_outline.isVisible());
      setEnabled(m_outline.isEnabled());
      setLabel(m_outline.getTitle());
      setTooltipText(m_outline.getTitle());
      setIconId(m_outline.getIconId());
      setSelected(desktop.getOutline() == m_outline);
      // add selection listener
      desktop.addDesktopListener(
          new DesktopListener() {
            @Override
            public void desktopChanged(DesktopEvent e) {
              switch (e.getType()) {
                case DesktopEvent.TYPE_OUTLINE_CHANGED: {
                  setSelected(e.getOutline() == m_outline);
                  break;
                }
              }
            }
          }
          );
      // add change listener
      m_outline.addPropertyChangeListener(
          new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
              String n = e.getPropertyName();
              Object v = e.getNewValue();
              if (n.equals(IOutline.PROP_VISIBLE)) {
                setVisible((Boolean) v);
              }
              else if (n.equals(IOutline.PROP_ENABLED)) {
                setEnabled((Boolean) v);
              }
              else if (n.equals(IOutline.PROP_TITLE)) {
                setLabel((String) v);
              }
              else if (n.equals(IOutline.PROP_ICON_ID)) {
                setIconId((String) v);
              }
            }
          }
          );
    }
  }

  /**
   * Activates the outline associated with this outline button (i.e. sets
   * the outline as the active outline on the desktop) if {@code selected} is {@code true}, does nothing otherwise.
   * 
   * @param selected
   *          the state of the toggle button
   */
  @Override
  protected final void execToggleAction(boolean selected) {
    if (selected) {
      IDesktop desktop = ClientSyncJob.getCurrentSession().getDesktop();
      if (desktop != null) {
        // activate outline
        desktop.setOutline(m_outline);
      }
    }
  }

}
