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
package org.eclipse.scout.rt.ui.swing.ext;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;

import javax.swing.JDialog;
import javax.swing.JRootPane;

/**
 * JDialog with support of wait cursor property as a second layer over normal
 * cursor concept Using bug fixed {@link JRootPaneEx} with min/max size
 * validation
 */
public class JDialogEx extends JDialog implements IWaitSupport {
  private static final long serialVersionUID = 1L;

  private boolean m_waitCursor;

  public JDialogEx() {
    super();
  }

  public JDialogEx(Dialog d) {
    super(d);
  }

  public JDialogEx(Dialog d, boolean modal) {
    super(d, modal);
  }

  public JDialogEx(Dialog d, String title) {
    super(d, title);
  }

  public JDialogEx(Dialog d, String title, boolean modal) {
    super(d, title, modal);
  }

  public JDialogEx(Dialog d, String title, boolean modal, GraphicsConfiguration gc) {
    super(d, title, modal, gc);
  }

  public JDialogEx(Frame f) {
    super(f);
  }

  public JDialogEx(Frame f, boolean modal) {
    super(f, modal);
  }

  public JDialogEx(Frame f, String title) {
    super(f, title);
  }

  public JDialogEx(Frame f, String title, boolean modal) {
    super(f, title, modal);
  }

  public JDialogEx(Frame f, String title, boolean modal, GraphicsConfiguration gc) {
    super(f, title, modal, gc);
  }

  @Override
  protected void dialogInit() {
    super.dialogInit();
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
  }

  public boolean isWaitCursor() {
    return m_waitCursor;
  }

  public void setWaitCursor(boolean b) {
    if (b != m_waitCursor) {
      m_waitCursor = b;
      Component comp = getContentPane();
      if (m_waitCursor) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      }
      else {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
    }
  }

  @Override
  protected JRootPane createRootPane() {
    JRootPaneEx rp = new JRootPaneEx() {
      private static final long serialVersionUID = 1L;

      @Override
      protected void correctRootPaneSize(int widthDelta, int heightDelta, int preferredWidthDelta, int preferredHeightDelta) {
        if (widthDelta != 0 || heightDelta != 0) {
          JDialogEx.this.pack();
        }
      }
    };
    rp.setName("Synth.Dialog");
    return rp;
  }

}
