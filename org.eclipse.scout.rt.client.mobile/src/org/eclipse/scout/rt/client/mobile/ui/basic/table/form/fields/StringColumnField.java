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
package org.eclipse.scout.rt.client.mobile.ui.basic.table.form.fields;

import org.eclipse.scout.rt.client.ui.basic.table.columns.IStringColumn;
import org.eclipse.scout.rt.client.ui.form.fields.stringfield.AbstractStringField;

/**
 * @since 3.9.0
 */
public class StringColumnField extends AbstractStringField implements IColumnWrapper<IStringColumn> {
  private StringColumnFieldPropertyDelegator m_propertyDelegator;

  public StringColumnField(IStringColumn column) {
    super(false);
    m_propertyDelegator = new StringColumnFieldPropertyDelegator(column, this);
    callInitializer();
  }

  @Override
  protected void initConfig() {
    super.initConfig();

    m_propertyDelegator.init();
  }

  @Override
  public IStringColumn getWrappedObject() {
    return m_propertyDelegator.getSender();
  }

  @Override
  protected int getConfiguredGridH() {
    //If text wrap is set to true the field typically contains a lot of information.
    //Therefore make it bigger so the user can read it.
    if (getWrappedObject().isTextWrap()) {
      return 2;
    }

    return super.getConfiguredGridH();
  }
}
