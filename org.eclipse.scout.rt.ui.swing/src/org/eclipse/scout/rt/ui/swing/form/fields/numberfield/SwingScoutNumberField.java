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
package org.eclipse.scout.rt.ui.swing.form.fields.numberfield;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.eclipse.scout.rt.client.ui.form.fields.numberfield.INumberField;
import org.eclipse.scout.rt.ui.swing.LogicalGridLayout;
import org.eclipse.scout.rt.ui.swing.SwingUtility;
import org.eclipse.scout.rt.ui.swing.basic.document.BasicDocumentFilter;
import org.eclipse.scout.rt.ui.swing.ext.JPanelEx;
import org.eclipse.scout.rt.ui.swing.ext.JStatusLabelEx;
import org.eclipse.scout.rt.ui.swing.ext.JTextFieldEx;
import org.eclipse.scout.rt.ui.swing.form.fields.SwingScoutBasicFieldComposite;

public class SwingScoutNumberField extends SwingScoutBasicFieldComposite<INumberField<?>> implements ISwingScoutNumberField {
  private static final long serialVersionUID = 1L;

  @Override
  protected void initializeSwing() {
    JPanelEx container = new JPanelEx();
    container.setOpaque(false);
    JStatusLabelEx label = getSwingEnvironment().createStatusLabel(getScoutObject());
    container.add(label);
    JTextFieldEx textField = new JTextFieldEx();
    Document doc = textField.getDocument();
    if (doc instanceof AbstractDocument) {
      ((AbstractDocument) doc).setDocumentFilter(new BasicDocumentFilter(60));
    }
    addInputListenersForBasicField(textField, doc);
    //
    container.add(textField);
    //
    setSwingContainer(container);
    setSwingLabel(label);
    setSwingField(textField);
    // layout
    getSwingContainer().setLayout(new LogicalGridLayout(getSwingEnvironment(), 1, 0));
  }

  @Override
  public JTextField getSwingTextField() {
    return (JTextField) getSwingField();
  }

  @Override
  protected void setForegroundFromScout(String scoutColor) {
    JComponent fld = getSwingField();
    if (fld != null && scoutColor != null && fld instanceof JTextComponent) {
      setDisabledTextColor(SwingUtility.createColor(scoutColor), (JTextComponent) fld);
    }
    super.setForegroundFromScout(scoutColor);
  }

  @Override
  protected void setHorizontalAlignmentFromScout(int scoutAlign) {
    int swingAlign = SwingUtility.createHorizontalAlignment(scoutAlign);
    getSwingTextField().setHorizontalAlignment(swingAlign);
  }

  @Override
  protected void setSelectionFromSwing() {
    //Nothing to do: Selection is not stored in model for DecimalField.
  }

  @Override
  protected boolean isSelectAllOnFocusInScout() {
    return true; //No such property in Scout for DecimalField.
  }
}
