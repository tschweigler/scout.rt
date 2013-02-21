/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.data.form;

import org.eclipse.scout.commons.annotations.FormData;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.IGroupBox;
import org.eclipse.scout.rt.shared.data.form.properties.AbstractPropertyData;
import org.eclipse.scout.testing.client.form.DynamicCancelButton;
import org.eclipse.scout.testing.client.form.DynamicForm;
import org.eclipse.scout.testing.client.form.DynamicGroupBox;
import org.eclipse.scout.testing.client.form.FormHandler;
import org.eclipse.scout.testing.client.runner.ScoutClientTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * bsi ticket #98'889
 * <p>
 * Tests the import behavior of properties form a form data into its form. Untouched form data properties are not
 * expected to be imported into the form.
 */
@RunWith(ScoutClientTestRunner.class)
public class FormDataImportWithPropertiesTest {

  private static final boolean EXPECTED_BOOLEAN = true;
  private static final int EXPECTED_INTEGER = 42;
  private static final String EXPECTED_TEXT = "a test text";

  @Test
  public void testImportFormData_untouchedProperties() throws Exception {
    DynamicGroupBox mainBox = new DynamicGroupBox(new DynamicCancelButton());
    final DynamicFormWithProperties f = new DynamicFormWithProperties("Form1", mainBox);
    try {
      f.start(new FormHandler());

      // set initial values
      f.setPrimitiveBoolean(EXPECTED_BOOLEAN);
      f.setPrimitiveInteger(EXPECTED_INTEGER);
      f.setText(EXPECTED_TEXT);

      // import untouched form data
      f.importFormData(new DynamicFormDataWithProperties());

      // all form properties are expected unchanged
      Assert.assertEquals(EXPECTED_BOOLEAN, f.isPrimitiveBoolean());
      Assert.assertEquals(EXPECTED_INTEGER, f.getPrimitiveInteger());
      Assert.assertEquals(EXPECTED_TEXT, f.getText());
    }
    finally {
      f.doClose();
    }
  }

  @Test
  public void testImportFormData_modifiedProperties() throws Exception {
    DynamicGroupBox mainBox = new DynamicGroupBox(new DynamicCancelButton());
    final DynamicFormWithProperties f = new DynamicFormWithProperties("Form1", mainBox);
    try {
      f.start(new FormHandler());

      // set initial values
      f.setPrimitiveBoolean(EXPECTED_BOOLEAN);
      f.setPrimitiveInteger(EXPECTED_INTEGER);
      f.setText(EXPECTED_TEXT);

      // import modified form data
      DynamicFormDataWithProperties formData = new DynamicFormDataWithProperties();
      formData.setPrimitiveBoolean(false);
      formData.setPrimitiveInteger(102);
      formData.setText(null);
      f.importFormData(formData);

      // check properties on form
      Assert.assertFalse(f.isPrimitiveBoolean());
      Assert.assertEquals(102, f.getPrimitiveInteger());
      Assert.assertNull(f.getText());
    }
    finally {
      f.doClose();
    }
  }

  public final static class DynamicFormWithProperties extends DynamicForm {
    private boolean m_primitiveBoolean;
    private int m_primitiveInteger;
    private String m_text;

    private DynamicFormWithProperties(String title, IGroupBox mainBox) throws ProcessingException {
      super(title, mainBox);
    }

    @FormData
    public boolean isPrimitiveBoolean() {
      return m_primitiveBoolean;
    }

    @FormData
    public void setPrimitiveBoolean(boolean primitiveBoolean) {
      m_primitiveBoolean = primitiveBoolean;
    }

    @FormData
    public int getPrimitiveInteger() {
      return m_primitiveInteger;
    }

    @FormData
    public void setPrimitiveInteger(int primitiveInteger) {
      m_primitiveInteger = primitiveInteger;
    }

    @FormData
    public String getText() {
      return m_text;
    }

    @FormData
    public void setText(String text) {
      m_text = text;
    }
  }

  public final static class DynamicFormDataWithProperties extends AbstractFormData {
    private static final long serialVersionUID = 1L;

    public class PrimitiveBooleanProperty extends AbstractPropertyData<Boolean> {
      private static final long serialVersionUID = 1L;
    }

    public class PrimitiveIntegerProperty extends AbstractPropertyData<Integer> {
      private static final long serialVersionUID = 1L;
    }

    public class TextProperty extends AbstractPropertyData<String> {
      private static final long serialVersionUID = 1L;
    }

    public PrimitiveBooleanProperty getPrimitiveBooleanProperty() {
      return getPropertyByClass(PrimitiveBooleanProperty.class);
    }

    public PrimitiveIntegerProperty getPrimitiveIntegerProperty() {
      return getPropertyByClass(PrimitiveIntegerProperty.class);
    }

    public TextProperty getTextProperty() {
      return getPropertyByClass(TextProperty.class);
    }

    public boolean isPrimitiveBoolean() {
      return getPrimitiveBooleanProperty().getValue() == null ? false : getPrimitiveBooleanProperty().getValue();
    }

    public void setPrimitiveBoolean(boolean primitiveBoolean) {
      getPrimitiveBooleanProperty().setValue(primitiveBoolean);
    }

    public int getPrimitiveInteger() {
      return getPrimitiveIntegerProperty().getValue() == null ? 0 : getPrimitiveIntegerProperty().getValue();
    }

    public void setPrimitiveInteger(int primitiveInteger) {
      getPrimitiveIntegerProperty().setValue(primitiveInteger);
    }

    public String getText() {
      return getTextProperty().getValue();
    }

    public void setText(String text) {
      getTextProperty().setValue(text);
    }
  }
}
