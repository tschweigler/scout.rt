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
package org.eclipse.scout.rt.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.eclipse.scout.commons.serialization.IObjectSerializer;
import org.eclipse.scout.commons.serialization.SerializationUtility;
import org.eclipse.scout.rt.server.testenvironment.TestEnvironmentServerSession;
import org.junit.Test;

/**
 *
 */
public class AbstractServerSessionSerializationTest {

  @Test
  public void serialize() {
    IServerSession session = new TestEnvironmentServerSession();

    IObjectSerializer objs = SerializationUtility.createObjectSerializer();

    try {
      objs.serialize(session);
    }
    catch (IOException e) {
      fail("Unable to serialize AbstractServerSession");
    }

  }

  @Test
  public void deserialize() {
    IServerSession session = new TestEnvironmentServerSession();

    IObjectSerializer objs = SerializationUtility.createObjectSerializer();
    byte[] serializedSession = null;
    try {
      serializedSession = objs.serialize(session);
    }
    catch (IOException e) {
      fail("Unable to serialize AbstractServerSession");
    }

    if (serializedSession != null) {
      try {
        IServerSession deserializedSession = objs.deserialize(serializedSession, IServerSession.class);

        assertEquals(session.getSessionId(), deserializedSession.getSessionId());
        assertEquals(session.getLocale(), deserializedSession.getLocale());
        assertEquals(session.getUserAgent(), deserializedSession.getUserAgent());
        assertEquals(session.getUserId(), deserializedSession.getUserId());
        assertNotNull(deserializedSession.getBundle());
        assertNotNull(deserializedSession.getTexts());

      }
      catch (ClassNotFoundException e) {
        fail("Unable to deserialize AbstractServerSession: ClassNotFoundException");
      }
      catch (IOException e) {
        fail("Unable to deserialize AbstractServerSession");
      }
    }
  }
}
