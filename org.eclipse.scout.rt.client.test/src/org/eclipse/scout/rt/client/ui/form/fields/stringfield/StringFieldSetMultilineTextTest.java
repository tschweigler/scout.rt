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
package org.eclipse.scout.rt.client.ui.form.fields.stringfield;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.ui.form.AbstractForm;
import org.eclipse.scout.rt.client.ui.form.fields.button.AbstractCloseButton;
import org.eclipse.scout.rt.client.ui.form.fields.groupbox.AbstractGroupBox;
import org.eclipse.scout.rt.client.ui.form.fields.stringfield.StringFieldSetMultilineTextTest.MyForm.MainBox.GroupBox.CloseButton;
import org.eclipse.scout.rt.client.ui.form.fields.stringfield.StringFieldSetMultilineTextTest.MyForm.MainBox.GroupBox.Text1Field;
import org.eclipse.scout.testing.client.form.FormHandler;
import org.eclipse.scout.testing.client.runner.ScoutClientTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * BSI Ticket #95'042
 * <p>
 * Writing text with newlines into a single line text field must eliminate the newlines
 * </p>
 */
@RunWith(ScoutClientTestRunner.class)
public class StringFieldSetMultilineTextTest {

  @Test
  public void test() throws Exception {
    MyForm f = new MyForm();
    try {
      f.startForm();
      f.getText1Field().getUIFacade().setTextFromUI("ABC\nDEF\nGHI");
      Assert.assertEquals(f.getText1Field().getValue(), "ABC DEF GHI");
    }
    finally {
      f.doClose();
    }
  }

  public final static class MyForm extends AbstractForm {

    private MyForm() throws ProcessingException {
      super();
    }

    @Override
    protected String getConfiguredTitle() {
      return "MyForm";
    }

    @Override
    protected boolean getConfiguredModal() {
      return false;
    }

    public Text1Field getText1Field() {
      return getFieldByClass(Text1Field.class);
    }

    @Order(10)
    public class MainBox extends AbstractGroupBox {
      @Order(10)
      public class GroupBox extends AbstractGroupBox {
        @Order(10)
        public class Text1Field extends AbstractStringField {
          @Override
          protected String getConfiguredLabel() {
            return "Text 1";
          }
        }

        @Order(100)
        public class CloseButton extends AbstractCloseButton {

          @Override
          protected String getConfiguredLabel() {
            return "Close";
          }
        }
      }
    }

    public CloseButton getCloseButton() {
      return getFieldByClass(CloseButton.class);
    }

    public void startForm() throws ProcessingException {
      startInternal(new FormHandler());
    }
  }
}
