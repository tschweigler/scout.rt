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
package org.eclipse.scout.rt.ui.swing;

import java.awt.Image;
import java.util.Hashtable;

import javax.swing.Icon;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.scout.rt.ui.swing.icons.SwingBundleIconLocator;
import org.eclipse.scout.rt.ui.swing.login.internal.InternalNetAuthenticator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public class Activator extends Plugin implements SwingIcons {
  public static final String PLUGIN_ID = "org.eclipse.scout.rt.ui.swing";

  private static Activator plugin;
  private SwingIconLocator m_iconLocator;
  private ServiceRegistration m_netAuthRegistration;

  public Activator() {
  }

  public static Activator getDefault() {
    return plugin;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    SwingBundleIconLocator iconLocator = new SwingBundleIconLocator();
    m_iconLocator = new SwingIconLocator(iconLocator);
    // register net authenticator ui
    Hashtable<String, Object> map = new Hashtable<String, Object>();
    map.put(Constants.SERVICE_RANKING, -2);
    m_netAuthRegistration = Activator.getDefault().getBundle().getBundleContext().registerService(java.net.Authenticator.class.getName(), new InternalNetAuthenticator(), map);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    m_iconLocator = null;
    if (m_netAuthRegistration != null) {
      m_netAuthRegistration.unregister();
      m_netAuthRegistration = null;
    }
    plugin = null;
    super.stop(context);
  }

  public static Image getImage(String name) {
    return getDefault().getImageImpl(name);
  }

  public static Icon getIcon(String name) {
    Activator activator = getDefault();
    if (activator != null) {
      return activator.getIconImpl(name);
    }
    return null;
  }

  private Image getImageImpl(String name) {
    return m_iconLocator.getImage(name);
  }

  private Icon getIconImpl(String name) {
    return m_iconLocator.getIcon(name);
  }
}
