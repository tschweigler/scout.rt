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
package org.eclipse.scout.commons.nls;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * <h4>ResourceBundleCache</h4> Is used to cache resource bundle instances per {@link java.util.Locale}
 * 
 * @author imo
 */
public class NlsResourceBundleCache implements Serializable {
  private static final long serialVersionUID = -6602842161151788761L;
  private String m_resourceBundleName;
  private Class m_wrapperClass;
  private HashMap<Locale, ResourceBundle> m_resourceBundles;
  private Object m_resourceBundlesLock;

  public NlsResourceBundleCache(String resourceBundleName, Class wrapperClass) {
    m_resourceBundleName = resourceBundleName;
    m_wrapperClass = wrapperClass;
    m_resourceBundlesLock = new Object();
    m_resourceBundles = new HashMap<Locale, ResourceBundle>();
  }

  public ResourceBundle getResourceBundle(Locale locale) {
    if (locale == null) {
      throw new IllegalArgumentException("locale must not be null");
    }
    synchronized (m_resourceBundlesLock) {
      // double check with lock
      ResourceBundle r = m_resourceBundles.get(locale);
      if (r == null) {
        r = NlsResourceBundle.getBundle(m_resourceBundleName, locale, m_wrapperClass.getClassLoader(), m_wrapperClass);
        if (r != null) {
          m_resourceBundles.put(locale, r);
        }
      }
      return r;
    }
  }

  public Class getWrapperClass() {
    return m_wrapperClass;
  }

}
