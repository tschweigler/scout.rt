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
package org.eclipse.scout.rt.client.mobile.ui.desktop;

import java.util.Collection;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.holders.IHolder;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.mobile.navigation.AbstractMobileBackAction;
import org.eclipse.scout.rt.client.mobile.navigation.AbstractMobileHomeAction;
import org.eclipse.scout.rt.client.mobile.navigation.IBreadCrumbsNavigationService;
import org.eclipse.scout.rt.client.mobile.transformation.IDeviceTransformationService;
import org.eclipse.scout.rt.client.mobile.transformation.IDeviceTransformer;
import org.eclipse.scout.rt.client.ui.action.IAction;
import org.eclipse.scout.rt.client.ui.basic.table.ITable;
import org.eclipse.scout.rt.client.ui.desktop.AbstractDesktopExtension;
import org.eclipse.scout.rt.client.ui.desktop.ContributionCommand;
import org.eclipse.scout.rt.client.ui.desktop.IDesktop;
import org.eclipse.scout.rt.client.ui.desktop.outline.IOutline;
import org.eclipse.scout.rt.client.ui.desktop.outline.pages.IPageWithTable;
import org.eclipse.scout.rt.client.ui.form.IForm;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.ui.UserAgentUtility;
import org.eclipse.scout.service.SERVICES;

public class DeviceTransformationDesktopExtension extends AbstractDesktopExtension {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(DeviceTransformationDesktopExtension.class);

  private boolean m_active;
  private IDeviceTransformer m_deviceTransformer;

  public DeviceTransformationDesktopExtension() {
    setActive(UserAgentUtility.isTouchDevice());
  }

  public boolean isActive() {
    return m_active;
  }

  public void setActive(boolean active) {
    m_active = active;
  }

  @Override
  public void setCoreDesktop(IDesktop desktop) {
    super.setCoreDesktop(desktop);

    if (isActive()) {
      SERVICES.getService(IBreadCrumbsNavigationService.class).install(getCoreDesktop());
      SERVICES.getService(IDeviceTransformationService.class).install(getCoreDesktop());
    }
  }

  public IDeviceTransformer getDeviceTransformer() {
    if (m_deviceTransformer == null) {
      m_deviceTransformer = SERVICES.getService(IDeviceTransformationService.class).getDeviceTransformer();
    }

    return m_deviceTransformer;
  }

  @Override
  protected ContributionCommand execInit() throws ProcessingException {
    if (!isActive()) {
      return super.execInit();
    }

    return ContributionCommand.Continue;
  }

  @Override
  public void contributeActions(Collection<IAction> actions) {
    if (!isActive()) {
      return;
    }

    getDeviceTransformer().adaptDesktopActions(actions);

    super.contributeActions(actions);
  }

  @Override
  public void contributeOutlines(Collection<IOutline> outlines) {
    if (!isActive()) {
      return;
    }

    getDeviceTransformer().adaptDesktopOutlines(outlines);

    super.contributeOutlines(outlines);
  }

  @Override
  protected ContributionCommand execOutlineChanged(IOutline oldOutline, IOutline newOutline) throws ProcessingException {
    if (!isActive()) {
      return super.execOutlineChanged(oldOutline, newOutline);
    }

    getDeviceTransformer().transformOutline(newOutline);

    return ContributionCommand.Continue;
  }

  @Override
  protected ContributionCommand execPageDetailTableChanged(ITable oldTable, ITable newTable) throws ProcessingException {
    if (!isActive()) {
      return super.execPageDetailTableChanged(oldTable, newTable);
    }

    getDeviceTransformer().transformPageDetailTable(newTable);

    return ContributionCommand.Continue;
  }

  @Override
  protected ContributionCommand execCustomFormModification(IHolder<IForm> formHolder) {
    if (!isActive()) {
      return super.execCustomFormModification(formHolder);
    }

    IForm form = formHolder.getValue();
    if (form == null) {
      return ContributionCommand.Stop;
    }

    try {
      getDeviceTransformer().transformForm(form);
    }
    catch (ProcessingException e) {
      SERVICES.getService(IExceptionHandlerService.class).handleException(e);
    }

    if (!getDeviceTransformer().acceptFormAddingToDesktop(form)) {
      formHolder.setValue(null);
      return ContributionCommand.Stop;
    }

    return ContributionCommand.Continue;
  }

  @Override
  protected ContributionCommand execTablePageLoaded(IPageWithTable<?> tablePage) throws ProcessingException {
    if (!isActive()) {
      return super.execTablePageLoaded(tablePage);
    }

    getDeviceTransformer().notifyTablePageLoaded(tablePage);

    return ContributionCommand.Continue;
  }

  @Order(10)
  public class BackAction extends AbstractMobileBackAction {

    @Override
    protected boolean getConfiguredVisible() {
      return false;
    }

  }

  @Order(20)
  public class HomeAction extends AbstractMobileHomeAction {

    @Override
    protected boolean getConfiguredVisible() {
      return false;
    }

  }

}
