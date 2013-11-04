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
package org.eclipse.scout.http.servletfilter.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;

/**
 *
 */
public class DefaultSessionStoreService implements ISessionStoreService {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(DefaultSessionStoreService.class);

  @Override
  public void initializeService() {
  }

  @Override
  public void setAttribute(HttpServletRequest req, HttpServletResponse res, String key, Object value) {
    LOG.info("[DefaultSessionStoreService] setAttribute: " + key);
    req.getSession().setAttribute(key, value);
  }

  @Override
  public Object getAttribute(HttpServletRequest req, HttpServletResponse res, String key) {
    LOG.info("[DefaultSessionStoreService] getAttribute: " + key);
    return req.getAttribute(key);
  }

}
