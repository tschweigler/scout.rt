package org.eclipse.scout.rt.client.fixture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MockHttpURLConnection extends HttpURLConnection {
  private ByteArrayOutputStream m_out;
  private ByteArrayInputStream m_in;
  private String m_statusLine;

  protected MockHttpURLConnection(URL u) {
    super(u);
    m_out = new ByteArrayOutputStream();
  }

  @Override
  public void connect() throws IOException {
  }

  @Override
  public void disconnect() {
  }

  @Override
  public boolean usingProxy() {
    return false;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return m_out;
  }

  @Override
  public String getHeaderField(int n) {
    if (n == 0) {
      return m_statusLine;
    }
    return null;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (m_in == null) {
      final AtomicInteger scRef = new AtomicInteger(500);
      final ByteArrayOutputStream servletOut = new ByteArrayOutputStream();
      final ByteArrayInputStream servletIn = new ByteArrayInputStream(m_out.toByteArray());
      m_out = null;
      Thread t = new Thread() {
        @Override
        public void run() {
          try {
            int sc = mockHttpServlet(servletIn, servletOut);
            scRef.set(sc);
          }
          catch (Throwable f) {
            //nop
          }
        }
      };
      t.start();
      while (t.isAlive()) {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException ie) {
          //nop
        }
      }
      m_in = new ByteArrayInputStream(servletOut.toByteArray());
      int sc = scRef.get();
      m_statusLine = "HTTP/1.0 " + sc + " " + (sc == 200 ? "OK" : "NOK");
    }
    return m_in;
  }

  /**
   * @return the http response code
   *         write to the output stream
   */
  protected abstract int mockHttpServlet(InputStream servletIn, OutputStream servletOut) throws Exception;

}
