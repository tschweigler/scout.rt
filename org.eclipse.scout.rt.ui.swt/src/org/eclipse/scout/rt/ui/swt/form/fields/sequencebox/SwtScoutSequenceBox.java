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
package org.eclipse.scout.rt.ui.swt.form.fields.sequencebox;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.scout.commons.exception.IProcessingStatus;
import org.eclipse.scout.rt.client.ui.form.fields.ICompositeField;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.IValueField;
import org.eclipse.scout.rt.client.ui.form.fields.booleanfield.IBooleanField;
import org.eclipse.scout.rt.client.ui.form.fields.sequencebox.ISequenceBox;
import org.eclipse.scout.rt.ui.swt.LogicalGridData;
import org.eclipse.scout.rt.ui.swt.LogicalGridLayout;
import org.eclipse.scout.rt.ui.swt.ext.ILabelComposite;
import org.eclipse.scout.rt.ui.swt.ext.StatusLabelEx;
import org.eclipse.scout.rt.ui.swt.form.fields.ISwtScoutFormField;
import org.eclipse.scout.rt.ui.swt.form.fields.SwtScoutFieldComposite;
import org.eclipse.scout.rt.ui.swt.form.fields.SwtScoutFormFieldGridData;
import org.eclipse.scout.rt.ui.swt.form.fields.checkbox.ISwtScoutCheckbox;
import org.eclipse.scout.rt.ui.swt.util.SwtLayoutUtility;
import org.eclipse.swt.widgets.Composite;

/**
 * <h3>SwtScoutRangeBox</h3> ...
 * 
 * @since 1.0.0 15.04.2008
 */
public class SwtScoutSequenceBox extends SwtScoutFieldComposite<ISequenceBox> implements ISwtScoutSequenceBox {

  private PropertyChangeListener m_scoutMandatoryChangeListener;
  private PropertyChangeListener m_scoutErrorStatusChangeListener;

  @Override
  protected void initializeSwt(Composite parent) {
    Composite container = getEnvironment().getFormToolkit().createComposite(parent);
    StatusLabelEx label = getEnvironment().getFormToolkit().createStatusLabel(container, getEnvironment(), getScoutObject());

    Composite fieldContainer = getEnvironment().getFormToolkit().createComposite(container);
    int visibleCount = 0;
    for (IFormField scoutField : getScoutObject().getFields()) {
      ISwtScoutFormField swtFormField = getEnvironment().createFormField(fieldContainer, scoutField);

      ILabelComposite childLabel = null;
      if (swtFormField instanceof ISwtScoutCheckbox) {
        childLabel = ((ISwtScoutCheckbox) swtFormField).getPlaceholderLabel();
      }
      else {
        childLabel = swtFormField.getSwtLabel();
      }
      if (childLabel != null && childLabel.getLayoutData() instanceof LogicalGridData) {
        //Make the label as small as possible
        ((LogicalGridData) childLabel.getLayoutData()).widthHint = 0;

        //Remove the label completely if the formField doesn't actually have a label on the left side
        if (removeLabelCompletely(swtFormField)) {
          childLabel.setVisible(false);
        }

        // <bsh 2010-10-01>
        // Force an empty, but visible label for all but the first visible fields
        // within an SequenceBox. This empty status label is necessary to show the
        // "mandatory" indicator.
        if (swtFormField.getScoutObject() instanceof IFormField) {
          IFormField childScoutField = ((IFormField) swtFormField.getScoutObject());
          if (childScoutField.isVisible()) {
            visibleCount++;
          }
          if (visibleCount > 1) {
            if (!childLabel.getVisible()) {
              // make the label visible, but clear any text
              childLabel.setVisible(true);
              childLabel.setText("");
            }
          }
        }
        // <bsh>
      }
      // create layout constraints
      SwtScoutFormFieldGridData data = new SwtScoutFormFieldGridData(scoutField) {
        @Override
        public void validate() {
          super.validate();
          useUiWidth = !getScoutObject().isEqualColumnWidths();
          useUiHeight = true;
        }
      };
      swtFormField.getSwtContainer().setLayoutData(data);

    }
    //
    setSwtContainer(container);
    setSwtLabel(label);
    setSwtField(fieldContainer);

    // layout
    fieldContainer.setLayout(new LogicalGridLayout(getGridColumnGapInPixel(), 0));
    container.setLayout(new LogicalGridLayout(1, 0));
  }

  protected int getGridColumnGapInPixel() {
    return 6;
  }

