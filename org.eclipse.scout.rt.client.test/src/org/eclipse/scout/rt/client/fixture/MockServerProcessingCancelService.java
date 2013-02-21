package org.eclipse.scout.rt.client.fixture;

import org.eclipse.scout.rt.shared.services.common.processing.IServerProcessingCancelService;
import org.eclipse.scout.service.AbstractService;

public class MockServerProcessingCancelService extends AbstractService implements IServerProcessingCancelService {
  private MockServiceTunnel m_tunnel;

  public MockServerProcessingCancelService(MockServiceTunnel tunnel) {
    m_tunnel = tunnel;
  }

  @Override
  public boolean cancel(long requestSequence) {
    Thread t = m_tunnel.getThreadByRequestSequence(requestSequence);
    if (t != null) {
      t.interrupt();
      return true;
    }
    return false;
  }
}
