package org.eclipse.scout.cloud.clientnotification.rabbitmq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.scout.commons.EventListenerList;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.commons.serialization.IObjectSerializer;
import org.eclipse.scout.commons.serialization.SerializationUtility;
import org.eclipse.scout.rt.server.IServerSession;
import org.eclipse.scout.rt.server.ThreadContext;
import org.eclipse.scout.rt.server.services.common.clientnotification.ClientNotificationQueueEvent;
import org.eclipse.scout.rt.server.services.common.clientnotification.IClientNotificationFilter;
import org.eclipse.scout.rt.server.services.common.clientnotification.IClientNotificationQueueListener;
import org.eclipse.scout.rt.server.services.common.clientnotification.IClientNotificationService;
import org.eclipse.scout.rt.server.services.common.clientnotification.QueueElement;
import org.eclipse.scout.rt.shared.services.common.clientnotification.IClientNotification;
import org.eclipse.scout.service.AbstractService;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQClientNotificationService extends AbstractService implements IClientNotificationService {

	private static final IScoutLogger LOG = ScoutLogManager.getLogger(RabbitMQClientNotificationService.class);

	private static final String CLIENT_NOTIFICATION_QUEUE = "clientNotification";
	private static final String CLIENT_NOTIFICATION_CONSUMPTION_QUEUE = "clientNotificationConsumption";
	private ConnectionFactory factory;
	private Connection connection;
	private Channel outgoingClientNotificationChannel;
	private Channel outgoingClientNotificationConsumptionChannel;

	private IObjectSerializer objectSerializer;
	private LinkedList<QueueElement> m_queue;
	private Object m_queueLock = new Object();
	private EventListenerList m_listenerList = new EventListenerList();

	public RabbitMQClientNotificationService() {
		m_queue = new LinkedList<QueueElement>();

		factory = new ConnectionFactory();
		factory.setHost("localhost");

		try {
			connection = factory.newConnection();
			outgoingClientNotificationChannel = connection.createChannel();
			outgoingClientNotificationConsumptionChannel = connection.createChannel();

			outgoingClientNotificationChannel.exchangeDeclare(CLIENT_NOTIFICATION_QUEUE, "fanout");
			outgoingClientNotificationConsumptionChannel.exchangeDeclare(CLIENT_NOTIFICATION_CONSUMPTION_QUEUE, "fanout");

			objectSerializer = SerializationUtility.createObjectSerializer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// start the scheduler

	}

	@Override
	public IClientNotification[] getNextNotifications(long blockingTimeout) {
		long endTime = System.currentTimeMillis() + blockingTimeout;
		ArrayList<IClientNotification> list = new ArrayList<IClientNotification>();
		ArrayList<QueueElement> consumed = new ArrayList<QueueElement>();
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
									} else {
										it.remove();
									}
									consumed.add(e);
								}
							}
						} else {
							it.remove();
						}
					}
				}
				long dt = endTime - System.currentTimeMillis();
				if (list.size() > 0 || dt <= 0) {
					break;
				} else {
					try {
						m_queueLock.wait(dt);
					} catch (InterruptedException ie) {
					}
				}
			}
		}
		LOG.info(list.size() + " neue Notifications gefunden");

		setMessagesConsumed(consumed);

		return list.toArray(new IClientNotification[list.size()]);
	}

	@Override
	public void putNotification(IClientNotification notification, IClientNotificationFilter filter) {
		// TODO Auto-generated method stub

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
	    	ArrayList<QueueElement> replaced = new ArrayList<QueueElement>();
	        for (Iterator<QueueElement> it = m_queue.iterator(); it.hasNext();) {
	          QueueElement e = it.next();
	          if (!e.getFilter().isActive()) {
	            it.remove();
	          }
	          else if (e.getClientNotification() == notification) {
	            it.remove();
	            replaced.add(e);
	          }
	          else if (e.getClientNotification().getClass() == notification.getClass() && filter.equals(e.getFilter()) && notification.coalesce(e.getClientNotification())) {
	            it.remove();
	            replaced.add(e);
	          }
	        }
	    }
		QueueElement qe = new QueueElement(notification, filter);

		try {
			outgoingClientNotificationChannel.basicPublish(CLIENT_NOTIFICATION_QUEUE, "", null, objectSerializer.serialize(qe));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fireEvent(notification, filter);
	}

	@Override
	public void addClientNotificationQueueListener(IClientNotificationQueueListener listener) {
		m_listenerList.add(IClientNotificationQueueListener.class, listener);

	}

	@Override
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

	private void setMessagesConsumed(List<QueueElement> queueElements) {
		for (QueueElement queueElement : queueElements) {
			notifyMessageConsumed(queueElement);
		}
	}

	public void notifyMessageConsumed(QueueElement queueElement) {
		try {
			outgoingClientNotificationConsumptionChannel
					.basicPublish(CLIENT_NOTIFICATION_CONSUMPTION_QUEUE, "", null, objectSerializer.serialize(queueElement));
		} catch (IOException e) {
			LOG.warn("Unable to notify the Queue about a consumed message");
			e.printStackTrace();
		}
	}

	public void addMessageToLocalQueue(byte[] message) {
		synchronized (m_queueLock) {
			m_queue.add(getQueueElement(message));
		}
	}

	public void setMessageConsumed(byte[] message) {
		synchronized (m_queueLock) {
			QueueElement queueElement = getQueueElement(message);

			for (QueueElement e : m_queue) {
				if (queueElement.getElementId().equals(e.getElementId())) {
					if (e.getFilter().isMulticast()) {
						e.addConsumedBy(queueElement.getConsumedBy());
					} else {
						m_queue.remove(e);
					}
				}
			}
		}
	}

	private QueueElement getQueueElement(byte[] m) {
		try {
			return objectSerializer.deserialize(m, QueueElement.class);
		} catch (ClassNotFoundException e) {
			LOG.warn("Unable to read Message: " + new String(m));
			e.printStackTrace();
		} catch (IOException e) {
			LOG.warn("Unable to read Message: " + new String(m));
		}
		return null;
	}

}
