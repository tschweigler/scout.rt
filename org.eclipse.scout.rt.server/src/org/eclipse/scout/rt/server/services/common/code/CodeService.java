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
package org.eclipse.scout.rt.server.services.common.code;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.Platform;
import org.eclipse.scout.commons.LocaleThreadLocal;
import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.osgi.BundleClassDescriptor;
import org.eclipse.scout.commons.runtime.BundleBrowser;
import org.eclipse.scout.rt.server.internal.Activator;
import org.eclipse.scout.rt.shared.services.common.code.ICode;
import org.eclipse.scout.rt.shared.services.common.code.ICodeService;
import org.eclipse.scout.rt.shared.services.common.code.ICodeType;
import org.eclipse.scout.rt.shared.servicetunnel.RemoteServiceAccessDenied;
import org.eclipse.scout.service.AbstractService;
import org.osgi.framework.Bundle;

/**
 * delegates to {@link CodeTypeStore}
 */
@Priority(-1)
public class CodeService extends AbstractService implements ICodeService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(CodeService.class);

  private CodeTypeStore m_codeTypeStore;
  private Object m_codeTypeClassDescriptorMapLock;
  private HashMap<String, BundleClassDescriptor[]> m_codeTypeClassDescriptorMap;

  public CodeService() {
    m_codeTypeStore = new CodeTypeStore();
    m_codeTypeClassDescriptorMapLock = new Object();
    m_codeTypeClassDescriptorMap = new HashMap<String, BundleClassDescriptor[]>();
  }

  @Override
  public <T extends ICodeType> T getCodeType(Class<T> type) {
    return getCodeTypeCache().getCodeType(type);
  }

  @Override
  public <T extends ICodeType> T getCodeType(Long partitionId, Class<T> type) {
    return getCodeTypeCache(partitionId).getCodeType(type);
  }

  @Override
  public ICodeType findCodeTypeById(Object id) {
    if (id == null) {
      return null;
    }
    ICodeType ct = getCodeTypeCache().findCodeTypeById(id);
    if (ct != null) {
      return ct;
    }
    // populate code type cache
    getAllCodeTypes("");
    return getCodeTypeCache().findCodeTypeById(id);
  }

  @Override
  public ICodeType findCodeTypeById(Long partitionId, Object id) {
    if (id == null) {
      return null;
    }
    ICodeType ct = getCodeTypeCache(partitionId).findCodeTypeById(id);
    if (ct != null) {
      return ct;
    }
    // populate code type cache
    getAllCodeTypes("");
    return getCodeTypeCache(partitionId).findCodeTypeById(id);
  }

  @Override
  public ICodeType[] getCodeTypes(Class... types) {
    return getCodeTypeCache().getCodeTypes(types);
  }

  @Override
  public ICodeType[] getCodeTypes(Long partitionId, Class... types) {
    return getCodeTypeCache(partitionId).getCodeTypes(types);
  }

  @Override
  public <T extends ICode> T getCode(final Class<T> type) {
    return getCodeTypeCache().getCode(type);
  }

  @Override
  public <T extends ICode> T getCode(final Long partitionId, final Class<T> type) {
    return getCodeTypeCache(partitionId).getCode(type);
  }

  @Override
  public <T extends ICodeType> T reloadCodeType(Class<T> type) {
    if (type == null) {
      return null;
    }
    m_codeTypeStore.unloadCodeTypeCache(type);
    return getCodeTypeCache().reloadCodeType(type);
  }

  @Override
  public ICodeType[] reloadCodeTypes(Class... types) {
    if (types == null) {
      return null;
    }
    m_codeTypeStore.unloadCodeTypeCache(types);
    return getCodeTypeCache().reloadCodeTypes(types);
  }

  @Override
  public BundleClassDescriptor[] getAllCodeTypeClasses(String classPrefix) {
    if (classPrefix == null) {
      return new BundleClassDescriptor[0];
    }
    synchronized (m_codeTypeClassDescriptorMapLock) {
      BundleClassDescriptor[] a = m_codeTypeClassDescriptorMap.get(classPrefix);
      if (a != null) {
        return a;
      }
      //
      HashSet<BundleClassDescriptor> discoveredCodeTypes = new HashSet<BundleClassDescriptor>();
      for (Bundle bundle : Activator.getDefault().getBundle().getBundleContext().getBundles()) {
        if (bundle.getSymbolicName().startsWith(classPrefix)) {
          // ok
        }
        else if (classPrefix.startsWith(bundle.getSymbolicName() + ".")) {
          // ok
        }
        else {
          continue;
        }
        // Skip uninteresting bundles
        if (!acceptBundle(bundle, classPrefix)) {
          continue;
        }
        String[] classNames;
        try {
          BundleBrowser bundleBrowser = new BundleBrowser(bundle.getSymbolicName(), bundle.getSymbolicName());
          classNames = bundleBrowser.getClasses(false, true);
        }
        catch (Exception e1) {
          LOG.warn(null, e1);
          continue;
        }
        // filter
        System.err.println("{getAllCodeTypeClasses:classNames.length}>>>" + classNames.length);
        for (String className : classNames) {
          System.err.println("{getAllCodeTypeClasses:classNames}>>>" + className);
          // fast pre-check
          if (acceptClassName(bundle, className)) {
            try {
              Class c = null;
              c = bundle.loadClass(className);
              if (acceptClass(bundle, c)) {
                discoveredCodeTypes.add(new BundleClassDescriptor(bundle.getSymbolicName(), c.getName()));
              }
            }
            catch (Throwable t) {
              // nop
            }
          }
        }
      }
      a = discoveredCodeTypes.toArray(new BundleClassDescriptor[discoveredCodeTypes.size()]);
      m_codeTypeClassDescriptorMap.put(classPrefix, a);
      return a;
    }
  }

  @Override
  @RemoteServiceAccessDenied
  public ICodeType[] getAllCodeTypes(String classPrefix) {
    ArrayList<Class> list = new ArrayList<Class>();
    for (BundleClassDescriptor d : getAllCodeTypeClasses(classPrefix)) {
      try {
        list.add(Platform.getBundle(d.getBundleSymbolicName()).loadClass(d.getClassName()));
      }
      catch (Throwable t) {
        LOG.warn("Loading " + d.getClassName() + " of bundle " + d.getBundleSymbolicName(), t);
        continue;
      }
    }
    return getCodeTypes(list.toArray(new Class[list.size()]));
  }

  @Override
  @RemoteServiceAccessDenied
  public ICodeType[] getAllCodeTypes(String classPrefix, Long partitionId) {
    return getAllCodeTypes(classPrefix);
  }

  private CodeTypeCache getCodeTypeCache() {
    return m_codeTypeStore.getCodeTypeCache(LocaleThreadLocal.get());
  }

  private CodeTypeCache getCodeTypeCache(Long partitionId) {
    return m_codeTypeStore.getCodeTypeCache(partitionId, LocaleThreadLocal.get());
  }

  /**
   * Checks whether the given bundle should be scanned for code type classes. The default implementations accepts
   * all bundles that are not fragments (because classes from fragments are automatically read when browsing the host
   * bundle).
   * 
   * @return Returns <code>true</code> if the given bundle meets the requirements to be scanned for code type classes.
   *         <code>false</code> otherwise.
   */
  protected boolean acceptBundle(Bundle bundle, String classPrefix) {
    return !Platform.isFragment(bundle);
  }

  /**
   * Checks whether the given class name is a potential code type class. Class names that do not meet the
   * requirements of this method are not considered further, i.e. the "expensive" class instantiation is skipped.
   * The default implementation checks whether the class name contains <code>"CodeType"</code>.
   * 
   * @param bundle
   *          The class's hosting bundle
   * @param className
   *          the class name to be checked
   * @return Returns <code>true</code> if the given class name meets the requirements to be considered as a code type
   *         class. <code>false</code> otherwise.
   */
  protected boolean acceptClassName(Bundle bundle, String className) {
    return (className.indexOf("CodeType") >= 0);
  }

  /**
   * Checks whether the given class is a CodeType class that should be visible to this service. The default
   * implementation checks if the class meets the following conditions:
   * <ul>
   * <li>subclass of {@link ICodeType}
   * <li><code>public</code>
   * <li>not an <code>interface</code>
   * <li>not <code>abstract</code>
   * <li>the class's simple name does not start with <code>"Abstract"</code> (convenience check)
   * </ul>
   * 
   * @param bundle
   *          The class's hosting bundle
   * @param c
   *          the class to be checked
   * @return Returns <code>true</code> if the class is a code type class. <code>false</code> otherwise.
   */
  protected boolean acceptClass(Bundle bundle, Class<?> c) {
    if (ICodeType.class.isAssignableFrom(c)) {
      if (!c.isInterface()) {
        int flags = c.getModifiers();
        if (Modifier.isPublic(flags) && (!Modifier.isAbstract(flags)) && (!c.getSimpleName().startsWith("Abstract"))) {
          return true;
        }
      }
    }
    return false;
  }
}
