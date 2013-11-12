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
package org.eclipse.scout.http.servletfilter.session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;

/**
 * This Session Service is for PaaS vendors with HTTP-Session synchronization by servlet-container The Objects will
 * simply be stored in the HTTP-Session, the synchronization is will be done by the servlet container
 * 
 * @author tsw
 */
public class SerializedSessionService extends AbstractSessionStoreService {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(SerializedSessionService.class);

  @Override
  public void setAttribute(HttpServletRequest req, HttpServletResponse res, String key, Object value) {
    LOG.info("Speichern der " + key);
    if (value != null) {
      long start = System.currentTimeMillis();
      req.getSession().setAttribute(key, StringUtility.bytesToHex(serialize(value)));
      long end = System.currentTimeMillis();
      LOG.info("Serialisierungszeit: " + (end - start));
    }
  }

  @Override
  public Object getAttribute(HttpServletRequest req, HttpServletResponse res, String key) {
    LOG.info("Laden der " + key);
    long start = System.currentTimeMillis();
    String hex = (String) req.getSession().getAttribute(key);
    if (hex != null) {
      Object obj = deserialize(StringUtility.hexToBytes(hex));
      long end = System.currentTimeMillis();
      LOG.info("Deserialisierungszeit: " + (end - start));
      return obj;
    }
    else {
      return null;
    }
  }

}