  private boolean removeLabelCompletely(ISwtScoutFormField swtScoutFormField) {
    if (swtScoutFormField == null) {
      return false;
    }

    if (!(swtScoutFormField.getScoutObject() instanceof IFormField)) {
      return false;
    }

    IFormField formField = ((IFormField) swtScoutFormField.getScoutObject());
    if (formField.getLabelPosition() == IFormField.LABEL_POSITION_ON_FIELD) {
      return true;
    }

    if (formField instanceof IBooleanField) {
      return true;
    }

    return false;
  }

  @Override
  public Composite getSwtField() {
    return (Composite) super.getSwtField();
  }

  @Override
  public ISequenceBox getScoutObject() {
    return super.getScoutObject();
  }

  protected void setChildMandatoryFromScout() {
    boolean inheritedMandatory = false;
    List<IValueField<?>> visibleFields = findVisibleValueFields(getScoutObject());
    for (IValueField<?> field : visibleFields) {
      if (field.isMandatory()) {
        inheritedMandatory = true;
        break;
      }
    }
    setMandatoryFromScout(inheritedMandatory);
  }

  protected void setChildErrorStatusFromScout() {
    IProcessingStatus inheritedErrorStatus = null;
    List<IValueField<?>> visibleFields = findVisibleValueFields(getScoutObject());
    if (visibleFields.size() > 0) {
      // bsh 2010-10-01: don't inherit error status from other fields than the first visible field
      inheritedErrorStatus = visibleFields.get(0).getErrorStatus();
    }
    setErrorStatusFromScout(inheritedErrorStatus);
  }

  protected List<IValueField<?>> findVisibleValueFields(ICompositeField parent) {
    List<IValueField<?>> valueFields = new LinkedList<IValueField<?>>();
    for (IFormField field : parent.getFields()) {
      if (!field.isVisible()) {
        continue;
      }
      if (field instanceof IValueField) {
        valueFields.add((IValueField<?>) field);
      }
      else if (field instanceof ICompositeField) {
        valueFields.addAll(findVisibleValueFields((ICompositeField) field));
      }
    }
    return valueFields;
  }

  @Override
  protected void setEnabledFromScout(boolean b) {
    boolean updateLayout = false;
    // only label
    if (getSwtLabel() != null) {
      if (getSwtLabel().getEnabled() != b) {
        updateLayout = true;
        getSwtLabel().setEnabled(b);
      }
    }
    if (updateLayout && isConnectedToScout()) {
      SwtLayoutUtility.invalidateLayout(getSwtContainer());
    }
  }

  @Override
  protected void attachScout() {
    super.attachScout();
    // add mandatory change listener on children to decorate my label same as any mandatory child
    m_scoutMandatoryChangeListener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Runnable j = new Runnable() {
          @Override
          public void run() {
            if (getSwtContainer() != null && !getSwtContainer().isDisposed()) {
              setChildMandatoryFromScout();
            }
          }
        };
        getEnvironment().invokeSwtLater(j);
      }
    };
    for (IFormField f : getScoutObject().getFields()) {
      f.addPropertyChangeListener(IFormField.PROP_MANDATORY, m_scoutMandatoryChangeListener);
    }
    // add error status change listener on children to decorate my label same as any child with an error status
    m_scoutErrorStatusChangeListener = new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent e) {
        Runnable j = new Runnable() {
          @Override
          public void run() {
            setChildErrorStatusFromScout();
          }
        };
        getEnvironment().invokeSwtLater(j);
      }
    };
    for (IFormField f : getScoutObject().getFields()) {
      f.addPropertyChangeListener(IFormField.PROP_ERROR_STATUS, m_scoutErrorStatusChangeListener);
    }
    setChildMandatoryFromScout();
  }

  @Override
  protected void detachScout() {
    if (m_scoutMandatoryChangeListener != null) {
      for (IFormField f : getScoutObject().getFields()) {
        f.removePropertyChangeListener(IFormField.PROP_MANDATORY, m_scoutMandatoryChangeListener);
      }
      m_scoutMandatoryChangeListener = null;
    }

    if (m_scoutErrorStatusChangeListener != null) {
      for (IFormField f : getScoutObject().getFields()) {
        f.removePropertyChangeListener(IFormField.PROP_ERROR_STATUS, m_scoutErrorStatusChangeListener);
      }
      m_scoutErrorStatusChangeListener = null;
    }

    super.detachScout();
  }
}
