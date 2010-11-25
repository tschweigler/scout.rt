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
package org.eclipse.scout.rt.client.services.common.perf.internal;

import java.beans.PropertyChangeListener;

import org.eclipse.scout.commons.annotations.Priority;
import org.eclipse.scout.commons.beans.BasicPropertySupport;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.services.common.perf.IPerformanceAnalyzerService;
import org.eclipse.scout.service.AbstractService;

@Priority(-1)
public class PerformanceAnalyzerService extends AbstractService implements IPerformanceAnalyzerService {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(PerformanceAnalyzerService.class);

  private final PerformanceSampleSet m_networkLatency;
  private final PerformanceSampleSet m_serverExecutionTime;
  private final BasicPropertySupport m_propertySupport;

  public PerformanceAnalyzerService() {
    m_propertySupport = new BasicPropertySupport(this);
    m_networkLatency = new PerformanceSampleSet(10, 70);
    m_serverExecutionTime = new PerformanceSampleSet(10, 100);
  }

  public void addNetworkLatencySample(long millis) {
    long oldValue = m_networkLatency.getValue();
    m_networkLatency.addSample(millis);
    long newValue = m_networkLatency.getValue();
    try {
      m_propertySupport.firePropertyChange(PROP_NETWORK_LATENCY, oldValue, newValue);
    }
    catch (Throwable t) {
      LOG.warn(null, t);
    }
  }

  public long getNetworkLatency() {
    return m_networkLatency.getValue();
  }

  public void addServerExecutionTimeSample(long millis) {
    long oldValue = m_serverExecutionTime.getValue();
    m_serverExecutionTime.addSample(millis);
    long newValue = m_serverExecutionTime.getValue();
    try {
      m_propertySupport.firePropertyChange(PROP_SERVER_EXECUTION_TIME, oldValue, newValue);
    }
    catch (Throwable t) {
      LOG.warn(null, t);
    }
  }

  public long getServerExecutionTime() {
    return m_serverExecutionTime.getValue();
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    m_propertySupport.addPropertyChangeListener(listener);
  }

  public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    m_propertySupport.addPropertyChangeListener(propertyName, listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    m_propertySupport.removePropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    m_propertySupport.removePropertyChangeListener(propertyName, listener);
  }
}
