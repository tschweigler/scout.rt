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
package org.eclipse.scout.rt.server.services.common.clientnotification.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.ThreadContext;
import org.eclipse.scout.rt.server.services.common.clientnotification.ClientNotificationQueueEvent;
import org.eclipse.scout.rt.server.services.common.clientnotification.IClientNotificationFilter;
import org.eclipse.scout.rt.server.services.common.clientnotification.IClientNotificationQueueListener;
import org.eclipse.scout.rt.server.services.common.clientnotification.QueueElement;
import org.eclipse.scout.rt.shared.services.common.clientnotification.IClientNotification;

/**
 * element type used in CTIStateCache
 */
public class ClientNotificationQueue {
  private static final IScoutLogger LOG = ScoutLogManager.getLogger(ClientNotificationQueue.class);
  private EventListenerList m_listenerList = new EventListenerList();

  private LinkedList<QueueElement> m_queue;
  private Object m_queueLock = new Object();

  public ClientNotificationQueue() {
    m_queue = new LinkedList<QueueElement>();
  }

  public void putNotification(IClientNotification notification, IClientNotificationFilter filter) {
    if (notification == null) {
      throw new IllegalArgumentException("notification must not be null");
    }
    if (filter == null) {
      throw new IllegalArgumentException("filter must not be null");
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("put " + notification + " for " + filter);
    }
    LOG.info("put " + notification + " for " + filter);

    synchronized (m_queueLock) {
      for (Iterator<QueueElement> it = m_queue.iterator(); it.hasNext();) {
        QueueElement e = it.next();
        if (!e.getFilter().isActive()) {
          it.remove();
        }
        else if (e.getClientNotification() == notification) {
          it.remove();
        }
        else if (e.getClientNotification().getClass() == notification.getClass() && filter.equals(e.getFilter()) && notification.coalesce(e.getClientNotification())) {
          it.remove();
        }
      }
      m_queue.add(new QueueElement(notification, filter));
      m_queueLock.notifyAll();
    }
    fireEvent(notification, filter);
  }

  public IClientNotification[] getNextNotifications(long blockingTimeout) {
    long endTime = System.currentTimeMillis() + blockingTimeout;
    ArrayList<IClientNotification> list = new ArrayList<IClientNotification>();
    synchronized (m_queueLock) {
      LOG.info("Suche nach neuen Notifications");
      while (true) {
        if (!m_queue.isEmpty()) {
          for (Iterator<QueueElement> it = m_queue.iterator(); it.hasNext();) {
            QueueElement e = it.next();
            if (e.getFilter().isActive()) {
              IServerSession serverSession = ThreadContext.getServerSession();
              if (!e.isConsumedBy(serverSession)) {
                if (e.getFilter().accept()) {
                  list.add(e.getClientNotification());
                  if (e.getFilter().isMulticast()) {
                    e.setConsumedBy(serverSession);
                  }
                  else {
                    it.remove();
                  }
                }
              }
            }
            else {
              it.remove();
            }
          }
        }
        long dt = endTime - System.currentTimeMillis();
        if (list.size() > 0 || dt <= 0) {
          break;
        }
        else {
          try {
            m_queueLock.wait(dt);
          }
          catch (InterruptedException ie) {
          }
        }
      }
    }
    LOG.info(list.size() + " neue Notifications gefunden");

    return list.toArray(new IClientNotification[list.size()]);
  }

  /**
   * Model Observer
   */
  public void addClientNotificationQueueListener(IClientNotificationQueueListener listener) {
    m_listenerList.add(IClientNotificationQueueListener.class, listener);
  }

  public void removeClientNotificationQueueListener(IClientNotificationQueueListener listener) {
    m_listenerList.remove(IClientNotificationQueueListener.class, listener);
  }

  private void fireEvent(IClientNotification notification, IClientNotificationFilter filter) {
    IClientNotificationQueueListener[] listeners = m_listenerList.getListeners(IClientNotificationQueueListener.class);
    if (listeners != null && listeners.length > 0) {
      for (int i = 0; i < listeners.length; i++) {
        (listeners[i]).queueChanged(new ClientNotificationQueueEvent(notification, filter, ClientNotificationQueueEvent.TYPE_NOTIFICATION_ADDED));
      }
    }
  }
}
