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
package org.eclipse.scout.rt.server.http;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.scout.rt.testing.commons.ScoutAssert;
import org.eclipse.scout.rt.testing.server.runner.ScoutServerTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ScoutServerTestRunner.class)
public class HttpKeepAliveTest {

  static ArrayList<String> protocol = new ArrayList<String>();

  /**
   * Test what happens in java http client when server returns 200, 400, no code.
   */
  @Test
  public void test() throws Exception {
    HttpServer server = new HttpServer();
    server.start();
    doRequest("ok", server.getPort());
    //The word "fail_400" is simulated on the server to be a bad request and returned with response code 400.
    doRequest("fail_400", server.getPort());
    //The word "fail_plain" is simulated on the server to be a bad request and returned with NO response code and plain html only.
    doRequest("fail_plain", server.getPort());
    server.stopServer();
    server.join();
    ScoutAssert.assertListEquals(new String[]{"ok", "200", "37", "fail_400", "400", "339", "fail_plain", "-1", "-1"}, protocol);
  }

  private void doRequest(String path, int port) {
    try {
      protocol.add(path);
      URL url = new URL("http://localhost:" + port + "/test/request/" + path);
      System.out.println("-------------------");
      System.out.println("CLIENT: Request: " + url);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoOutput(true);
      conn.getOutputStream().write("ABCDEFG".getBytes());
      conn.getOutputStream().close();
      //
      int code = conn.getResponseCode();
      protocol.add("" + code);
      System.out.println("CLIENT: Code: " + code);
      int contentLength = conn.getContentLength();
      protocol.add("" + contentLength);
      System.out.println("CLIENT: Content-Length: " + contentLength);
      if (code == 200 && contentLength > 0) {
        InputStream rawIn = conn.getInputStream();
        byte[] content = new byte[contentLength];
        for (int k = 0; k < contentLength; k++) {
          content[k] = (byte) rawIn.read();
        }
        System.out.println("CLIENT: " + new String(content, 0, content.length, "UTF-8"));
      }
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
