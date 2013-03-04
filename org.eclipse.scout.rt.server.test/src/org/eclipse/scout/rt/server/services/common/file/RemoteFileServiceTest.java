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
package org.eclipse.scout.rt.server.services.common.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;

import org.eclipse.scout.commons.IOUtility;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.server.internal.Activator;
import org.eclipse.scout.rt.shared.services.common.file.IRemoteFileService;
import org.eclipse.scout.rt.shared.services.common.file.RemoteFile;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.eclipse.scout.service.SERVICES;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

/**
 * Test for security leaks on accessing files.
 * Mock file structure contains two files, one internal one public, the root for the remotefile service is
 * /internal/public
 * 
 * <pre>
 * /internal/config.ini = "INTERNAL"
 * /internal/public/index.html = "PUBLIC"
 * /internal/public/index_de.html = "de"
 * /internal/public/index_de_CH.html = "de_CH"
 * /internal/public/index_de_CH_V.html = "de_CH_V"
 * /internal/public/index_de_CH_V_X_Y.html = "de_CH_V_X_Y"
 * </pre>
 */
public class RemoteFileServiceTest {

  private File m_fsroot;
  private List<ServiceRegistration> m_reg;

  @Before
  public void setupMockFileSystem() throws Exception {
    tearDownMockFileSystem();
    m_fsroot = IOUtility.createTempDirectory("junit");
    m_fsroot.mkdirs();
    //
    createFile("internal/config.ini", "INTERNAL");
    createFile("internal/public/index.html", "PUBLIC");
    createFile("internal/public/index_de.html", "de");
    createFile("internal/public/index_de_CH.html", "de_CH");
    createFile("internal/public/index_de_CH_V.html", "de_CH_V");
    createFile("internal/public/index_de_CH_V_X_Y.html", "de_CH_V_X_Y");
    //register service
    RemoteFileServiceMock service = new RemoteFileServiceMock();
    service.setRootPath(m_fsroot + File.separator + "internal" + File.separator + "public");
    m_reg = TestingUtility.registerServices(Activator.getDefault().getBundle(), 0, service);
  }

  private void createFile(String path, String content) throws Exception {
    File f = new File(m_fsroot, path);
    f.getParentFile().mkdirs();
    IOUtility.writeContent(f.getAbsolutePath(), content);
  }

  @After
  public void tearDownMockFileSystem() {
    TestingUtility.unregisterServices(m_reg);
    if (m_fsroot != null) {
      IOUtility.deleteDirectory(m_fsroot);
      m_fsroot = null;
    }
  }

  @Test
  public void test() throws Exception {
    IRemoteFileService svc = SERVICES.getService(IRemoteFileService.class);
    assertEquals(RemoteFileServiceMock.class, svc.getClass());
    assertAccessible(svc, new RemoteFile(null, "index.html", 0L), "PUBLIC");
    assertAccessible(svc, new RemoteFile("/", "index.html", 0L), "PUBLIC");
    assertAccessible(svc, new RemoteFile("/abc/..", "index.html", 0L), "PUBLIC");
    assertAccessible(svc, new RemoteFile("", "index.html", new Locale("de"), 0L), "de");
    assertAccessible(svc, new RemoteFile("", "index.html", new Locale("de_CH"), 0L), "de_CH");
    assertAccessible(svc, new RemoteFile("", "index.html", new Locale("de_CH_V"), 0L), "de_CH_V");
    assertAccessible(svc, new RemoteFile("", "index.html", new Locale("de_CH_V_X_Y"), 0L), "de_CH_V_X_Y");
    assertAccessible(svc, new RemoteFile("", "index.html", new Locale("de_CH_V_Q"), 0L), "de_CH_V");
    assertAccessible(svc, new RemoteFile("", "index.html", new Locale("de_XH"), 0L), "de");
    assertAccessible(svc, new RemoteFile("", "index.html", new Locale("en"), 0L), "PUBLIC");
    assertAccessible(svc, new RemoteFile("", "index_en.html", new Locale("en"), 0L), null);
    assertAccessible(svc, new RemoteFile("", "index_de.html", new Locale("de"), 0L), "de");
    assertNonAccessible(svc, new RemoteFile("/", "abc/../index.html", 0L));
    assertNonAccessible(svc, new RemoteFile("..", "config.ini", 0L));
    assertNonAccessible(svc, new RemoteFile("/..", "config.ini", 0L));
    assertNonAccessible(svc, new RemoteFile("./..", "config.ini", 0L));
    assertNonAccessible(svc, new RemoteFile(null, "../config.ini", 0L));
    assertNonAccessible(svc, new RemoteFile(null, "./../config.ini", 0L));
    assertNonAccessible(svc, new RemoteFile(null, "../config.ini", 0L));
    assertNonAccessible(svc, new RemoteFile("", "config.ini", new Locale("de/../../config"), 0L));
  }

  private void assertAccessible(IRemoteFileService svc, RemoteFile spec, String expectedContent) throws Exception {
    RemoteFile r = svc.getRemoteFile(spec);
    if (expectedContent == null) {
      assertFalse(r.exists());
      return;
    }
    StringWriter w = new StringWriter();
    r.writeData(w);
    assertEquals(expectedContent, w.toString());
  }

  private void assertNonAccessible(IRemoteFileService svc, RemoteFile spec) throws Exception {
    RemoteFile r;
    try {
      r = svc.getRemoteFile(spec);
    }
    catch (SecurityException e) {
      return;
    }
    //should have failed
    StringWriter w = new StringWriter();
    r.writeData(w);
    fail("accessing " + spec.getDirectory() + spec.getName() + " should fail");
  }

  static class RemoteFileServiceMock extends RemoteFileService {

    public RemoteFileServiceMock() throws ProcessingException {
      super();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void initializeService() {
    }

    @Override
    public void initializeService(ServiceRegistration registration) {
    }

    @Override
    public void setRootPath(String rootPath) throws ProcessingException {
      super.setRootPath(rootPath);
    }

  }
}
