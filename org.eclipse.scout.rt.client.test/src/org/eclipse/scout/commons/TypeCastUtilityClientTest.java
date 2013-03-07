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
package org.eclipse.scout.commons;

import java.util.Map;

import org.eclipse.scout.commons.holders.IHolder;
import org.eclipse.scout.rt.client.ui.form.fields.AbstractValueField;
import org.eclipse.scout.rt.client.ui.form.fields.IValueField;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.AbstractListBox;
import org.eclipse.scout.rt.client.ui.form.fields.listbox.IListBox;
import org.junit.Assert;
import org.junit.Test;

/**
 * JUnit tests for {@link TypeCastUtility} using classes from the org.eclipse.scout.rt.client.ui.form.fields package.
 * See also: {@link org.eclipse.scout.commons.TypeCastUtilityTest} for tests that do not require this package.
 */
public class TypeCastUtilityClientTest {

  @Test
  public void testGetGenericsParameterClass() {
    Class<?> T;
    //
    T = TypeCastUtility.getGenericsParameterClass(ListBox.class, IHolder.class, 0);
    Assert.assertEquals(Long[].class, T);
    //
    T = TypeCastUtility.getGenericsParameterClass(ListBox.class, IValueField.class, 0);
    Assert.assertEquals(Long[].class, T);
    //
    T = TypeCastUtility.getGenericsParameterClass(ListBox.class, IListBox.class, 0);
    Assert.assertEquals(Long.class, T);
    //
    T = TypeCastUtility.getGenericsParameterClass(LongField.class, IHolder.class, 0);
    Assert.assertEquals(Long.class, T);
    //
    T = TypeCastUtility.getGenericsParameterClass(LongArrayField.class, IHolder.class, 0);
    Assert.assertEquals(Long[].class, T);
    //
    T = TypeCastUtility.getGenericsParameterClass(MapField.class, IHolder.class, 0);
    Assert.assertEquals(Map.class, T);
    //
    T = TypeCastUtility.getGenericsParameterClass(MapArrayField.class, IHolder.class, 0);
    Assert.assertEquals(Map[].class, T);
  }

  static class ListBox extends AbstractListBox<Long> {

  }

  static class LongField extends AbstractValueField<Long> {

  }

  static class LongArrayField extends AbstractValueField<Long[]> {

  }

  static class MapField extends AbstractValueField<Map<String, Integer>> {

  }

  static class MapArrayField extends AbstractValueField<Map<String[], Integer>[]> {

  }
}
