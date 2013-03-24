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
package org.eclipse.scout.testing.client;

import org.eclipse.scout.rt.testing.client.IGuiMock;

/**
 * Deprecated: use {@link org.eclipse.scout.rt.testing.client.AbstractTestWithGuiScript} instead
 * will be removed with the L-Release.
 */
@Deprecated
public abstract class AbstractTestWithGuiScript extends org.eclipse.scout.rt.testing.client.AbstractTestWithGuiScript {

  /**
   * Override this method
   * <p>
   * This method runs in the ui thread.
   */
  protected void runGui(org.eclipse.scout.testing.client.IGuiMock gui) throws Throwable {
  }

  @Override
  protected void runGui(IGuiMock gui) throws Throwable {
    runGui((org.eclipse.scout.testing.client.IGuiMock) gui);
  }
}
