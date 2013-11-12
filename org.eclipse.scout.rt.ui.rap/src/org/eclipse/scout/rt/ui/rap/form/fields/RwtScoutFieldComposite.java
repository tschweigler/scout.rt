/*******************************************************************************
 * Copyright (c) 2011 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.scout.rt.ui.rap.form.fields;

import java.util.ArrayList;

import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.rt.client.ui.action.keystroke.IKeyStroke;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.sequencebox.ISequenceBox;
import org.eclipse.scout.rt.shared.data.basic.FontSpec;
import org.eclipse.scout.rt.ui.rap.LogicalGridData;
import org.eclipse.scout.rt.ui.rap.basic.RwtScoutComposite;
import org.eclipse.scout.rt.ui.rap.ext.ILabelComposite;
import org.eclipse.scout.rt.ui.rap.extension.UiDecorationExtensionPoint;
import org.eclipse.scout.rt.ui.rap.keystroke.IRwtKeyStroke;
import org.eclipse.scout.rt.ui.rap.util.RwtLayoutUtility;
import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

public abstract class RwtScoutFieldComposite<T extends IFormField> extends RwtScoutComposite<T> implements IRwtScoutFormField<T> {

  protected static final String CLIENT_PROP_INITIAL_LABEL_FONT = "scoutInitialLabelFont";
  protected static final String CLIENT_PROP_INITIAL_LABEL_BACKGROUND = "scoutInitialLabelBackground";
  protected static final String CLIENT_PROP_INITIAL_LABEL_FOREGROUND = "scoutInitialLabelForeground";

  private ILabelComposite m_label;
  private IRwtKeyStroke[] m_keyStrokes;

  private Color m_mandatoryFieldBackgroundColor;

  @Override
  public ILabelComposite getUiLabel() {
    return m_label;
  }

  protected void setUiLabel(ILabelComposite label) {
    m_label = label;
    if (m_label != null && label.getLayoutData() == null) {
      LogicalGridData statusLabelGridData = null;
      if (getScoutObject().getLabelPosition() == IFormField.LABEL_POSITION_TOP) {
        statusLabelGridData = LogicalGridDataBuilder.createLabelOnTop(getScoutObject().getGridData());
      }
      else {
        statusLabelGridData = LogicalGridDataBuilder.createLabel(getScoutObject().getGridData());
      }

      m_label.setLayoutData(statusLabelGridData);
    }
  }

  public Color getMandatoryFieldBackgroundColor() {
    return m_mandatoryFieldBackgroundColor;
  }

  protected void setErrorStatusFromScout(IProcessingStatus s) {
    if (getUiLabel() != null) {
      getUiLabel().setStatus(s);
      getUiContainer().layout(true, true);
    }
  }

  public void setMandatoryFieldBackgroundColor(Color mandatoryFieldBackgroundColor) {
    m_mandatoryFieldBackgroundColor = mandatoryFieldBackgroundColor;
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    if (getScoutObject() != null) {
      setBackgroundFromScout(getScoutObject().getBackgroundColor());
      setForegroundFromScout(getScoutObject().getForegroundColor());
      setLabelBackgroundFromScout(getScoutObject().getLabelBackgroundColor());
      setLabelForegroundFromScout(getScoutObject().getLabelForegroundColor());
      setVisibleFromScout(getScoutObject().isVisible());
      setEnabledFromScout(getScoutObject().isEnabled());
      // bsh 2010-10-01: The "mandatory" state of a SequenceBoxes is always derived from the
      // inner fields. Don't use the model value (it will always be false).
      if (!(getScoutObject() instanceof ISequenceBox)) {
        setMandatoryFromScout(getScoutObject().isMandatory());
      }
      setErrorStatusFromScout(getScoutObject().getErrorStatus());
      setLabelFromScout(getScoutObject().getLabel());
      setLabelWidthInPixelFromScout();
      setLabelVisibleFromScout();
      setLabelHorizontalAlignmentFromScout();
      setTooltipTextFromScout(getScoutObject().getTooltipText());
      if (getScoutObject().getLabelPosition() == IFormField.LABEL_POSITION_ON_FIELD && getScoutObject().getLabel() != null && getScoutObject().getTooltipText() == null) {
        setTooltipTextFromScout(getScoutObject().getLabel());
      }
      setFontFromScout(getScoutObject().getFont());
      setLabelFontFromScout(getScoutObject().getLabelFont());
      setSaveNeededFromScout(getScoutObject().isSaveNeeded());
      setFocusableFromScout(getScoutObject().isFocusable());
      updateKeyStrokesFromScout();
    }
  }

  protected void setVisibleFromScout(boolean b) {
    boolean updateLayout = false;

    if (getUiContainer() != null) {
      if (getUiContainer().getVisible() != b) {
        updateLayout = true;
        getUiContainer().setVisible(b);
      }
    }
    else if (getUiField() != null && getUiField().getVisible() != b) {
      updateLayout = true;
      getUiField().setVisible(b);
    }

    if (updateLayout && isCreated()) {
      RwtLayoutUtility.invalidateLayout(getUiEnvironment(), getUiContainer());
    }
  }

  protected void setEnabledFromScout(boolean b) {
    boolean updateLayout = false;
    Control field = getUiField();
    if (field != null) {
      updateLayout = true;
      setFieldEnabled(field, b);
      if (b) {
        setForegroundFromScout(getScoutObject().getForegroundColor());
      }
      else {
        setForegroundFromScout(UiDecorationExtensionPoint.getLookAndFeel().getColorForegroundDisabled());
      }
    }
    if (getUiLabel() != null) {
      if (getUiLabel().getEnabled() != b) {
        updateLayout = true;
        getUiLabel().setEnabled(b);
      }
    }
    if (updateLayout && isCreated()) {
      RwtLayoutUtility.invalidateLayout(getUiEnvironment(), getUiContainer());
    }
  }

  /**
   * used to change enabled into read only
   * 
   * @param field
   * @param enabled
   */
  protected void setFieldEnabled(Control field, boolean enabled) {
    field.setEnabled(enabled);
  }

  protected void setMandatoryFromScout(boolean b) {
    String fieldBackgroundColorString = UiDecorationExtensionPoint.getLookAndFeel().getMandatoryFieldBackgroundColor();
    if (fieldBackgroundColorString != null) {
      Color color = null;
      if (b) {
        color = getUiEnvironment().getColor(fieldBackgroundColorString);
      }
      else {
        color = null;
      }
      if (getMandatoryFieldBackgroundColor() != color) {
        setMandatoryFieldBackgroundColor(color);
        setBackgroundFromScout(getScoutObject().getBackgroundColor());
      }
    }
    if (getUiLabel() != null) {
      if (getUiLabel().setMandatory(b)) {
        if (isCreated()) {
          RwtLayoutUtility.invalidateLayout(getUiEnvironment(), getUiContainer());
        }
      }

      //In case of on field labels it is necessary to recompute the label visibility if the mandatory status changes.
      if (getScoutObject().getLabelPosition() == IFormField.LABEL_POSITION_ON_FIELD) {
        setLabelVisibleFromScout();
      }
    }
  }

  protected void setLabelWidthInPixelFromScout() {
    if (getUiLabel() == null) {
      return;
    }

    int w = getScoutObject().getLabelWidthInPixel();
    if (w > 0) {
      getUiLabel().setLayoutWidthHint(w);
    }
    else if (w == IFormField.LABEL_WIDTH_DEFAULT) {
      getUiLabel().setLayoutWidthHint(UiDecorationExtensionPoint.getLookAndFeel().getFormFieldLabelWidth());
    }
    else if (w == IFormField.LABEL_WIDTH_UI) {
      getUiLabel().setLayoutWidthHint(0);
    }
  }

  protected void setLabelHorizontalAlignmentFromScout() {
    // XXX not supported by swt to dynamically change style of a widget
  }

  protected void setLabelFromScout(String s) {
    if (m_label == null || s == null) {
      return;
    }

    if (getScoutObject().getLabelPosition() == IFormField.LABEL_POSITION_ON_FIELD) {
      setLabelOnField(s);
    }
    else {
      m_label.setText(s);
    }
  }

  /**
   * Uses {@link Text#setMessage(String)} to display the label on the field. Does nothing, if the {@link #getUiField()}
   * is not of type {@link Text}.
   */
  protected void setLabelOnField(String label) {
    if (getUiField() == null) {
      return;
    }
    if (label == null) {
      label = "";
    }

    if (getUiField() instanceof Text) {
      Text textField = (Text) getUiField();
      textField.setMessage(label);
    }
  }

  protected void setLabelVisibleFromScout() {
    if (m_label == null) {
      return;
    }

    boolean visible = getScoutObject().isLabelVisible();
    if (getScoutObject().getLabelPosition() == IFormField.LABEL_POSITION_ON_FIELD) {
      //Make the label as small as possible in order to show the mandatory marker (*).
      m_label.setText(null);
      m_label.setLayoutWidthHint(0);

      //Unfortunately the label can't be removed completely by only setting the width hint to 0.
      //So it is necessary to make it invisible if it is really not necessary.
      if (!getScoutObject().isMandatory()) {
        visible = false;
      }
    }
    m_label.setVisible(visible);
    if (getUiContainer() != null) {
      getUiContainer().layout(true, true);
    }
  }

  protected void setTooltipTextFromScout(String s) {
    if (getUiField() != null) {
      getUiField().setToolTipText(s);
    }
  }

  protected void setBackgroundFromScout(String scoutColor) {
    setBackgroundFromScout(scoutColor, getUiField());
  }

  protected void setBackgroundFromScout(String scoutColor, Control field) {
    if (field == null) {
      return;
    }

    if (field.getData(CLIENT_PROP_INITIAL_BACKGROUND) == null) {
      field.setData(CLIENT_PROP_INITIAL_BACKGROUND, field.getBackground());
    }

    Color color = getMandatoryFieldBackgroundColor();
    if (color != null) {
      field.setBackground(color);
      return;
    }

    Color initCol = (Color) field.getData(CLIENT_PROP_INITIAL_BACKGROUND);
    color = getUiEnvironment().getColor(scoutColor);
    if (color == null) {
      color = initCol;
    }
    //Only set the color if explicitly requested by the scout model (to respect the settings of the stylesheet)
    if (!CompareUtility.equals(color, field.getBackground())) {
      field.setBackground(color);
    }
  }

  protected void setForegroundFromScout(String scoutColor) {
    setForegroundFromScout(scoutColor, getUiField());
  }

  protected void setForegroundFromScout(String scoutColor, Control field) {
    if (field == null) {
      return;
    }

    if (field.getData(CLIENT_PROP_INITIAL_FOREGROUND) == null) {
      field.setData(CLIENT_PROP_INITIAL_FOREGROUND, field.getForeground());
    }
    Color initCol = (Color) field.getData(CLIENT_PROP_INITIAL_FOREGROUND);
    Color color = getUiEnvironment().getColor(scoutColor);
    if (color == null) {
      color = initCol;
    }
    //Only set the color if explicitly requested by the scout model (to respect the settings of the stylesheet)
    if (!CompareUtility.equals(color, field.getForeground())) {
      field.setForeground(color);
    }
  }

  protected void setFontFromScout(FontSpec scoutFont) {
    if (getUiField() != null) {
      Control fld = getUiField();
      Font currentFont = fld.getFont();
      if (fld.getData(CLIENT_PROP_INITIAL_FONT) == null) {
        fld.setData(CLIENT_PROP_INITIAL_FONT, currentFont);
      }
      Font initFont = (Font) fld.getData(CLIENT_PROP_INITIAL_FONT);
      Font f = getUiEnvironment().getFont(scoutFont, initFont);
      if (f == null) {
        f = initFont;
      }
      if (currentFont == null || !currentFont.equals(f)) {
        // only set the new font if it is different to the current one
        fld.setFont(f);
      }
    }
    if (isCreated()) {
      RwtLayoutUtility.invalidateLayout(getUiEnvironment(), getUiContainer());
    }
  }

  protected void setLabelBackgroundFromScout(String scoutColor) {
    setLabelBackgroundFromScout(scoutColor, getUiLabel());
  }

  protected void setLabelBackgroundFromScout(String scoutColor, ILabelComposite label) {
    if (label == null) {
      return;
    }

    if (label.getData(CLIENT_PROP_INITIAL_LABEL_BACKGROUND) == null) {
      label.setData(CLIENT_PROP_INITIAL_LABEL_BACKGROUND, label.getBackground());
    }

    Color color = getMandatoryFieldBackgroundColor();
    Color initCol = (Color) label.getData(CLIENT_PROP_INITIAL_LABEL_BACKGROUND);
    color = getUiEnvironment().getColor(scoutColor);
    if (color == null) {
      color = initCol;
    }
    //Only set the color if explicitly requested by the scout model (to respect the settings of the stylesheet)
    if (!CompareUtility.equals(color, label.getBackground())) {
      label.setBackground(color);
    }
  }

  protected void setLabelForegroundFromScout(String scoutColor) {
    setLabelForegroundFromScout(scoutColor, getUiLabel());
  }

  protected void setLabelForegroundFromScout(String scoutColor, ILabelComposite label) {
    if (label == null) {
      return;
    }

    if (label.getData(CLIENT_PROP_INITIAL_LABEL_FOREGROUND) == null) {
      label.setData(CLIENT_PROP_INITIAL_LABEL_FOREGROUND, label.getForeground());
    }

    Color initCol = (Color) label.getData(CLIENT_PROP_INITIAL_LABEL_FOREGROUND);
    Color color = getUiEnvironment().getColor(scoutColor);
    if (color == null) {
      color = initCol;
    }
    //Only set the color if explicitly requested by the scout model (to respect the settings of the stylesheet)
    if (!CompareUtility.equals(color, label.getForeground())) {
      label.setForeground(color);
    }
  }

  protected void setLabelFontFromScout(FontSpec scoutFont) {
    if (getUiLabel() != null) {
      ILabelComposite fld = getUiLabel();
      Font currentFont = fld.getFont();
      if (fld.getData(CLIENT_PROP_INITIAL_LABEL_FONT) == null) {
        fld.setData(CLIENT_PROP_INITIAL_LABEL_FONT, currentFont);
      }
      Font initFont = (Font) fld.getData(CLIENT_PROP_INITIAL_LABEL_FONT);
      Font f = getUiEnvironment().getFont(scoutFont, initFont);
      if (f == null) {
        f = initFont;
      }
      if (currentFont == null || !currentFont.equals(f)) {
        // only set the new font if it is different to the current one
        fld.setFont(f);
      }
    }
    if (isCreated()) {
      RwtLayoutUtility.invalidateLayout(getUiEnvironment(), getUiContainer());
    }
  }

  protected boolean isSelectAllOnFocusEnabled() {
    return UiDecorationExtensionPoint.getLookAndFeel().isFormFieldSelectAllOnFocusEnabled();
  }

  protected void setSaveNeededFromScout(boolean b) {
  }

  protected void setFocusableFromScout(boolean b) {
  }

  protected void updateEmptyFromScout() {
  }

  protected void updateKeyStrokesFromScout() {
    // key strokes
    Control widget = getUiContainer();
    if (widget == null) {
      widget = getUiField();
    }
    if (widget != null) {

      // remove old
      if (m_keyStrokes != null) {
        for (IRwtKeyStroke uiKeyStroke : m_keyStrokes) {
          getUiEnvironment().removeKeyStroke(widget, uiKeyStroke);
        }
      }

      ArrayList<IRwtKeyStroke> newUiKeyStrokes = new ArrayList<IRwtKeyStroke>();
      IKeyStroke[] scoutKeyStrokes = getScoutObject().getKeyStrokes();
      for (IKeyStroke scoutKeyStroke : scoutKeyStrokes) {
        IRwtKeyStroke[] uiStrokes = RwtUtility.getKeyStrokes(scoutKeyStroke, getUiEnvironment());
        for (IRwtKeyStroke uiStroke : uiStrokes) {
          getUiEnvironment().addKeyStroke(widget, uiStroke, false);
          newUiKeyStrokes.add(uiStroke);
        }
      }
      m_keyStrokes = newUiKeyStrokes.toArray(new IRwtKeyStroke[newUiKeyStrokes.size()]);
    }
  }

  //runs in scout job
  @Override
  protected boolean isHandleScoutPropertyChange(final String name, final Object newValue) {
    if (name.equals(IFormField.PROP_ENABLED) || name.equals(IFormField.PROP_VISIBLE)) {
      //add immediate change to rwt environment to support TAB traversal to component that changes from disabled to enabled.
      getUiEnvironment().postImmediateUiJob(new Runnable() {
        @Override
        public void run() {
          handleScoutPropertyChange(name, newValue);
        }
      });
    }
    return super.isHandleScoutPropertyChange(name, newValue);
  }

  //runs in gui thread
  @Override
  protected void handleScoutPropertyChange(String name, Object newValue) {
    super.handleScoutPropertyChange(name, newValue);
    if (name.equals(IFormField.PROP_ENABLED)) {
      setEnabledFromScout(((Boolean) newValue).booleanValue());
    }
    else if (name.equals(IFormField.PROP_FOCUSABLE)) {
      setFocusableFromScout(((Boolean) newValue).booleanValue());
    }
    else if (name.equals(IFormField.PROP_LABEL)) {
      setLabelFromScout((String) newValue);
    }
    else if (name.equals(IFormField.PROP_LABEL_VISIBLE)) {
      setLabelVisibleFromScout();
    }
    else if (name.equals(IFormField.PROP_TOOLTIP_TEXT)) {
      setTooltipTextFromScout((String) newValue);
    }
    else if (name.equals(IFormField.PROP_VISIBLE)) {
      setVisibleFromScout(((Boolean) newValue).booleanValue());
    }
    else if (name.equals(IFormField.PROP_MANDATORY)) {
      setMandatoryFromScout(((Boolean) newValue).booleanValue());
    }
    else if (name.equals(IFormField.PROP_ERROR_STATUS)) {
      setErrorStatusFromScout((IProcessingStatus) newValue);
    }
    else if (name.equals(IFormField.PROP_FOREGROUND_COLOR)) {
      setForegroundFromScout((String) newValue);
    }
    else if (name.equals(IFormField.PROP_BACKGROUND_COLOR)) {
      setBackgroundFromScout((String) newValue);
    }
    else if (name.equals(IFormField.PROP_FONT)) {
      setFontFromScout((FontSpec) newValue);
    }
    else if (name.equals(IFormField.PROP_LABEL_FOREGROUND_COLOR)) {
      setLabelForegroundFromScout((String) newValue);
    }
    else if (name.equals(IFormField.PROP_LABEL_BACKGROUND_COLOR)) {
      setLabelBackgroundFromScout((String) newValue);
    }
    else if (name.equals(IFormField.PROP_LABEL_FONT)) {
      setLabelFontFromScout((FontSpec) newValue);
    }
    else if (name.equals(IFormField.PROP_SAVE_NEEDED)) {
      setSaveNeededFromScout(((Boolean) newValue).booleanValue());
    }
    else if (name.equals(IFormField.PROP_EMPTY)) {
      updateEmptyFromScout();
    }
    else if (name.equals(IFormField.PROP_KEY_STROKES)) {
      updateKeyStrokesFromScout();
    }
  }

}
