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

package org.eclipse.scout.testing.client;

import java.util.List;

import javax.security.auth.Subject;

import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.services.common.session.IClientSessionRegistryService;
import org.eclipse.scout.rt.shared.ui.UserAgent;
import org.osgi.framework.ServiceRegistration;

/**
 * Deprecated: use {@link org.eclipse.scout.rt.testing.client.TestingClientSessionRegistryService} instead
 * will be removed with the L-Release.
 */
@Deprecated
public class TestingClientSessionRegistryService extends AbstractService implements IClientSessionRegistryService {

  private org.eclipse.scout.rt.testing.client.TestingClientSessionRegistryService m_newDelegate;

  public TestingClientSessionRegistryService(IClientSessionRegistryService delegate) {
    //This constructor should not be public. Use instead: org.eclipse.scout.rt.testing.client.TestingClientSessionRegistryService.registerTestingClientSessionRegistryService()
    m_newDelegate = new org.eclipse.scout.rt.testing.client.TestingClientSessionRegistryService(delegate);
  }

  private TestingClientSessionRegistryService(org.eclipse.scout.rt.testing.client.TestingClientSessionRegistryService newDelegate) {
    m_newDelegate = newDelegate;
  }

  public static TestingClientSessionRegistryService registerTestingClientSessionRegistryService() {
    org.eclipse.scout.rt.testing.client.TestingClientSessionRegistryService serviceDelegate = org.eclipse.scout.rt.testing.client.TestingClientSessionRegistryService.registerTestingClientSessionRegistryService();
    return new TestingClientSessionRegistryService(serviceDelegate);
  }

  public static void unregisterTestingClientSessionRegistryService(TestingClientSessionRegistryService service) {
    org.eclipse.scout.rt.testing.client.TestingClientSessionRegistryService.unregisterTestingClientSessionRegistryService(service.m_newDelegate);
  }

  public List<ServiceRegistration> getServiceRegistrations() {
    return m_newDelegate.getServiceRegistrations();
  }

  public void setServiceRegistrations(List<ServiceRegistration> serviceRegistrations) {
    m_newDelegate.setServiceRegistrations(serviceRegistrations);
  }

  public IClientSessionRegistryService getDelegateService() {
    return m_newDelegate.getDelegateService();
  }

  public <T extends IClientSession> T newClientSession(Class<T> clazz, UserAgent userAgent) {
    return m_newDelegate.newClientSession(clazz, userAgent);
  }

  public <T extends IClientSession> T newClientSession(Class<T> clazz, Subject subject, String virtualSessionId, UserAgent userAgent) {
    return m_newDelegate.newClientSession(clazz, subject, virtualSessionId, userAgent);
  }

}
