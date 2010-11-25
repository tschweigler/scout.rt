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
package org.eclipse.scout.rt.shared.services.common.context;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.scout.commons.beans.BasicPropertySupport;

/**
 * Property observer fires property "values" of data type Map<String,Object>
 */
public class SharedVariableMap implements Serializable, Map<String, Object> {
  private static final long serialVersionUID = 1L;

  private int m_version;
  private HashMap<String, Object> m_variables;
  private transient BasicPropertySupport m_propertySupport;

  public SharedVariableMap() {
    m_version = 0;
    m_variables = new HashMap<String, Object>();
    m_propertySupport = new BasicPropertySupport(this);
  }

  public SharedVariableMap(SharedVariableMap map) {
    m_version = map.m_version;
    m_variables = new HashMap<String, Object>(map.m_variables);
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    m_propertySupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    m_propertySupport.removePropertyChangeListener(listener);
  }

  /**
   * Update values of this variable map with the new one if version of new map
   * is newer <br>
   * Does not fire a change event
   */
  public void updateInternal(SharedVariableMap newMap) {
    if (newMap.getVersion() >= getVersion()) {
      m_variables = new HashMap<String, Object>(newMap.m_variables);
      m_version = newMap.getVersion();
    }
  }

  public int getVersion() {
    return m_version;
  }

  private void mapChanged() {
    m_version++;
    if (m_propertySupport != null) {
      m_propertySupport.firePropertyChange("values", null, new HashMap<String, Object>(m_variables));
    }
  }

  /*
   * Map implementation
   */
  /**
   * Fires a change event
   */
  public void clear() {
    m_variables.clear();
    mapChanged();
  }

  public boolean containsKey(Object key) {
    return m_variables.containsKey(key);
  }

  public boolean containsValue(Object value) {
    return m_variables.containsValue(value);
  }

  public Set<java.util.Map.Entry<String, Object>> entrySet() {
    return Collections.unmodifiableSet(m_variables.entrySet());
  }

  public Object get(Object key) {
    return m_variables.get(key);
  }

  public boolean isEmpty() {
    return m_variables.isEmpty();
  }

  public Set<String> keySet() {
    return Collections.unmodifiableSet(m_variables.keySet());
  }

  /**
   * Fires a change event
   */
  public Object put(String key, Object value) {
    Object o = m_variables.put(key, value);
    mapChanged();
    return o;
  }

  /**
   * Fires a change event
   */
  public void putAll(Map<? extends String, ? extends Object> m) {
    m_variables.putAll(m);
    mapChanged();
  }

  /**
   * Fires a change event
   */
  public Object remove(Object key) {
    Object o = m_variables.remove(key);
    mapChanged();
    return o;
  }

  public int size() {
    return m_variables.size();
  }

  public Collection<Object> values() {
    return Collections.unmodifiableCollection(m_variables.values());
  }

  @Override
  public String toString() {
    return m_variables.toString();
  }
}
