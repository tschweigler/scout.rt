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
package org.eclipse.scout.rt.server.services.common.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.service.SERVICES;

public final class SessionStore {

  public static Class<? extends ISessionStoreService> usedServiceType = ISessionStoreService.class;

  private SessionStore() {
  }

  public static void setAttribute(HttpServletRequest req, HttpServletResponse res, String key, Object value) {
    ISessionStoreService service = SERVICES.getService(usedServiceType);
    if (service != null) {
      service.setAttribute(req, res, key, value);
    }
  }

  public static Object getAttribute(HttpServletRequest req, HttpServletResponse res, String key) {

    ISessionStoreService service = SERVICES.getService(usedServiceType);
    if (service != null) {
      return service.getAttribute(req, res, key);
    }
    else {
      return null;
    }
  }
}
