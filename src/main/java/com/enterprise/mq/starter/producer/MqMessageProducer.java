package com.enterprise.mq.starter.producer;

import com.enterprise.mq.starter.converter.MqMessageConverter;
import com.enterprise.mq.starter.exception.MqPublishException;
import com.enterprise.mq.starter.exception.RetryableMqException;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.MqMessageHeaders;
import com.enterprise.mq.starter.util.MqHeaderUtils;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Low-level JMS message producer.
 */
public class MqMessageProducer {

  private static final Logger LOG = LoggerFactory.getLogger(MqMessageProducer.class);

  private final MqMessageConverter messageConverter;

  public MqMessageProducer(MqMessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

  public void send(
      ManagedConnection connection,
      String queueName,
      Object payload,
      MqMessageHeaders headers) {
    try (Connection jmsConnection = connection.connectionFactory().createConnection();
        Session session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
      jmsConnection.start();
      Destination destination = session.createQueue(queueName);
      Message message = messageConverter.toMessage(session, payload);
      MqHeaderUtils.applyHeaders(session, message, headers);
      try (MessageProducer producer = session.createProducer(destination)) {
        producer.send(message);
      }
    } catch (JMSException ex) {
      LOG.debug("Publish failed for queue {} on {}", queueName, connection.name(), ex);
      throw mapPublishException(ex, connection.name(), queueName);
    }
  }

  public Message requestReply(
      ManagedConnection connection,
      String queueName,
      Object payload,
      MqMessageHeaders headers,
      String replyQueue,
      long timeoutMs) {
    try (Connection jmsConnection = connection.connectionFactory().createConnection();
        Session session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
      jmsConnection.start();
      Destination destination = session.createQueue(queueName);
      Destination replyDestination = session.createQueue(replyQueue);
      Message message = messageConverter.toMessage(session, payload);
      MqMessageHeaders effectiveHeaders =
          headers == null
              ? MqMessageHeaders.builder().replyTo(replyQueue).build()
              : MqMessageHeaders.builder()
                  .correlationId(headers.correlationId())
                  .messageId(headers.messageId())
                  .replyTo(replyQueue)
                  .expiration(headers.expiration())
                  .priority(headers.priority())
                  .persistent(headers.persistent())
                  .customHeaders(headers.customHeaders())
                  .build();
      MqHeaderUtils.applyHeaders(session, message, effectiveHeaders);
      message.setJMSReplyTo(replyDestination);

      try (MessageProducer producer = session.createProducer(destination);
          jakarta.jms.MessageConsumer consumer =
              session.createConsumer(replyDestination, "JMSCorrelationID='" + message.getJMSMessageID() + "'")) {
        producer.send(message);
        Message reply = consumer.receive(timeoutMs);
        if (reply == null) {
          throw new MqPublishException(
              "Request-reply timed out after " + timeoutMs + "ms",
              null,
              connection.name(),
              queueName);
        }
        return reply;
      }
    } catch (JMSException ex) {
      throw mapPublishException(ex, connection.name(), queueName);
    }
  }

  private RuntimeException mapPublishException(JMSException ex, String queueManager, String queue) {
    if (isRetryable(ex)) {
      return new RetryableMqException("Retryable publish failure", ex, queueManager, queue);
    }
    return new MqPublishException("Publish failure", ex, queueManager, queue);
  }

  private boolean isRetryable(JMSException ex) {
    return ex.getErrorCode() != null
        && (ex.getErrorCode().contains("2033") || ex.getErrorCode().contains("2195"));
  }
}
