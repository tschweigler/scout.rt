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
package org.eclipse.scout.rt.ui.rap.window;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.rap.rwt.client.service.UrlLauncher;
import org.eclipse.scout.commons.StringUtility;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.ui.rap.AbstractRwtEnvironment;
import org.eclipse.scout.rt.ui.rap.window.filedownloader.RwtScoutDownloadHandler;

/**
 * @since 3.8.0
 */
public class BrowserWindowHandler {
  private static IScoutLogger LOG = ScoutLogManager.getLogger(AbstractRwtEnvironment.class);

  public void openLink(String link) {
    if (link == null) {
      return;
    }

    if (isEmailLink(link)) {
      openLinkInSameBrowserWindow(link);
    }
    else if (isTelLink(link)) {
      openLinkInSameBrowserWindow(link);
    }
    else if (isHttpLink(link)) {
      openLinkInNewBrowserWindow(link);
    }
    else {
      downloadFile(link);
    }
  }

  public boolean isEmailLink(String link) {
    if (link != null && link.startsWith("mailto:")) {
      return true;
    }

    return false;
  }

  public boolean isTelLink(String link) {
    if (link != null && link.startsWith("tel:")) {
      return true;
    }

    return false;
  }

  public boolean isHttpLink(String link) {
    if ((StringUtility.find(link, "http://") >= 0) || (StringUtility.find(link, "https://") >= 0)) {
      return true;
    }

    return false;
  }

  public void openLinkInNewBrowserWindow(String link) {
    if (!isHttpLink(link)) {
      return;
    }

    UrlLauncher launcher = RWT.getClient().getService(UrlLauncher.class);
    if (launcher != null) {
      launcher.openURL(link);
    }
  }

  public void openLinkInSameBrowserWindow(String link) {
    if (link == null) {
      return;
    }
    JavaScriptExecutor executor = RWT.getClient().getService(JavaScriptExecutor.class);
    if (executor != null) {
      executor.execute("window.location='" + link + "'");
    }
  }

  public void downloadFile(String link) {
    try {
      File file = validateLink(link);
      String nextId = UUID.randomUUID().toString();
      RwtScoutDownloadHandler handler = new RwtScoutDownloadHandler(nextId, file, "", file.getName());
      handler.startDownload();
    }
    catch (IOException e) {
      LOG.error("Unexpected: " + link, e);
    }
  }

  protected File validateLink(String link) throws IOException {
    String px = link.replace('\\', File.separatorChar);
    File file = new File(px);
    if (file.exists()) {
      px = file.getCanonicalPath();
      String osName = System.getProperty("os.name");
      if (osName != null && osName.startsWith("Mac OS")) {
        //mac is not able to open files with a space, even when in quotes
        String ext = px.substring(px.lastIndexOf('.'));
        File f = new File(file.getParentFile(), "" + System.nanoTime() + ext);
        file.renameTo(f);
        f.deleteOnExit();
      }
    }
    return file;
  }
}
