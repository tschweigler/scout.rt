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
package org.eclipse.scout.rt.ui.rap.form.fields.datefield;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.scout.commons.CompareUtility;
import org.eclipse.scout.commons.holders.Holder;
import org.eclipse.scout.commons.job.JobEx;
import org.eclipse.scout.rt.client.ui.form.fields.IFormField;
import org.eclipse.scout.rt.client.ui.form.fields.datefield.IDateField;
import org.eclipse.scout.rt.ui.rap.LogicalGridLayout;
import org.eclipse.scout.rt.ui.rap.ext.ButtonEx;
import org.eclipse.scout.rt.ui.rap.ext.StatusLabelEx;
import org.eclipse.scout.rt.ui.rap.ext.StyledTextEx;
import org.eclipse.scout.rt.ui.rap.ext.custom.StyledText;
import org.eclipse.scout.rt.ui.rap.form.fields.IPopupSupport;
import org.eclipse.scout.rt.ui.rap.form.fields.LogicalGridDataBuilder;
import org.eclipse.scout.rt.ui.rap.form.fields.RwtScoutValueFieldComposite;
import org.eclipse.scout.rt.ui.rap.form.fields.datefield.chooser.DateChooserDialog;
import org.eclipse.scout.rt.ui.rap.internal.TextFieldEditableSupport;
import org.eclipse.scout.rt.ui.rap.keystroke.RwtKeyStroke;
import org.eclipse.scout.rt.ui.rap.util.RwtLayoutUtility;
import org.eclipse.scout.rt.ui.rap.util.RwtUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class RwtScoutDateField extends RwtScoutValueFieldComposite<IDateField> implements IRwtScoutDateField, IPopupSupport {

  private Button m_dropDownButton;
  private TextFieldEditableSupport m_editableSupport;

  private Set<IPopupSupportListener> m_popupEventListeners;
  private Object m_popupEventListenerLock;

  private boolean m_ignoreLabel = false;
  private Composite m_dateContainer;
  private boolean m_dateTimeCompositeMember;
  private String m_displayTextToVerify;
  private DateChooserDialog m_dateChooserDialog = null;
  private FocusAdapter m_textFieldFocusAdapter = null;

  @Override
  public void setIgnoreLabel(boolean ignoreLabel) {
    m_ignoreLabel = ignoreLabel;
    if (ignoreLabel) {
      getUiLabel().setVisible(false);
    }
    else {
      getUiLabel().setVisible(getScoutObject().isLabelVisible());
    }
  }

  public boolean isIgnoreLabel() {
    return m_ignoreLabel;
  }

  public boolean isDateTimeCompositeMember() {
    return m_dateTimeCompositeMember;
  }

  @Override
  public void setDateTimeCompositeMember(boolean dateTimeCompositeMember) {
    m_dateTimeCompositeMember = dateTimeCompositeMember;
  }

  @Override
  protected void initializeUi(Composite parent) {
    m_popupEventListeners = new HashSet<IPopupSupportListener>();
    m_popupEventListenerLock = new Object();

    Composite container = getUiEnvironment().getFormToolkit().createComposite(parent);
    StatusLabelEx label = getUiEnvironment().getFormToolkit().createStatusLabel(container, getScoutObject());

    m_dateContainer = getUiEnvironment().getFormToolkit().createComposite(container, SWT.BORDER);
    m_dateContainer.setData(RWT.CUSTOM_VARIANT, VARIANT_DATEFIELD);

    StyledText textField = new StyledTextEx(m_dateContainer, SWT.SINGLE);
    getUiEnvironment().getFormToolkit().adapt(textField, false, false);
    textField.setData(RWT.CUSTOM_VARIANT, VARIANT_DATEFIELD);

    ButtonEx dateChooserButton = getUiEnvironment().getFormToolkit().createButtonEx(m_dateContainer, SWT.PUSH | SWT.NO_FOCUS);
    dateChooserButton.setData(RWT.CUSTOM_VARIANT, VARIANT_DATEFIELD);
    m_dateContainer.setTabList(new Control[]{textField});
    container.setTabList(new Control[]{m_dateContainer});

    // key strokes on container
    getUiEnvironment().addKeyStroke(container, new P_DateChooserOpenKeyStroke(), false);

    // key strokes on field
    getUiEnvironment().addKeyStroke(textField, new P_ShiftDayUpKeyStroke(), false);
    getUiEnvironment().addKeyStroke(textField, new P_ShiftDayDownKeyStroke(), false);
    getUiEnvironment().addKeyStroke(textField, new P_ShiftMonthUpKeyStroke(), false);
    getUiEnvironment().addKeyStroke(textField, new P_ShiftMonthDownKeyStroke(), false);
    getUiEnvironment().addKeyStroke(textField, new P_ShiftYearUpKeyStroke(), false);
    getUiEnvironment().addKeyStroke(textField, new P_ShiftYearDownKeyStroke(), false);

    // listener
    dateChooserButton.addListener(ButtonEx.SELECTION_ACTION, new P_RwtBrowseButtonListener());
    attachFocusListener(textField, true);
    textField.addMouseListener(new MouseAdapter() {
      private static final long serialVersionUID = 1L;

      @Override
      public void mouseUp(MouseEvent e) {
        handleUiDateChooserAction();
      }
    });
    //
    setUiContainer(container);
    setUiLabel(label);
    setDropDownButton(dateChooserButton);
    setUiField(textField);

    // layout
    container.setLayout(new LogicalGridLayout(1, 0));

    m_dateContainer.setLayoutData(LogicalGridDataBuilder.createField(((IFormField) getScoutObject()).getGridData()));
    m_dateContainer.setLayout(RwtLayoutUtility.createGridLayoutNoSpacing(2, false));

    GridData textLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    textField.setLayoutData(textLayoutData);

    GridData buttonLayoutData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
    buttonLayoutData.heightHint = 20;
    buttonLayoutData.widthHint = 20;
    dateChooserButton.setLayoutData(buttonLayoutData);
  }

  @Override
  public Button getDropDownButton() {
    return m_dropDownButton;
  }

  public void setDropDownButton(ButtonEx b) {
    m_dropDownButton = b;
  }

  @Override
  public StyledTextEx getUiField() {
    return (StyledTextEx) super.getUiField();
  }

  public boolean isFocusInDatePicker() {
    if (m_dateChooserDialog == null) {
      return false;
    }

    Control focusControl = getUiEnvironment().getDisplay().getFocusControl();
    boolean isFocusInDatePicker = RwtUtility.isAncestorOf(m_dateChooserDialog.getShell(), focusControl);
    return isFocusInDatePicker;
  }

  private void installFocusListenerOnTextField() {
    if (getUiField().isDisposed()) {
      return;
    }

    getUiField().setFocus();
    if (m_textFieldFocusAdapter == null) {
      m_textFieldFocusAdapter = new FocusAdapter() {
        private static final long serialVersionUID = 1L;

        @Override
        public void focusLost(FocusEvent e) {
          handleUiFocusLostOnDatePickerPopup(e);
        }
      };
    }
    getUiField().addFocusListener(m_textFieldFocusAdapter);
  }

  /**
   * The event is fired only if the datepicker popup is open.
   * <p>
   * The default sets the focus on the ui field if the new focus is inside the date picker. <br/>
   * If the new focus is outside the date picker it makes sure the date picker popup will be closed.
   * </p>
   */
  protected void handleUiFocusLostOnDatePickerPopup(FocusEvent event) {
    if (isFocusInDatePicker()) {
      getUiEnvironment().getDisplay().asyncExec(new Runnable() {

        @Override
        public void run() {
          getUiField().setFocus();
        }

      });
    }
    else {
      getUiEnvironment().getDisplay().asyncExec(new Runnable() {

        @Override
        public void run() {
          makeSureDateChooserIsClosed();
        }

      });
    }
  }

  private void uninstallFocusListenerOnTextField() {
    if (!getUiField().isDisposed() && m_textFieldFocusAdapter != null) {
      getUiField().removeFocusListener(m_textFieldFocusAdapter);
    }
  }

  @Override
  protected void setEnabledFromScout(boolean b) {
    super.setEnabledFromScout(b);
    m_dropDownButton.setEnabled(b);
    if (b) {
      m_dateContainer.setData(RWT.CUSTOM_VARIANT, VARIANT_DATEFIELD);
    }
    else {
      m_dateContainer.setData(RWT.CUSTOM_VARIANT, VARIANT_DATEFIELD_DISABLED);
    }
  }

  @Override
  protected void setLabelVisibleFromScout() {
    if (!isIgnoreLabel()) {
      super.setLabelVisibleFromScout();
    }
  }

  @Override
  protected void setFieldEnabled(Control rwtField, boolean enabled) {
    if (m_editableSupport == null) {
      m_editableSupport = new TextFieldEditableSupport(getUiField());
    }
    m_editableSupport.setEditable(enabled);
  }

  @Override
  protected void setDisplayTextFromScout(String s) {
    IDateField scoutField = getScoutObject();
    if (s == null) {
      s = "";
    }
    m_displayTextToVerify = s;
    Date value = scoutField.getValue();
    if (value != null) {
      DateFormat format = scoutField.getIsolatedDateFormat();
      if (format != null) {
        m_displayTextToVerify = format.format(value);
      }
    }
    getUiField().setText(m_displayTextToVerify);
    getUiField().setCaretOffset(0);
  }

  @Override
  protected void setBackgroundFromScout(String scoutColor) {
    setBackgroundFromScout(scoutColor, m_dateContainer);
  }

  @Override
  protected void handleUiInputVerifier(boolean doit) {
    if (!doit) {
      return;
    }
    final String text = getUiField().getText();
    // only handle if text has changed
    if (CompareUtility.equals(text, m_displayTextToVerify) && (isDateTimeCompositeMember() || getScoutObject().getErrorStatus() == null)) {
      return;
    }
    m_displayTextToVerify = text;
    final Holder<Boolean> result = new Holder<Boolean>(Boolean.class, false);
    // notify Scout
    Runnable t = new Runnable() {
      @Override
      public void run() {
        boolean b = getScoutObject().getUIFacade().setDateTextFromUI(text);
        result.setValue(b);
      }
    };
    JobEx job = getUiEnvironment().invokeScoutLater(t, 0);
    try {
      job.join(2345);
    }
    catch (InterruptedException e) {
      //nop
    }
    getUiEnvironment().dispatchImmediateUiJobs();
  }

  @Override
  protected void handleUiFocusGained() {
    if (isSelectAllOnFocusEnabled()) {
      getUiField().setSelection(0, getUiField().getText().length());
    }
  }

  private void notifyPopupEventListeners(int eventType) {
    IPopupSupportListener[] listeners;
    synchronized (m_popupEventListenerLock) {
      listeners = m_popupEventListeners.toArray(new IPopupSupportListener[m_popupEventListeners.size()]);
    }
    for (IPopupSupportListener listener : listeners) {
      listener.handleEvent(eventType);
    }
  }

  @Override
  public void addPopupEventListener(IPopupSupportListener listener) {
    synchronized (m_popupEventListenerLock) {
      m_popupEventListeners.add(listener);
    }
  }

  @Override
  public void removePopupEventListener(IPopupSupportListener listener) {
    synchronized (m_popupEventListenerLock) {
      m_popupEventListeners.remove(listener);
    }
  }

  protected void makeSureDateChooserIsClosed() {
    if (m_dateChooserDialog != null
        && m_dateChooserDialog.getShell() != null
        && !m_dateChooserDialog.getShell().isDisposed()) {
      m_dateChooserDialog.getShell().close();
    }

    uninstallFocusListenerOnTextField();
  }

  private void handleUiDateChooserAction() {
    if (!getDropDownButton().isVisible() || !getDropDownButton().isEnabled()) {
      return;
    }

    Date oldDate = getScoutObject().getValue();
    if (oldDate == null) {
      oldDate = new Date();
    }

    notifyPopupEventListeners(IPopupSupportListener.TYPE_OPENING);

    makeSureDateChooserIsClosed();
    m_dateChooserDialog = createDateChooserDialog(getUiField().getShell(), oldDate);
    if (m_dateChooserDialog != null) {

      m_dateChooserDialog.getShell().addDisposeListener(new P_DateChooserDisposeListener());

      m_dateChooserDialog.openDateChooser(getUiField());
      installFocusListenerOnTextField();
    }
  }

  protected DateChooserDialog createDateChooserDialog(Shell parentShell, Date currentDate) {
    return new DateChooserDialog(parentShell, currentDate);
  }

  private void getDateFromClosedDateChooserDialog() {
    boolean setFocusToUiField = false;
    try {
      final Date newDate = m_dateChooserDialog.getReturnDate();
      if (newDate != null) {
        setFocusToUiField = true;
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            getScoutObject().getUIFacade().setDateFromUI(newDate);
          }
        };
        getUiEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
    }
    finally {
      notifyPopupEventListeners(IPopupSupportListener.TYPE_CLOSED);
      uninstallFocusListenerOnTextField();
      if (setFocusToUiField
          && !getUiField().isDisposed()) {
        getUiField().setFocus();
      }
    }
  }

  private final class P_DateChooserDisposeListener implements DisposeListener {
    private static final long serialVersionUID = 1L;

    @Override
    public void widgetDisposed(DisposeEvent event) {
      getDateFromClosedDateChooserDialog();
      m_dateChooserDialog = null;
    }
  }

  private class P_RwtBrowseButtonListener implements Listener {
    private static final long serialVersionUID = 1L;

    public P_RwtBrowseButtonListener() {
    }

    @Override
    public void handleEvent(Event event) {
      switch (event.type) {
        case ButtonEx.SELECTION_ACTION:
          getUiField().forceFocus();
          handleUiDateChooserAction();
          break;
        default:
          break;
      }
    }
  } // end class P_RwtBrowseButtonListener

  private void shiftDate(final int level, final int value) {
    if (getUiField().isDisposed()) {
      return;
    }
    if (getUiField().isEnabled()
        && getUiField().getEditable()
        && getUiField().isVisible()) {
      if (level >= 0) {
        // notify Scout
        Runnable t = new Runnable() {
          @Override
          public void run() {
            getScoutObject().getUIFacade().fireDateShiftActionFromUI(level, value);
          }
        };
        getUiEnvironment().invokeScoutLater(t, 0);
        // end notify
      }
    }
  }

  private class P_ShiftDayUpKeyStroke extends RwtKeyStroke {
    public P_ShiftDayUpKeyStroke() {
      super(SWT.ARROW_UP);
    }

    @Override
    public void handleUiAction(Event e) {
      int level = 0;
      int value = 1;
      shiftDate(level, value);
    }
  }

  private class P_ShiftDayDownKeyStroke extends RwtKeyStroke {
    public P_ShiftDayDownKeyStroke() {
      super(SWT.ARROW_DOWN);
    }

    @Override
    public void handleUiAction(Event e) {
      int level = 0;
      int value = -1;
      shiftDate(level, value);
    }
  }

  private class P_ShiftMonthUpKeyStroke extends RwtKeyStroke {
    public P_ShiftMonthUpKeyStroke() {
      super(SWT.ARROW_UP, SWT.SHIFT);
    }

    @Override
    public void handleUiAction(Event e) {
      int level = 1;
      int value = 1;
      shiftDate(level, value);
    }
  }

  private class P_ShiftMonthDownKeyStroke extends RwtKeyStroke {
    public P_ShiftMonthDownKeyStroke() {
      super(SWT.ARROW_DOWN, SWT.SHIFT);
    }

    @Override
    public void handleUiAction(Event e) {
      int level = 1;
      int value = -1;
      shiftDate(level, value);
    }
  }

  private class P_ShiftYearUpKeyStroke extends RwtKeyStroke {
    public P_ShiftYearUpKeyStroke() {
      super(SWT.ARROW_UP, SWT.CONTROL);
    }

    @Override
    public void handleUiAction(Event e) {
      int level = 2;
      int value = 1;
      shiftDate(level, value);
    }
  }

  private class P_ShiftYearDownKeyStroke extends RwtKeyStroke {
    public P_ShiftYearDownKeyStroke() {
      super(SWT.ARROW_DOWN, SWT.CONTROL);
    }

    @Override
    public void handleUiAction(Event e) {
      int level = 2;
      int value = -1;
      shiftDate(level, value);
    }
  }

  private class P_DateChooserOpenKeyStroke extends RwtKeyStroke {
    public P_DateChooserOpenKeyStroke() {
      super(SWT.F2);
    }

    @Override
    public void handleUiAction(Event e) {
      handleUiDateChooserAction();
    }
  }
}
