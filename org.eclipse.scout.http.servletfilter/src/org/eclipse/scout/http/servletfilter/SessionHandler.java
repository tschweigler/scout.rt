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
package org.eclipse.scout.http.servletfilter;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.serialization.IObjectSerializer;
import org.eclipse.scout.commons.serialization.SerializationUtility;

/**
 * Kümmert sich um die Speicherung der sessionabhängigen Daten, sodass auch bei mehreren Server-Instanzen alle Daten
 * konsistet bleiben
 */
public class SessionHandler {

  private static SessionHandler m_sessionHandler;
  private SessionStoreType m_sessionStoreType;

  public enum SessionStoreType {
    HTTPSESSION, DATABASE
  }

  private SessionHandler() {
    m_sessionStoreType = SessionStoreType.HTTPSESSION;
  }

  public static SessionHandler getInstance() {
    return (m_sessionHandler != null) ? m_sessionHandler : new SessionHandler();
  }

  public void setAttribute(HttpServletRequest req, String key, Object value) {
    switch (m_sessionStoreType) {
      case HTTPSESSION:
        setHttpAttribute(req, key, value);
        break;
      case DATABASE:

    }
  }

  public Object getAttribute(HttpServletRequest req, String key) {

    Object value = null;

    switch (m_sessionStoreType) {
      case HTTPSESSION:
        value = getHttpAttribute(req, key);
        break;
      case DATABASE:
        value = null;
    }

    return value;
  }

  private void setHttpAttribute(HttpServletRequest req, String key, Object value) {
    if (value != null) {
      req.getSession().setAttribute(key, StringUtility.bytesToHex(serialize(value)));
    }
  }

  private Object getHttpAttribute(HttpServletRequest req, String key) {

    String hex = (String) req.getSession().getAttribute(key);
    if (hex != null) {
      return deserialize(StringUtility.hexToBytes(hex));
    }
    else {
      return null;
    }
    //return deserialize(StringUtility.hexToBytes((String) req.getSession().getAttribute(key)));
  }

  private byte[] serialize(Object obj) {

    byte[] bytes = null;

    try {
      //long start = new Date().getTime();
      IObjectSerializer objs = SerializationUtility.createObjectSerializer();
      bytes = objs.serialize(obj);
      //System.out.println("Serializationtime [" + obj.getClass().getName() + "]: " + (new Date().getTime() - start));
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return bytes;
  }

  private Object deserialize(byte[] bytes) {

    Object obj = null;

    try {
      if (bytes != null) {
        IObjectSerializer objs = SerializationUtility.createObjectSerializer();
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
