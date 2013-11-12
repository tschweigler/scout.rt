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
package org.eclipse.scout.rt.server.services.common.clientnotification;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.services.common.clientnotification.IClientNotificationFilter;
import org.eclipse.scout.rt.shared.services.common.clientnotification.IClientNotification;

/**
 *
 */
public class QueueElement implements Serializable {
	private static final long serialVersionUID = -8513131031858145786L;
	private IClientNotification m_notification;
	private IClientNotificationFilter m_filter;
	private transient Object m_consumedBySessionsLock;
	private Set<String> m_consumedBySessions;
	private String elementId;

	public QueueElement(IClientNotification notification, IClientNotificationFilter filter) {
		m_notification = notification;
		m_filter = filter;
		m_consumedBySessionsLock = new Object();
		elementId = UUID.randomUUID().toString();
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		m_consumedBySessionsLock = new Object();
	}

	public IClientNotification getClientNotification() {
		return m_notification;
	}

	public IClientNotificationFilter getFilter() {
		return m_filter;
	}

	/**
	 * @return true if this notifcation is already consumed by the session specified
	 */
	public boolean isConsumedBy(IServerSession session) {
		// fast check
		if (session == null) {
			return false;
		}
		if (m_consumedBySessions == null) {
			return false;
		}
		//
		synchronized (m_consumedBySessionsLock) {
			if (m_consumedBySessions != null) {
				return m_consumedBySessions.contains(session.getSessionId());
			} else {
				return false;
			}
		}
	}

	/**
	 * keeps in mind that this notifcation was consumed by the session specified
	 */
	public void setConsumedBy(IServerSession session) {
		if (session != null) {
			synchronized (m_consumedBySessionsLock) {
				if (m_consumedBySessions == null) {
					m_consumedBySessions = new HashSet<String>();
				}
				m_consumedBySessions.add(session.getSessionId());
			}
		}
	}

	/**
	 *
	 * @return Map
	 */
	public Set<String> getConsumedBy() {
		return m_consumedBySessions;
	}

	/**
	 * Adds a Set of SessionIds to the consumed sessionIds
	 * @param consumedBySessions Set of SessionIds
	 */
	public void addConsumedBy(Set<String> consumedBySessions) {
		m_consumedBySessions.addAll(consumedBySessions);
	}

	/**
	 * Returns the Id of the QueueElement
	 * @return the elementId
	 */
	public String getElementId() {
		return elementId;
	}
}
