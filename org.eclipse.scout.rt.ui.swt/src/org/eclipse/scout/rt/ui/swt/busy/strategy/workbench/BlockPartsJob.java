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
package org.eclipse.scout.rt.ui.swt.busy.strategy.workbench;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.busy.BusyJob;
import org.eclipse.scout.rt.ui.swt.busy.SwtBusyHandler;
import org.eclipse.scout.rt.ui.swt.window.ISwtScoutPart;
import org.eclipse.swt.widgets.Display;

/**
 * Default SWT busy handler for a {@link org.eclipse.scout.rt.client.IClientSession IClientSession}
 * 
 * @author imo
 * @since 3.8
 */
public class BlockPartsJob extends BusyJob {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(BlockPartsJob.class);

  private final List<ISwtScoutPart> m_parts;

  public BlockPartsJob(String name, SwtBusyHandler handler, List<ISwtScoutPart> parts) {
    super(name, handler);
    setSystem(true);
    m_parts = parts;
  }

  @Override
  protected SwtBusyHandler getBusyHandler() {
    return (SwtBusyHandler) super.getBusyHandler();
  }

  @Override
  protected void runBusy(IProgressMonitor monitor) {
    //nop
  }

  /**
   * Show a stop button in the active form parts header section and block all parts of the specific session
   * (environment).
   * <p>
   * Do not show a wait cursor anymore.
   */
  @Override
  protected void runBlocking(final IProgressMonitor monitor) {
    if (m_parts == null || m_parts.size() == 0) {
      return;
    }
    final ArrayList<SwtScoutPartBlockingDecorator> decoList = new ArrayList<SwtScoutPartBlockingDecorator>();
    final Display display = getBusyHandler().getDisplay();
    try {
      display.syncExec(new Runnable() {
        @Override
        public void run() {
          ISwtScoutPart activePart = m_parts.get(0);
          for (ISwtScoutPart p : m_parts) {
            if (p == null) {
              continue;
            }
            decoList.add(new SwtScoutPartBlockingDecorator(p, p == activePart));
          }
          for (SwtScoutPartBlockingDecorator deco : decoList) {
            try {
              deco.attach(monitor);
            }
            catch (Exception e1) {
              LOG.warn("attach", e1);
            }
          }
        }
      });
      //
      BlockPartsJob.super.runBlocking(monitor);
      //
    }
    finally {
      display.asyncExec(new Runnable() {
        @Override
        public void run() {
          for (SwtScoutPartBlockingDecorator deco : decoList) {
            try {
              deco.detach();
            }
            catch (Exception e1) {
              LOG.warn("detach", e1);
            }
          }
        }
      });
    }
  }

}
