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
package org.eclipse.scout.rt.shared.validate.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.eclipse.scout.rt.shared.services.common.code.ICodeType;

/**
 * value or array (each element) must be part of a code type enum
 * <p>
 * In order to treat 0 as null, use the {@link Treat0AsNull} annotation.
 * <p>
 * In order to annotate a field where you have no access (super class) you may consider using {@link FieldReference} on
 * a placeholder field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@ValidateAnnotationMarker
public @interface CodeValue {

  Class<? extends ICodeType> value();

  /**
   * subtree=false is the default, the check is applied to the parameter or field only.
   * <p>
   * subtree=true causes the check to be applied to all objects in the subtree of this objects (deep structure) as well.
   */
  boolean subtree() default false;
}
