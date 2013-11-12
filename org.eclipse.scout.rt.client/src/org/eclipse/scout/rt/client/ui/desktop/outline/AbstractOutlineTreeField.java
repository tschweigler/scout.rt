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

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ClientSyncJob;
import org.eclipse.scout.rt.client.ui.basic.tree.ITree;
import org.eclipse.scout.rt.client.ui.desktop.DesktopEvent;
import org.eclipse.scout.rt.client.ui.desktop.DesktopListener;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.form.fields.treefield.AbstractTreeField;

public abstract class AbstractOutlineTreeField extends AbstractTreeField {
  private DesktopListener m_desktopListener;
  private PropertyChangeListener m_treePropertyListener;

  public AbstractOutlineTreeField() {
    this(true);
  }

  public AbstractOutlineTreeField(boolean callInitializer) {
    super(callInitializer);
  }

  @Override
  protected boolean getConfiguredLabelVisible() {
    return false;
  }

  @Override
  protected void execInitField() throws ProcessingException {
    m_desktopListener = new DesktopListener() {
      @Override
      public void desktopChanged(DesktopEvent e) {
        switch (e.getType()) {
          case DesktopEvent.TYPE_OUTLINE_CHANGED: {
            installOutline(e.getOutline());
            break;
          }
        }
      }
    };
    m_treePropertyListener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals(ITree.PROP_TITLE)) {
          setLabel((String) e.getNewValue());
        }
      }
    };
    //
    IDesktop desktop = ClientSyncJob.getCurrentSession().getDesktop();
    desktop.addDesktopListener(m_desktopListener);
    installOutline(desktop.getOutline());
  }

  @Override
  protected void execDisposeField() throws ProcessingException {
    ClientSyncJob.getCurrentSession().getDesktop().removeDesktopListener(m_desktopListener);
    m_desktopListener = null;
  }

  private void installOutline(IOutline outline) {
    if (getTree() == outline) {
      return;
    }
    //
    if (getTree() != null) {
      getTree().removePropertyChangeListener(m_treePropertyListener);
      setLabel(null);
    }
    setTree(outline, true);
    if (getTree() != null) {
      getTree().addPropertyChangeListener(m_treePropertyListener);
      setLabel(getTree().getTitle());
    }
  }
}
