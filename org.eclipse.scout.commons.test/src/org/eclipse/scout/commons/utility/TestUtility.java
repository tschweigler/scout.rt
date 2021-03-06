/*******************************************************************************
 * Copyright (c) 2012 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Leicht Vogt - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.commons.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.scout.commons.IOUtility;

/**
 * Utility Class for Unit Testing
 */
public final class TestUtility {

  public static File createTempFileFromResource(InputStream inputStream) {
    File temp = null;
    FileOutputStream fo = null;
    try {
      temp = File.createTempFile("temp", "zip");
      fo = new FileOutputStream(temp);
      IOUtility.writeContent(fo, IOUtility.getContent(inputStream));
    }
    catch (Exception ex) {
      //nop -> test will fail. Which is ok.
    }
    finally {
      if (fo != null) {
        try {
          fo.close();
        }
        catch (IOException e) {
          // nop
        }
      }
    }
    return temp;
  }

  public static void deleteTempFile(File tempFile) {
    if (tempFile != null && tempFile.exists()) {
      tempFile.delete();
    }
  }

  private TestUtility() {
    //empty hidden constructor
  }
}
