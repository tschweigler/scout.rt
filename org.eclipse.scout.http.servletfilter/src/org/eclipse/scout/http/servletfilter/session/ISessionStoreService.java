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

import org.eclipse.scout.service.IService;

public interface ISessionStoreService extends IService {

  public void setAttribute(HttpServletRequest req, HttpServletResponse res, String key, Object value);

  public Object getAttribute(HttpServletRequest req, HttpServletResponse res, String key);

  String getSessionId(HttpServletRequest req, HttpServletResponse res);
}
