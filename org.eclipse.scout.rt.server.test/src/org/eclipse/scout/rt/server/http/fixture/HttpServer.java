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
package org.eclipse.scout.rt.server.http.fixture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class HttpServer extends Thread {
  private final ServerSocket m_server;

  public HttpServer() throws IOException {
    m_server = new ServerSocket(0);
    System.out.println("SERVER: Listeining on " + m_server.getLocalPort());
  }

  public int getPort() {
    return m_server.getLocalPort();
  }

  @Override
  public void run() {
    try {
      Socket client;
      while ((client = m_server.accept()) != null) {
        try {
          System.out.println("SERVER: Client from " + client.getInetAddress());
          try {
            handleRequest(client);
          }
          finally {
            client.close();
          }
        }
        catch (Throwable t) {
          t.printStackTrace();
        }
      }
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public void stopServer() {
    try {
      m_server.close();
    }
    catch (IOException e) {
      //nop
    }
  }

  private void handleRequest(Socket client) throws UnsupportedEncodingException, IOException {
    InputStream in = client.getInputStream();
    int ch;
    int crlfIndex = 0;
    ByteArrayOutputStream headBuf = new ByteArrayOutputStream();
    while (crlfIndex < 4 && (ch = in.read()) >= 0) {
      switch (ch) {
        case 13: {
          headBuf.write('\n');
          if (crlfIndex == 0 || crlfIndex == 2) {
            crlfIndex++;
          }
          else {
            crlfIndex = 1;
          }
          break;
        }
        case 10: {
          if (crlfIndex == 1) {
            crlfIndex++;
          }
          else if (crlfIndex == 3) {
            //end header
            crlfIndex++;
          }
          else {
            crlfIndex = 0;
          }
          break;
        }
        default: {
          headBuf.write(ch);
          crlfIndex = 0;
          break;
        }
      }
    }
    String[] head = headBuf.toString("UTF-8").trim().split("\n");
    String command = head[0];
    int code = acceptCommand(command);
    if (code == 200) {
      System.out.println("SERVER: " + command);
      HashMap<String, String> headerMap = new HashMap<String, String>();
      for (int i = 1; i < head.length; i++) {
        System.out.println("SERVER: " + head[i]);
        String[] a = head[i].split(":", 2);
        if (a.length == 2) {
          headerMap.put(a[0].trim(), a[1].trim());
        }
      }
      int contentLength = Integer.parseInt(headerMap.get("Content-Length"));
      byte[] content = null;
      if (contentLength > 0) {
        content = new byte[contentLength];
        for (int i = 0; i < contentLength; i++) {
          content[i] = (byte) in.read();
        }
        System.out.println("SERVER: " + new String(content, "UTF-8"));
      }
      send200Reponse(client, command, headerMap, content);
    }
    else if (code == 400) {
      System.out.println("SERVER: " + command);
      send400Reponse(client);
    }
    else if (code == -1) {
      System.out.println("SERVER: " + command);
      sendTextErrorReponse(client);
    }
  }

  private int acceptCommand(String command) {
    if (command.indexOf("fail_400") >= 0) {
      return 400;
    }
    if (command.indexOf("fail_plain") >= 0) {
      return -1;
    }
    return 200;
  }

  private void send200Reponse(Socket client, String command, Map<String, String> headerMap, byte[] content) throws UnsupportedEncodingException, IOException {
    StringBuffer html = new StringBuffer();
    html.append("<html>");
    html.append("<body>");
    html.append("Hello World");
    html.append("</body>");
    html.append("</html>");
    byte[] outContent = html.toString().getBytes("UTF-8");
    //
    OutputStream rawOut = client.getOutputStream();
    PrintStream out = new PrintStream(rawOut, true, "UTF-8");
    out.print("HTTP/1.0 200 OK");
    out.write(13);
    out.write(10);
    out.print("Content-Type: text/html");
    out.write(13);
    out.write(10);
    out.print("Content-Length: " + outContent.length);
    out.write(13);
    out.write(10);
    out.write(13);
    out.write(10);
    out.write(outContent);
  }

  private void send400Reponse(Socket client) throws UnsupportedEncodingException, IOException {
    StringBuffer html = new StringBuffer();
    html.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n");
    html.append("<html><head>\n");
    html.append("<title>400 Bad Request</title>\n");
    html.append("</head><body>\n");
    html.append("<h1>Bad Request</h1>\n");
    html.append("<p>Your browser sent a request that this server could not understand.<br />\n");
    html.append("</p>\n");
    html.append("<hr>\n");
    html.append("<address>Apache/2.3.8 (Unix) mod_ssl/2.3.8 OpenSSL/1.0.0a Server at www.apache.org Port 80</address>\n");
    html.append("\n");
    html.append("</body></html>\n");
    html.append("<html>");
    byte[] outContent = html.toString().getBytes("UTF-8");
    //
    OutputStream rawOut = client.getOutputStream();
    PrintStream out = new PrintStream(rawOut, true, "UTF-8");
    out.print("HTTP/1.0 400 Bad Request");
    out.write(13);
    out.write(10);
    out.print("Content-Type: text/html");
    out.write(13);
    out.write(10);
    out.print("Content-Length: " + outContent.length);
    out.write(13);
    out.write(10);
    out.print("Connection: keep-alive");
    out.write(13);
    out.write(10);
    out.write(13);
    out.write(10);
    out.write(outContent);
  }

  private void sendTextErrorReponse(Socket client) throws UnsupportedEncodingException, IOException {
    StringBuffer html = new StringBuffer();
    html.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n");
    html.append("<html><head>\n");
    html.append("<title>400 Bad Request</title>\n");
    html.append("</head><body>\n");
    html.append("<h1>Bad Request</h1>\n");
    html.append("<p>Your browser sent a request that this server could not understand.<br />\n");
    html.append("</p>\n");
    html.append("<hr>\n");
    html.append("<address>Apache/2.3.8 (Unix) mod_ssl/2.3.8 OpenSSL/1.0.0a Server at www.apache.org Port 80</address>\n");
    html.append("\n");
    html.append("</body></html>\n");
    html.append("<html>");
    byte[] outContent = html.toString().getBytes("UTF-8");
    //
    OutputStream rawOut = client.getOutputStream();
    rawOut.write(outContent);
  }
}
