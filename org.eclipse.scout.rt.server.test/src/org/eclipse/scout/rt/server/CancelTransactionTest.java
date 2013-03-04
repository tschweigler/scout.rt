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
package org.eclipse.scout.rt.server;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.server.fixture.ServiceTunnelServletCall;
import org.eclipse.scout.rt.server.internal.Activator;
import org.eclipse.scout.rt.shared.services.common.exceptionhandler.IExceptionHandlerService;
import org.eclipse.scout.rt.shared.services.common.ping.IPingService;
import org.eclipse.scout.rt.shared.services.common.processing.IServerProcessingCancelService;
import org.eclipse.scout.rt.shared.servicetunnel.ServiceTunnelRequest;
import org.eclipse.scout.rt.shared.servicetunnel.ServiceTunnelResponse;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.eclipse.scout.service.AbstractService;
import org.eclipse.scout.service.IService2;
import org.eclipse.scout.service.SERVICES;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

/**
 * Checks what happens when a transaction is cancelled via remote service call.
 * <p>
 * No exception should appear in the log
 */
@Ignore
public class CancelTransactionTest {
  private Handler m_handler;
  private List<ServiceRegistration> m_reg;
  private int m_errorOrWarningCount;

  @Before
  public void setUp() throws Exception {
    // register services
    m_reg = TestingUtility.registerServices(Activator.getDefault().getBundle(), 0, new BulkOperationService());
    // attach to log
    Logger log = Logger.getLogger(SERVICES.getService(IExceptionHandlerService.class).getClass().getName());
    m_handler = new Handler() {
      @Override
      public void publish(LogRecord record) {
        if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
          m_errorOrWarningCount++;
        }
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() throws SecurityException {
      }
    };
    log.addHandler(m_handler);
  }

  @After
  public void tearDown() throws Exception {
    TestingUtility.unregisterServices(m_reg);
    Logger log = Logger.getLogger(SERVICES.getService(IExceptionHandlerService.class).getClass().getName());
    log.removeHandler(m_handler);
  }

  @Test
  public void test() throws Exception {
    String virtualSessionId = "999999";
    m_errorOrWarningCount = 0;
    // check basic functionality
    ServiceTunnelRequest req0 = new ServiceTunnelRequest("9.9.9", IPingService.class.getName(), "ping", new Class[]{String.class}, new Object[]{"hello"});
    req0.setVirtualSessionId(virtualSessionId);
    ServiceTunnelServletCall call0 = new ServiceTunnelServletCall(req0);
    call0.start();
    call0.join();
    ServiceTunnelResponse res0 = call0.getServiceTunnelResponse();
    if (res0.getException() != null) {
      System.out.println("$$$>>" + res0);
      res0.getException().printStackTrace();
    }
    Assert.assertNull(res0.getException());
    Assert.assertEquals("hello", res0.getData());
    // start long running job
    ServiceTunnelRequest req1 = new ServiceTunnelRequest("9.9.9", IBulkOperationService.class.getName(), "updateLargeDataset", null, null);
    req1.setVirtualSessionId(virtualSessionId);
    ServiceTunnelServletCall call1 = new ServiceTunnelServletCall(req1);
    call1.start();
    // cancel job
    Thread.sleep(4000L);
    ServiceTunnelRequest req2 = new ServiceTunnelRequest("9.9.9", IServerProcessingCancelService.class.getName(), "cancel", new Class[]{long.class}, new Object[]{req1.getRequestSequence()});
    req2.setVirtualSessionId(virtualSessionId);
    ServiceTunnelServletCall call2 = new ServiceTunnelServletCall(req2);
    call2.start();
    call2.join();
    ServiceTunnelResponse res2 = call2.getServiceTunnelResponse();
    Assert.assertNull(res2.getException());
    Assert.assertEquals(true, res2.getData());
    // now wait until the long running job returns
    call1.join();
    ServiceTunnelResponse res1 = call1.getServiceTunnelResponse();
    Assert.assertNotNull(res1.getException());
    Assert.assertNull(res1.getData());
    Assert.assertEquals("The log must not contain any errors or warings due to cancel", 0, m_errorOrWarningCount);
  }

  public static interface IBulkOperationService extends IService2 {
    void updateLargeDataset() throws ProcessingException;
  }

  public static class BulkOperationService extends AbstractService implements IBulkOperationService {
    @Override
    public void updateLargeDataset() throws ProcessingException {
      System.out.println("bulk started");
      // takes loooog time
      while (true) {
        try {
          Thread.sleep(200L);
        }
        catch (InterruptedException e) {
          // nop
        }
        if (ThreadContext.getTransaction().isCancelled()) {
          System.out.println("bulk interrupted");
          throw new ProcessingException("M-999 - MockCancelTest", new SQLException("M-999 - MockCancelTest"));
        }
      }
    }
  }
}
