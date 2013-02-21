package org.eclipse.scout.rt.client.fixture;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.servicetunnel.http.HttpServiceTunnel;
import org.eclipse.scout.rt.shared.servicetunnel.ServiceTunnelRequest;
import org.eclipse.scout.rt.shared.servicetunnel.ServiceTunnelResponse;
import org.eclipse.scout.service.SERVICES;
import org.eclipse.scout.service.ServiceUtility;

public class MockServiceTunnel extends HttpServiceTunnel {

  private final HashMap<Long, Thread> m_runningMap = new HashMap<Long, Thread>();

  public MockServiceTunnel(IClientSession session) throws Exception {
    super(session, "http://mock/process");
    resetRequestSequenceGenerator();
  }

  public static void resetRequestSequenceGenerator() throws Exception {
    System.out.println(MockServiceTunnel.class.getSimpleName() + ".resetRequestSequenceGenerator");
    Field f = ServiceTunnelRequest.class.getDeclaredField("requestSequenceGenerator");
    f.setAccessible(true);
    AtomicLong gen = (AtomicLong) f.get(null);
    gen.set(0);
  }

  public Thread getThreadByRequestSequence(long requestSequence) {
    return m_runningMap.get(requestSequence);
  }

  /**
   * @return the service response
   *         You may call callTargetService() to simply call a service for test purpose (without a transaction!)
   */
  protected ServiceTunnelResponse mockServiceCall(ServiceTunnelRequest req) throws Exception {
    try {
      Class<?> serviceInterface = Class.forName(req.getServiceInterfaceClassName());
      Method serviceOperation = ServiceUtility.getServiceOperation(serviceInterface, req.getOperation(), req.getParameterTypes());
      Object service = null;
      for (Object t : SERVICES.getServices(serviceInterface)) {
        if (Proxy.isProxyClass(t.getClass())) {
          continue;
        }
        service = t;
        break;
      }
      Object result = ServiceUtility.invoke(serviceOperation, service, req.getArgs());
      return new ServiceTunnelResponse(200, result, null, null);
    }
    catch (ProcessingException pe) {
      return new ServiceTunnelResponse(200, null, null, pe);
    }
    catch (Throwable t) {
      return new ServiceTunnelResponse(200, null, null, t);
    }
  }

  @Override
  protected URLConnection createURLConnection(final ServiceTunnelRequest call, byte[] callData) throws IOException {
    URLConnection urlConn = new MockHttpURLConnection(getServerURL()) {
      @Override
      protected int mockHttpServlet(InputStream servletIn, OutputStream servletOut) throws Exception {
        ServiceTunnelRequest req = getContentHandler().readRequest(servletIn);
        try {
          m_runningMap.put(call.getRequestSequence(), Thread.currentThread());
          //
          ServiceTunnelResponse res = MockServiceTunnel.this.mockServiceCall(req);
          getContentHandler().writeResponse(servletOut, res);
          return res.getHttpCode();
        }
        finally {
          m_runningMap.remove(call.getRequestSequence());
        }
      }
    };
    //
    String contentType = "text/xml";
    urlConn.setRequestProperty("Content-type", contentType);
    urlConn.setDoOutput(true);
    urlConn.setDoInput(true);
    urlConn.setDefaultUseCaches(false);
    urlConn.setUseCaches(false);
    //
    OutputStream httpOut = urlConn.getOutputStream();
    httpOut.write(callData);
    httpOut.close();
    httpOut = null;
    return urlConn;
  }
}
