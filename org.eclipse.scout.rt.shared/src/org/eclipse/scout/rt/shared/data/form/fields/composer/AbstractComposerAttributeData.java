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
package org.eclipse.scout.rt.shared.data.form.fields.composer;

import java.io.Serializable;

/**
 * Data representation for a composer attribute definiton
 */
public abstract class AbstractComposerAttributeData implements Serializable, ComposerConstants {
  private static final long serialVersionUID = 1L;

  private AbstractComposerEntityData m_parentEntity;

  public AbstractComposerAttributeData() {
  }

  public AbstractComposerEntityData getParentEntity() {
    return m_parentEntity;
  }

  public void setParentEntity(AbstractComposerEntityData parentEntity) {
    m_parentEntity = parentEntity;
  }
}
