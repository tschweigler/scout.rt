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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.core.runtime.Platform;
import org.eclipse.scout.commons.LocaleThreadLocal;
import org.eclipse.scout.commons.TypeCastUtility;
import org.eclipse.scout.commons.annotations.ConfigOperation;
import org.eclipse.scout.commons.annotations.ConfigProperty;
import org.eclipse.scout.commons.annotations.ConfigPropertyValue;
import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.server.services.common.clientnotification.IClientNotificationService;
import org.eclipse.scout.rt.server.services.common.clientnotification.SessionFilter;
import org.eclipse.scout.rt.shared.OfflineState;
import org.eclipse.scout.rt.shared.ScoutTexts;
import org.eclipse.scout.rt.shared.TextsThreadLocal;
import org.eclipse.scout.rt.shared.services.common.context.SharedContextChangedNotification;
import org.eclipse.scout.rt.shared.services.common.context.SharedVariableMap;
import org.eclipse.scout.rt.shared.services.common.security.IAccessControlService;
import org.eclipse.scout.rt.shared.ui.UserAgent;
import org.eclipse.scout.service.SERVICES;
import org.osgi.framework.Bundle;

public abstract class AbstractServerSession implements IServerSession, Serializable {

  // Change for maven 04
  private static final long serialVersionUID = 1L;
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractServerSession.class);

  private transient Bundle m_bundle; // transient
  private boolean m_initialized;
  private boolean m_active;
  private Locale m_locale;
  private final HashMap<String, Object> m_attributes;
  private final SharedVariableMap m_sharedVariableMap;
  private boolean m_singleThreadSession;
  private transient ScoutTexts m_scoutTexts;
  private UserAgent m_userAgent;
  private String m_clientId;

  public AbstractServerSession(boolean autoInitConfig) {
    m_locale = LocaleThreadLocal.get();
    m_attributes = new HashMap<String, Object>();
    m_sharedVariableMap = new SharedVariableMap();
    if (autoInitConfig) {
      initConfig();
    }
  }

  private void readObject(ObjectInputStream ois)
      throws ClassNotFoundException, IOException {
    ois.defaultReadObject();
    if (m_bundle == null) {
      String symbolicName = this.getClass().getPackage().getName();
      m_bundle = Platform.getBundle(symbolicName);
    }

    if (m_scoutTexts == null) {
      m_scoutTexts = new ScoutTexts();
    }

  }

  /**
   * @deprecated use {@link #getConfiguredSingleThreadSession()} instead. Will be removed in Release 3.10.
   */
  @Deprecated
  protected boolean getConfiguredWebSession() {
    return getConfiguredSingleThreadSession();
  }

  @ConfigProperty(ConfigProperty.BOOLEAN)
  @Order(100)
  @ConfigPropertyValue("false")
  protected boolean getConfiguredSingleThreadSession() {
    return false;
  }

  @Override
  public Map<String, Object> getSharedVariableMap() {
    return Collections.unmodifiableMap(m_sharedVariableMap);
  }

  /**
   * do not use this internal method directly
   */
  protected <T> T getSharedContextVariable(String name, Class<T> type) {
    Object o = m_sharedVariableMap.get(name);
    return TypeCastUtility.castValue(o, type);
  }

  /**
   * do not use this internal method directly
   */
  protected <T> void setSharedContextVariable(String name, Class<T> type, T value) {
    T typedValue = TypeCastUtility.castValue(value, type);
    m_sharedVariableMap.put(name, typedValue);
  }

  private void assignUserId() {
    String userId = SERVICES.getService(IAccessControlService.class).getUserIdOfCurrentSubject();
    setUserIdInternal(userId);
  }

  /**
   * The session is running in its event loop
   */
  @Override
  public boolean isActive() {
    return m_active;
  }

  @Override
  public final String getUserId() {
    return getSharedContextVariable("userId", String.class);
  }

  private void setUserIdInternal(String newValue) {
    setSharedContextVariable("userId", String.class, newValue);
  }

  @Override
  public Locale getLocale() {
    return m_locale;
  }

  @Override
  public void setLocale(Locale l) {
    if (l != null) {
      m_locale = l;
    }
  }

  /**
   * <p>
   * Returns the {@link ScoutTexts} instance assigned to the type (class) of the current ServerSession.
   * </p>
   * <p>
   * Override this method to set the application specific texts implementation
   * </p>
   */
  @Override
  public ScoutTexts getTexts() {
    return m_scoutTexts;
  }

  @Override
  public Object getData(String key) {
    synchronized (AbstractServerSession.class) {
      return m_attributes.get(key);
    }
  }

  @Override
  public void setData(String key, Object value) {
    synchronized (AbstractServerSession.class) {
      m_attributes.put(key, value);
    }
  }

  protected void initConfig() {
    m_singleThreadSession = getConfiguredSingleThreadSession();
    if (!isWebSession()) {
      m_sharedVariableMap.addPropertyChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent e) {
          if (OfflineState.isOfflineDefault() == OfflineState.isOfflineInCurrentThread()) {
            // notify this session
            SERVICES.getService(IClientNotificationService.class).putNotification(new SharedContextChangedNotification(new SharedVariableMap(m_sharedVariableMap)), new SessionFilter(AbstractServerSession.this, 60000L));
          }
        }
      });
    }
    if (m_initialized) {
      return;
    }
    m_initialized = true;
  }

  @Override
  public Bundle getBundle() {
    return m_bundle;
  }

  @Override
  public final void loadSession(Bundle bundle) throws ProcessingException {
    if (isActive()) {
      throw new IllegalStateException("session is active");
    }
    if (bundle == null) {
      throw new IllegalArgumentException("bundle must not be null");
    }
    //m_bundle = bundle;
    m_active = true;
    m_scoutTexts = new ScoutTexts();
    // explicitly set the just created instance to the ThreadLocal because it was not available yet, when the job was started.
    TextsThreadLocal.set(m_scoutTexts);
    assignUserId();
    execLoadSession();
  }

  /**
   * 1. Identify (authenticate) user by its credentials
   * <ul>
   * <li>success: nop
   * <li>failure: throws wrapped ProcessingException(SecurityException)
   * </ul>
   * 2. Load session data such as properties, permissions
   */
  @ConfigOperation
  @Order(10)
  protected void execLoadSession() throws ProcessingException {
  }

  /**
   * @deprecated never called by the framework. Will be removed in Release 3.10.
   */
  @Deprecated
  protected void execLocaleChanged() throws ProcessingException {
  }

  /**
   * @deprecated use #isSingleThreadSession instead. Will be removed in Release 3.10.
   */
  @Deprecated
  public boolean isWebSession() {
    return isSingleThreadSession();
  }

  @Override
  public boolean isSingleThreadSession() {
    return m_singleThreadSession;
  }

  @Override
  public UserAgent getUserAgent() {
    return m_userAgent;
  }

  @Override
  public void setUserAgent(UserAgent userAgent) {
    m_userAgent = userAgent;
  }

  @Override
  public void setClientId(String clientId) {
    m_clientId = clientId;
  }

  @Override
  public void setClientId(HttpServletRequest req) {
    setClientId(getClientId(req));
  }

  public String getClientId() {
    return m_clientId;
  }

  //TODO TSW Sollte über einen Service realisiert werden
  private String getClientId(HttpServletRequest req) {
    Cookie[] cookies = req.getCookies();
    String cookieName = "clientid";

    for (int i = 0; i < cookies.length; i++) {
      Cookie cookie = cookies[i];

      if (cookie.getName().equals(cookieName)) {
        LOG.info("# ClientdId gefunden: " + cookie.getValue());
        return cookie.getValue();
      }
    }
    if (req.getAttribute(cookieName) != null) {
      LOG.info("# Innerhalb dieses Calls wurde bereits ein Cookie gesetzt");
      return (String) req.getAttribute(cookieName);
    }

    return null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    if (m_clientId != null) {
      return m_clientId.hashCode();
    }
    result = prime * result + ((m_attributes == null) ? 0 : m_attributes.hashCode());
    result = prime * result + ((m_clientId == null) ? 0 : m_clientId.hashCode());
    result = prime * result + (m_initialized ? 1231 : 1237);
    result = prime * result + ((m_locale == null) ? 0 : m_locale.hashCode());
    result = prime * result + ((m_sharedVariableMap == null) ? 0 : m_sharedVariableMap.hashCode());
    result = prime * result + (m_singleThreadSession ? 1231 : 1237);
    result = prime * result + ((m_userAgent == null) ? 0 : m_userAgent.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AbstractServerSession other = (AbstractServerSession) obj;

    if (m_clientId.equals(other.getClientId())) {
      return true;
    }

    if (m_active != other.m_active) {
      return false;
    }
    if (m_attributes == null) {
      if (other.m_attributes != null) {
        return false;
      }
    }
    else if (!m_attributes.equals(other.m_attributes)) {
      return false;
    }
    if (m_clientId == null) {
      if (other.m_clientId != null) {
        return false;
      }
    }
    else if (!m_clientId.equals(other.m_clientId)) {
      return false;
    }
    if (m_initialized != other.m_initialized) {
      return false;
    }
    if (m_locale == null) {
      if (other.m_locale != null) {

        return false;
      }
    }
    else if (!m_locale.equals(other.m_locale)) {
      return false;
    }
    if (m_sharedVariableMap == null) {
      if (other.m_sharedVariableMap != null) {
        return false;
      }
    }
    else if (!m_sharedVariableMap.equals(other.m_sharedVariableMap)) {
      return false;
    }
    if (m_singleThreadSession != other.m_singleThreadSession) {
      return false;
    }
    if (m_userAgent == null) {
      if (other.m_userAgent != null) {
        return false;
      }
    }
    else if (!m_userAgent.equals(other.m_userAgent)) {
      return false;
    }
    return true;
  }

}
