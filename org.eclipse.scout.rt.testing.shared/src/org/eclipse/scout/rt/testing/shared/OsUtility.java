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
package org.eclipse.scout.rt.testing.shared;

/**
 * Utility class to check for the underlying Operating System name.
 * Currently, only the latest Windows versions are implemented.
 * Deprecated: use {@link org.eclipse.scout.rt.testing.commons.OsUtility} instead.
 * will be removed with the L-Release.
 */
@Deprecated
public class OsUtility {
  private OsUtility() {
  }

  public static boolean isWindows7() {
    return org.eclipse.scout.rt.testing.commons.OsUtility.isWindows7();
  }

  public static boolean isWindowsVista() {
    return org.eclipse.scout.rt.testing.commons.OsUtility.isWindowsVista();
  }

  public static boolean isWindowsXP() {
    return org.eclipse.scout.rt.testing.commons.OsUtility.isWindowsXP();
  }
}
