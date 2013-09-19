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
package org.eclipse.scout.rt.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.servlet.http.HttpServletRequest;

/**
 * Kümmert sich um die Speicherung der sessionabhängigen Daten, sodass auch bei mehreren Server-Instanzen alle Daten
 * konsistet bleiben
 */
public class SessionHandler {

  // Könnte auch mit ISessionHandler und den Implementierungen HTTPSessionHandler DBSesionHandler usw gemacht werden (über SessionHandlerFactory?)
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

  public void storeInSession(HttpServletRequest req, String key, Object value) {

  }

  public Object loadFromSession(HttpServletRequest req, String key) {

    Object value = null;

    switch (m_sessionStoreType) {
      case HTTPSESSION:
        value = loadFromHttpSession(req, key);
        break;
      case DATABASE:
        value = null;
    }

    return value;
  }

  private void storeInHttpSession(HttpServletRequest req, String key, Object value) {
    req.setAttribute(key, serialize(value));
  }

  private Object loadFromHttpSession(HttpServletRequest req, String key) {
    return deserialize((byte[]) req.getAttribute(key));
  }

  private byte[] serialize(Object obj) {

    byte[] bytes = null;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.close();
      bytes = baos.toByteArray();
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
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        obj = ois.readObject();
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
