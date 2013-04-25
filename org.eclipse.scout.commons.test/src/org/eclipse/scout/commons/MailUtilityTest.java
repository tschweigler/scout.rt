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

import java.io.ByteArrayOutputStream;

import javax.activation.MailcapCommandMap;
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

  /**
   * encoding error caused by jax-ws
   * <p>
   * <b>Note: This test must be run alone in an isolated jre, otherwise it is not correct.</b>
   */
  //@Test
  public void testJaxWsMimeMessageWithoutSender() throws Exception {
    String mime = "text/plain; charset=utf-8";
    //raw jre, everything fine
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    MailcapCommandMap.getDefaultCommandMap().createDataContentHandler("text/plain").writeTo("äöü", mime, bos);
    Assert.assertArrayEquals(new byte[]{-61, -92, -61, -74, -61, -68}, bos.toByteArray());
    //access jax-ws mime codec that adds the buggy mapping
    Class.forName("com.sun.xml.internal.ws.encoding.MimeCodec");
    bos = new ByteArrayOutputStream();
    MailcapCommandMap.getDefaultCommandMap().createDataContentHandler("text/plain").writeTo("äöü", mime, bos);
    Assert.assertArrayEquals(new byte[]{-28, -10, -4}, bos.toByteArray());
    //activate MailUtility that fixes the bug
    MailUtility.getContentTypeForExtension("html");
    bos = new ByteArrayOutputStream();
    MailcapCommandMap.getDefaultCommandMap().createDataContentHandler("text/plain").writeTo("äöü", mime, bos);
    Assert.assertArrayEquals(new byte[]{-61, -92, -61, -74, -61, -68}, bos.toByteArray());
  }
}
