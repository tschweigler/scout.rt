package org.eclipse.scout.cloud.clientnotification.rabbitmq;

import java.io.IOException;

import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.server.scheduler.AbstractSchedulerJob;
import org.eclipse.scout.rt.server.scheduler.IScheduler;
import org.eclipse.scout.rt.server.scheduler.TickSignal;
import org.eclipse.scout.service.SERVICES;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

public class RabbitMQClientNotificationListenerJob extends AbstractSchedulerJob {

	private static final IScoutLogger LOG = ScoutLogManager.getLogger(RabbitMQClientNotificationListenerJob.class);
	private final static String groupId = "RabbitMQListener";
	private final static String jobId = "ClientNotifications";
	private boolean m_running;

	private static final String EXCHANGE_NAME = "clientNotification";
	private ConnectionFactory factory;
	private Connection connection;
	private Channel incomingChannel;
	private String queueName;

	public RabbitMQClientNotificationListenerJob() {
		super(groupId, jobId);
	}

	@Override
	protected boolean execAcceptTick(TickSignal signal, int second, int minute, int hour, int day, int week, int month, int year, int dayOfWeek,
			int dayOfMonthReverse, int dayOfYear, int secondOfDay) {
		if (!m_running) {
			return true;
		}
		return false;
	}

	@Override
	public void run(IScheduler scheduler, TickSignal signal) throws ProcessingException {
		synchronized (this) {
			if (m_running) { /* prevent the job from being started twice */
				LOG.warn("The Job " + getGroupId() + "." + getJobId() + " is already running, but should be started. Job was not started.");
				return;
			}
			m_running = true;
		}
		try {
			LOG.info("Started scheduled job: " + getGroupId() + "." + getJobId() + ", listening for new Notifications");

			factory = new ConnectionFactory();
			factory.setHost("localhost");

			try {
				connection = factory.newConnection();
				incomingChannel = connection.createChannel();

				incomingChannel.exchangeDeclare(EXCHANGE_NAME, "fanout");
				queueName = incomingChannel.queueDeclare().getQueue();
				incomingChannel.queueBind(queueName, EXCHANGE_NAME, "");

			    QueueingConsumer consumer = new QueueingConsumer(incomingChannel);
			    incomingChannel.basicConsume(queueName, true, consumer);

				while (true) {
				      QueueingConsumer.Delivery delivery = consumer.nextDelivery();
				      byte[] message = delivery.getBody();
				      SERVICES.getService(RabbitMQClientNotificationService.class).addMessageToLocalQueue(message);
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ShutdownSignalException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ConsumerCancelledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			LOG.info("Finished scheduled job: " + getGroupId() + "." + getJobId() + ", this should never happen");
		} finally {
			synchronized (this) {
				m_running = false;
			}
		}
	}
}
