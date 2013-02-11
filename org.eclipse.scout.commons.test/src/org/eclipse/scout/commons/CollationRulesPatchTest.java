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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

/**
 * JUnit tests for {@link CollationRulesPatch}
 * 
 * @since 3.8.2
 */
public class CollationRulesPatchTest {

  @Test
  public void testPatchCollationRules() {
    String[] namesToSort = new String[]{"ABB MANNHEIM2", "abB NORWEGEN", "abb-mannheim1", "abb_PT", "abb PT", "abbott", "ABBOT", "ABB POWER", "ABB-PT"};

    String[] expectedNamesAfterPatch = new String[]{"abb-mannheim1", "ABB MANNHEIM2", "abB NORWEGEN", "ABB POWER", "abb PT", "ABB-PT", "abb_PT", "ABBOT", "abbott"};
    String[] expectedNamesBeforePatch = new String[]{"abb_PT", "abb-mannheim1", "ABB MANNHEIM2", "abB NORWEGEN", "ABBOT", "abbott", "ABB POWER", "abb PT", "ABB-PT"};

    CollationRulesPatch.revertPatchDefaultCollationRules();
    Collator collator = Collator.getInstance();
    String[] namesWrongSorting = (String[]) namesToSort.clone();
    sort(collator, namesWrongSorting);
    Assert.assertArrayEquals("Testing sort order before patch", expectedNamesBeforePatch, namesWrongSorting);

    CollationRulesPatch.patchDefaultCollationRules();
    collator = Collator.getInstance();
    String[] namesCorrectSorting = (String[]) namesToSort.clone();
    sort(collator, namesCorrectSorting);
    Assert.assertArrayEquals("Testing sort order after patch", expectedNamesAfterPatch, namesCorrectSorting);

    Collator collator2 = Collator.getInstance();
    String[] namesCorrectSorting2 = (String[]) namesToSort.clone();
    sort(collator2, namesCorrectSorting2);
    Assert.assertArrayEquals("Testing sort order after patch 2", expectedNamesAfterPatch, namesCorrectSorting2);
  }

  @Test
  public void testClearCollatorCache() throws Exception {
    Method methodClearCollatorCache = CollationRulesPatch.class.getDeclaredMethod("clearCollatorCache");
    Method methodGetAccessibleCollatorCache = CollationRulesPatch.class.getDeclaredMethod("getAccessibleCollatorCacheField");

    methodClearCollatorCache.setAccessible(true);
    methodGetAccessibleCollatorCache.setAccessible(true);

    Field cacheField = (Field) methodGetAccessibleCollatorCache.invoke(null);
    Object cache = (Object) cacheField.get(null);
    Class<?> cacheClass = cacheField.getType();
    Method cacheSizeMethod = cacheClass.getMethod("size");

    methodClearCollatorCache.invoke(null);
    int cacheSize = (Integer) cacheSizeMethod.invoke(cache);
    Assert.assertEquals(0, cacheSize);

    Collator.getInstance(Locale.GERMAN);
    cacheSize = (Integer) cacheSizeMethod.invoke(cache);
    Assert.assertEquals(1, cacheSize);

    Collator.getInstance(Locale.ENGLISH);
    cacheSize = (Integer) cacheSizeMethod.invoke(cache);
    Assert.assertEquals(2, cacheSize);

    methodClearCollatorCache.invoke(null);
    cacheSize = (Integer) cacheSizeMethod.invoke(cache);
    Assert.assertEquals(0, cacheSize);

    methodClearCollatorCache.setAccessible(false);
    methodGetAccessibleCollatorCache.setAccessible(false);
  }

  private void sort(final Collator collator, String[] array) {
    Arrays.sort(array, 0, array.length, new Comparator<String>() {
      @Override
      public int compare(String a, String b) {
        return collator.compare(a, b);
      }
    });
  }
}
