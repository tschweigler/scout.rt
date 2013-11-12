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
package org.eclipse.scout.rt.ui.swt.form.fields.decimalfield;

import org.eclipse.scout.rt.client.ui.form.fields.decimalfield.IDecimalField;
import org.eclipse.scout.rt.ui.swt.LogicalGridLayout;
import org.eclipse.scout.rt.ui.swt.ext.StatusLabelEx;
import org.eclipse.scout.rt.ui.swt.form.fields.SwtScoutBasicFieldComposite;
import org.eclipse.scout.rt.ui.swt.internal.TextFieldEditableSupport;
import org.eclipse.scout.rt.ui.swt.util.SwtUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * <h3>SwtScoutDoubleField</h3> ...
 * 
 * @since 1.0.0 14.04.2008
 */
public class SwtScoutDecimalField extends SwtScoutBasicFieldComposite<IDecimalField<?>> implements ISwtScoutDecimalField {

  @Override
  protected void initializeSwt(Composite parent) {
    Composite container = getEnvironment().getFormToolkit().createComposite(parent);
    StatusLabelEx label = getEnvironment().getFormToolkit().createStatusLabel(container, getEnvironment(), getScoutObject());

    int style = SWT.BORDER;
    style |= SwtUtility.getVerticalAlignment(getScoutObject().getGridData().verticalAlignment);
    style |= SwtUtility.getHorizontalAlignment(getScoutObject().getGridData().horizontalAlignment);

    Text text = getEnvironment().getFormToolkit().createText(container, style);
    text.setTextLimit(32);
    //
    setSwtContainer(container);
    setSwtLabel(label);
    setSwtField(text);
    //listeners
    addModifyListenerForBasicField(text);
    // layout
    getSwtContainer().setLayout(new LogicalGridLayout(1, 0));
  }

  @Override
  public Text getSwtField() {
    return (Text) super.getSwtField();
  }

  @Override
  protected String getText() {
    return getSwtField().getText();
  }

  @Override
  protected void setText(String text) {
    getSwtField().setText(text);
  }

  @Override
  protected Point getSelection() {
    return getSwtField().getSelection();
  }

  @Override
  protected void setSelection(int startIndex, int endIndex) {
    getSwtField().setSelection(startIndex, endIndex);
  }

  @Override
  protected TextFieldEditableSupport createEditableSupport() {
    return new TextFieldEditableSupport(getSwtField());
  }

  @Override
  protected int getCaretOffset() {
    return getSwtField().getCaretPosition();
  }

  @Override
  protected void setCaretOffset(int caretPosition) {
    //nothing to do: SWT sets the caret itself. If startIndex > endIndex it is placed at the beginning.
  }

}
