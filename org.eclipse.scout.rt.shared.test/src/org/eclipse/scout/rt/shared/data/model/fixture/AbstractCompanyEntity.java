/*******************************************************************************
 * Copyright (c) 2013 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.shared.data.model.fixture;

import java.util.List;

import org.eclipse.scout.commons.annotations.Order;
import org.eclipse.scout.rt.shared.data.model.AbstractDataModelAttribute;
import org.eclipse.scout.rt.shared.data.model.AbstractDataModelEntity;
import org.eclipse.scout.rt.shared.data.model.IDataModelAttribute;
import org.eclipse.scout.rt.shared.data.model.IDataModelEntity;

public abstract class AbstractCompanyEntity extends AbstractDataModelEntity {
  private static final long serialVersionUID = 1L;

  @Override
  protected String getConfiguredText() {
    return "Company";
  }

  @Override
  protected void injectAttributesInternal(List<IDataModelAttribute> attributeList) {
    super.injectAttributesInternal(attributeList);
    CustomDataModelExtension.injectAttributes(this, attributeList);
  }

  @Override
  protected void injectEntitiesInternal(List<IDataModelEntity> entityList) {
    super.injectEntitiesInternal(entityList);
    CustomDataModelExtension.injectEntities(this, entityList);
  }

  @Order(10.0f)
  public class NameAttribute extends AbstractDataModelAttribute {
    private static final long serialVersionUID = 1L;

    @Override
    protected String getConfiguredText() {
      return "Name";
    }

    @Override
    protected int getConfiguredType() {
      return IDataModelAttribute.TYPE_STRING;
    }
  }

  @Order(10.0f)
  public class PrimaryAddressEntity extends AbstractAddressEntity {
    private static final long serialVersionUID = 1L;

    @Override
    protected String getConfiguredText() {
      return "PrimaryAddress";
    }

    @Override
    protected boolean getConfiguredOneToMany() {
      return false;
    }
  }

  @Order(10.0f)
  public class LegalAddressEntity extends AbstractAddressEntity {
    private static final long serialVersionUID = 1L;

    @Override
    protected String getConfiguredText() {
      return "LegalAddress";
    }

    @Override
    protected boolean getConfiguredOneToMany() {
      return false;
    }
  }

  @Order(10.0f)
  public class AccountManagerEntity extends AbstractPersonEntity {
    private static final long serialVersionUID = 1L;

    @Override
    protected String getConfiguredText() {
      return "AccountManager";
    }

    @Override
    protected boolean getConfiguredOneToMany() {
      return false;
    }
  }

  @Order(20.0f)
  public class EmployeeEntity extends AbstractPersonEntity {
    private static final long serialVersionUID = 1L;

    @Override
    protected String getConfiguredText() {
      return "Employee";
    }
  }
}
