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
package org.eclipse.scout.rt.ui.rap.form.fields.listbox;

import org.eclipse.rwt.lifecycle.WidgetUtil;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.IListBox;
import org.eclipse.scout.rt.ui.rap.LogicalGridLayout;
import org.eclipse.scout.rt.ui.rap.basic.table.RwtScoutTable;
import org.eclipse.scout.rt.ui.rap.core.LogicalGridData;
import org.eclipse.scout.rt.ui.rap.core.basic.IRwtScoutComposite;
import org.eclipse.scout.rt.ui.rap.ext.StatusLabelEx;
import org.eclipse.scout.rt.ui.rap.ext.table.TableEx;
import org.eclipse.scout.rt.ui.rap.extension.UiDecorationExtensionPoint;
import org.eclipse.scout.rt.ui.rap.form.fields.LogicalGridDataBuilder;
import org.eclipse.scout.rt.ui.rap.form.fields.RwtScoutValueFieldComposite;
import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * <h3>RwtScoutListBox</h3> ...
 * 
 * @since 3.7.0 June 2011
 */
public class RwtScoutListBox extends RwtScoutValueFieldComposite<IListBox<?>> implements IRwtScoutListBox {

  private RwtScoutTable m_tableComposite;
  private Composite m_tableContainer;

  @Override
  protected void initializeUi(Composite parent) {
    Composite container = getUiEnvironment().getFormToolkit().createComposite(parent);
    int labelStyle = UiDecorationExtensionPoint.getLookAndFeel().getFormFieldLabelAlignment();
    StatusLabelEx label = new StatusLabelEx(container, labelStyle);
    getUiEnvironment().getFormToolkit().getFormToolkit().adapt(label, false, false);
    Composite tableContainer = new Composite(container, SWT.NONE);
    tableContainer.setLayout(new LogicalGridLayout(1, 0));
    tableContainer.setData(WidgetUtil.CUSTOM_VARIANT, RwtUtility.VARIANT_LISTBOX);
    m_tableContainer = tableContainer;
    m_tableComposite = new RwtScoutTable(RwtUtility.VARIANT_LISTBOX);
    m_tableComposite.createUiField(tableContainer, getScoutObject().getTable(), getUiEnvironment());
    LogicalGridData fieldData = LogicalGridDataBuilder.createField(getScoutObject().getGridData());
    // filter box
    IFormField[] childFields = getScoutObject().getFields();
    if (childFields.length > 0) {
      IRwtScoutComposite filterComposite = getUiEnvironment().createFormField(container, childFields[0]);
      LogicalGridData filterData = LogicalGridDataBuilder.createField(childFields[0].getGridData());
      filterData.gridx = fieldData.gridx;
      filterData.gridy = fieldData.gridy + fieldData.gridh;
      filterData.gridw = fieldData.gridw;
      filterData.weightx = fieldData.weightx;
      filterComposite.getUiContainer().setLayoutData(filterData);
    }
    //
    setUiContainer(container);
    setUiLabel(label);
    TableEx tableField = m_tableComposite.getUiField();
    tableContainer.setLayoutData(fieldData);
    setUiField(tableField);

    // layout
    getUiContainer().setLayout(new LogicalGridLayout(1, 0));
  }

  /**
   * complete override
   */
  @Override
  protected void setFieldEnabled(Control uiField, boolean b) {
    m_tableComposite.setEnabledFromScout(b);
  }

  @Override
  protected void setEnabledFromScout(boolean b) {
    super.setEnabledFromScout(b);
    // Workaround, because ":disabled" state seems to be ignored by RAP
    if (m_tableContainer != null) {
      m_tableContainer.setData(WidgetUtil.CUSTOM_VARIANT, (b ? RwtUtility.VARIANT_LISTBOX : RwtUtility.VARIANT_LISTBOX_DISABLED));
    }
  }
}