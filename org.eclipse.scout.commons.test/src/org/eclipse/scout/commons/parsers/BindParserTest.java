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
package org.eclipse.scout.commons.parsers;

import org.eclipse.scout.commons.parsers.token.IToken;
import org.eclipse.scout.commons.parsers.token.ValueInputToken;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link BindParser}
 */
public class BindParserTest {

  /**
   * Column starts with "IN"
   */
  @Test
  public void testInListAttribute() {
    String sql = "SELECT INT_COLUMN_ID FROM TABLE1 WHERE INT_COLUMN_ID != :refId";
    BindModel bindModel = new BindParser(sql).parse();
    IToken[] tokens = bindModel.getIOTokens();
    ValueInputToken tok = (ValueInputToken) tokens[0];
    Assert.assertEquals("INT_COLUMN_ID", tok.getParsedAttribute());
    Assert.assertEquals("!=", tok.getParsedOp());
    Assert.assertEquals(":refId", tok.getParsedToken());
    Assert.assertEquals("refId", tok.getName());
  }
}
