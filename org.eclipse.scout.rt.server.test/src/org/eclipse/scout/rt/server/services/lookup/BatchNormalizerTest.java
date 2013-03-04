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
package org.eclipse.scout.rt.server.services.lookup;

import java.util.List;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.rt.server.internal.Activator;
import org.eclipse.scout.rt.shared.services.lookup.BatchLookupCall;
import org.eclipse.scout.rt.shared.services.lookup.BatchLookupNormalizer;
import org.eclipse.scout.rt.shared.services.lookup.BatchLookupResultCache;
import org.eclipse.scout.rt.shared.services.lookup.IBatchLookupService;
import org.eclipse.scout.rt.shared.services.lookup.ILookupService;
import org.eclipse.scout.rt.shared.services.lookup.LookupCall;
import org.eclipse.scout.rt.shared.services.lookup.LookupRow;
import org.eclipse.scout.rt.testing.shared.TestingUtility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

/**
 * Test {@link IBatchLookupService} and caching with {@link BatchLookupResultCache}
 */
public class BatchNormalizerTest {
  private List<ServiceRegistration> m_reg;
  private static long m_serverInvocations;

  @Before
  public void setUp() throws Exception {
    //register services
    m_reg = TestingUtility.registerServices(Activator.getDefault().getBundle(), 0, new FruitLookupService());
  }

  @After
  public void tearDown() throws Exception {
    TestingUtility.unregisterServices(m_reg);
  }

  /**
   * <ul>
   * <li>Calls: not null</li>
   * <li>Keys: not null</li>
   * <li>Cacheable: all</li>
   * </ul>
   */
  @Test
  public void testNice() throws Exception {
    BatchLookupCall batchCall = new BatchLookupCall();
    for (int i = 0; i < 1000; i++) {
      FruitLookupCall call = new FruitLookupCall();
      call.setKey(new Long((i / 100) + 1));
      batchCall.addLookupCall((LookupCall) call);
    }
    testInternal(batchCall, 10, 10, 0, 1000);
  }

  /**
   * <ul>
   * <li>Calls: some null</li>
   * <li>Keys: not null</li>
   * <li>Cacheable: all</li>
   * </ul>
   */
  @Test
  public void testNullCalls() throws Exception {
    BatchLookupCall batchCall = new BatchLookupCall();
    for (int i = 0; i < 1000; i++) {
      FruitLookupCall call = new FruitLookupCall();
      long key = (i / 100) + 1;
      call.setKey(key);
      if (key == 5L || key == 6L || key == 9L) {
        call = null;
      }
      batchCall.addLookupCall((LookupCall) call);
    }
    testInternal(batchCall, 7, 7, 300, 700);
  }

  /**
   * <ul>
   * <li>Calls: not null</li>
   * <li>Keys: some null</li>
   * <li>Cacheable: all</li>
   * </ul>
   */
  @Test
  public void testNullKeys() throws Exception {
    BatchLookupCall batchCall = new BatchLookupCall();
    for (int i = 0; i < 1000; i++) {
      FruitLookupCall call = new FruitLookupCall();
      long key = (i / 100) + 1;
      call.setKey(key);
      if (key == 5L || key == 6L || key == 9L) {
        call.setKey(null);
      }
      batchCall.addLookupCall((LookupCall) call);
    }
    testInternal(batchCall, 8, 7, 0, 700);
  }

  /**
   * <ul>
   * <li>Calls: not null</li>
   * <li>Keys: not null</li>
   * <li>Cacheable: only some</li>
   * </ul>
   */
  @Test
  public void testCacheable() throws Exception {
    BatchLookupCall batchCall = new BatchLookupCall();
    for (int i = 0; i < 1000; i++) {
      boolean cacheable = (i % 2 == 0);
      LookupCall call = (cacheable ? new FruitLookupCall() : new FruitLookupCallNonCacheable());
      long key = (i / 100) + 1;
      call.setKey(key);
      batchCall.addLookupCall((LookupCall) call);
    }
    testInternal(batchCall, 10 + 500, 10 + 500, 0, 1000);
  }

  private void testInternal(BatchLookupCall batchCall, long expectedNormalizedSize, long expectedServerInvocations, long expectedNullArrayCount, long expectedTotalResultRowCount) throws Exception {
    m_serverInvocations = 0;
    //
    BatchLookupNormalizer normalizer = new BatchLookupNormalizer();
    LookupCall[] callArray = batchCall.getCallBatch();
    LookupCall[] normCallArray = normalizer.normalizeCalls(callArray);
    LookupRow[][] normResultArray = new BatchLookupService().getBatchDataByKey(new BatchLookupCall(normCallArray));
    LookupRow[][] resultArray = normalizer.denormalizeResults(normResultArray);
    //
    Assert.assertEquals(resultArray.length, callArray.length);
    Assert.assertEquals(normResultArray.length, normCallArray.length);
    Assert.assertEquals(expectedNormalizedSize, normResultArray.length);
    Assert.assertEquals(expectedServerInvocations, m_serverInvocations);
    int rowCount = 0;
    int nullArrayCount = 0;
    for (int i = 0; i < resultArray.length; i++) {
      if (resultArray[i] == null) {
        nullArrayCount++;
      }
      else if (resultArray[i].length == 1) {
        rowCount++;
        Assert.assertEquals(callArray[i].getKey(), resultArray[i][0].getKey());
        Assert.assertEquals(dumpCall(callArray[i]), resultArray[i][0].getText());
      }
      else if (resultArray[i].length > 1) {
        Assert.fail("result length is expected to be 0 or 1");
      }
    }
    Assert.assertEquals(expectedNullArrayCount, nullArrayCount);
    Assert.assertEquals(expectedTotalResultRowCount, rowCount);
  }

  private static LookupRow[] createCallResult(LookupCall call) {
    return new LookupRow[]{new LookupRow(call.getKey(), dumpCall(call))};
  }

  private static String dumpCall(LookupCall call) {
    return "Fruit[key=" + call.getKey() + ", text=" + call.getText() + "]";
  }

  public static class FruitLookupCall extends LookupCall {
    private static final long serialVersionUID = 1L;

    @Override
    protected final Class<? extends ILookupService> getConfiguredService() {
      return IFruitLookupService.class;
    }
  }

  /**
   * not cacheable since there is a member but no equals override, see {@link BatchLookupResultCache#isCacheable(Class)}
   */
  public static class FruitLookupCallNonCacheable extends LookupCall {
    private static final long serialVersionUID = 1L;

    private String m_meta;

    @Override
    protected final Class<? extends ILookupService> getConfiguredService() {
      return IFruitLookupService.class;
    }

    public String getMeta() {
      return m_meta;
    }

    public void setMeta(String meta) {
      m_meta = meta;
    }
  }

  public static interface IFruitLookupService extends ILookupService {
  }

  public static class FruitLookupService extends AbstractLookupService implements IFruitLookupService {

    @Override
    public LookupRow[] getDataByKey(LookupCall call) throws ProcessingException {
      m_serverInvocations++;
      return createCallResult(call);
    }

    @Override
    public LookupRow[] getDataByText(LookupCall call) throws ProcessingException {
      m_serverInvocations++;
      return createCallResult(call);
    }

    @Override
    public LookupRow[] getDataByAll(LookupCall call) throws ProcessingException {
      m_serverInvocations++;
      return createCallResult(call);
    }

    @Override
    public LookupRow[] getDataByRec(LookupCall call) throws ProcessingException {
      m_serverInvocations++;
      return createCallResult(call);
    }
  }
}
