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

import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.commons.serialization.IObjectSerializer;
import org.eclipse.scout.commons.serialization.SerializationUtility;
import org.eclipse.scout.service.AbstractService;

/**
 *
 */
public abstract class AbstractSessionStoreService extends AbstractService implements ISessionStoreService {

  private String cookieName = "clientid";
  IObjectSerializer objs;

  protected AbstractSessionStoreService() {
    objs = SerializationUtility.createObjectSerializer();
  }

  /**
   * returns the session id of the HTTP-Request. If no session id is set a new id will be generated and set.
   * 
   * @param req
   *          HttpServletRequest
   * @param res
   *          HttpServletResponse
   * @return the session id
   */
  @Override
  public String getSessionId(HttpServletRequest req, HttpServletResponse res) {
    Cookie[] cookies = req.getCookies();

    for (int i = 0; i < cookies.length; i++) {
      Cookie cookie = cookies[i];

      if (cookie.getName().equals(cookieName)) {
        return cookie.getValue();
      }
    }
    if (req.getAttribute(cookieName) != null) {
      return (String) req.getAttribute(cookieName);
    }

    return getNewSessionId(req, res);
  }

  /**
   * generates a new session id, set it as cookie and returns it
   * 
   * @param req
   *          HttpServletRequest
   * @param res
   *          HttpServletResponse
   * @return the new session id
   */
  private String getNewSessionId(HttpServletRequest req, HttpServletResponse res) {
    do {
      String newClientId = UUID.randomUUID().toString();
      Cookie cookie = new Cookie(cookieName, newClientId);
      req.setAttribute(cookieName, newClientId);
      res.addCookie(cookie);
      return newClientId;
    }
    while (true);
  }

  /**
   * Serializes objects
   * 
   * @param obj
   *          Object to serialize
   * @return bytestream
   */
  protected byte[] serialize(Object obj) {

    byte[] bytes = null;

    try {
      bytes = objs.serialize(obj);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return bytes;
  }

  /**
   * deserializes bytestreams, can handle nearly all scout classes
   * 
   * @param bytes
   *          bytestream
   * @return deserialized Object
   */
  protected Object deserialize(byte[] bytes) {

    Object obj = null;

    try {
      if (bytes != null) {
        obj = objs.deserialize(bytes, Object.class);
      }
    }
    catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return obj;
  }

}
