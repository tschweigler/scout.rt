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
package org.eclipse.scout.commons;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.junit.Assert;
import org.junit.Test;

/**
 * JUnit tests for {@link MailUtility}
 */
@SuppressWarnings("restriction")
public class MailUtilityTest {

  /**
   * Message without sender can be created
   * 
   * @throws ProcessingException
   *           ,MessagingException
   */
  @Test
  public void testMimeMessageWithoutSender() throws ProcessingException, MessagingException {
    MimeMessage message = MailUtility.createMimeMessage("Body", null, null);
    Assert.assertNotNull(message);
    message = MailUtility.createMimeMessage(null, null, "Subject", "Body", null);
    Assert.assertNotNull(message);
  }
}
