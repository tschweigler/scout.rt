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
package org.eclipse.scout.rt.shared.data.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.scout.commons.ConfigurationUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;

public abstract class AbstractDataModel implements IDataModel, Serializable {
  private static final long serialVersionUID = 1L;
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(AbstractDataModel.class);

  private boolean m_calledInitializer;
  private IDataModelAttribute[] m_attributes;
  private IDataModelEntity[] m_entities;

  public AbstractDataModel() {
    this(true);
  }

  public AbstractDataModel(boolean callInitializer) {
    super();
    if (callInitializer) {
      callInitializer();
    }
  }

  protected void callInitializer() {
    if (!m_calledInitializer) {
      m_calledInitializer = true;
      initConfig();
    }
  }

  protected IDataModelAttribute[] createAttributes() {
    ArrayList<IDataModelAttribute> attributes = new ArrayList<IDataModelAttribute>();
    Class[] all = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    Class[] filtered = ConfigurationUtility.filterClasses(all, IDataModelAttribute.class);
    for (Class<? extends IDataModelAttribute> c : ConfigurationUtility.sortFilteredClassesByOrderAnnotation(filtered, IDataModelAttribute.class)) {
      try {
        IDataModelAttribute a = ConfigurationUtility.newInnerInstance(this, c);
        attributes.add(a);
      }
      catch (Exception e) {
        LOG.warn(null, e);
      }
    }
    return attributes.toArray(new IDataModelAttribute[attributes.size()]);
  }

  protected IDataModelEntity[] createEntities() {
    ArrayList<IDataModelEntity> entities = new ArrayList<IDataModelEntity>();
    Class[] all = ConfigurationUtility.getDeclaredPublicClasses(getClass());
    Class[] filtered = ConfigurationUtility.filterClasses(all, IDataModelEntity.class);
    for (Class<? extends IDataModelEntity> c : ConfigurationUtility.sortFilteredClassesByOrderAnnotation(filtered, IDataModelEntity.class)) {
      try {
        IDataModelEntity e = ConfigurationUtility.newInnerInstance(this, c);
        entities.add(e);
      }
      catch (Exception e) {
        LOG.warn(null, e);
      }
    }
    return entities.toArray(new IDataModelEntity[entities.size()]);
  }

  @SuppressWarnings("deprecation")
  protected void initConfig() {
    // attributes
    m_attributes = createAttributes();
    for (IDataModelAttribute a : m_attributes) {
      a.setParentEntity(null);
    }
    // entities
    m_entities = createEntities();
    HashMap<Class<? extends IDataModelEntity>, IDataModelEntity> instanceMap = new HashMap<Class<? extends IDataModelEntity>, IDataModelEntity>();
    for (IDataModelEntity e : m_entities) {
      e.setParentEntity(null);
      instanceMap.put(e.getClass(), e);
    }
    for (IDataModelEntity e : m_entities) {
      e.initializeChildEntities(instanceMap);
    }
  }

  @Override
  public void init() {
    //init tree structure
    for (IDataModelEntity e : getEntities()) {
      try {
        e.initEntity();
      }
      catch (Throwable t) {
        LOG.error("entity " + e, t);
      }
    }
    for (IDataModelAttribute a : getAttributes()) {
      try {
        a.initAttribute();
      }
      catch (Throwable t) {
        LOG.error("attribute " + a, t);
      }
    }
  }

  @Override
  public IDataModelAttribute[] getAttributes() {
    return m_attributes;
  }

  @Override
  public IDataModelEntity[] getEntities() {
    return m_entities;
  }

  @Override
  public IDataModelAttribute getAttribute(Class<? extends IDataModelAttribute> attributeClazz) {
    for (IDataModelAttribute attribute : m_attributes) {
      if (attribute.getClass() == attributeClazz) {
        return attribute;
      }
    }
    return null;
  }

  @Override
  public IDataModelEntity getEntity(Class<? extends IDataModelEntity> entityClazz) {
    for (IDataModelEntity entity : m_entities) {
      if (entity.getClass() == entityClazz) {
        return entity;
      }
    }
    return null;
  }
}
