package com.enterprise.mq.starter.consumer;

import com.enterprise.mq.starter.converter.MqMessageConverter;
import com.enterprise.mq.starter.exception.MqReceiveException;
import com.enterprise.mq.starter.exception.RetryableMqException;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.MqReceiveResult;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Low-level JMS message consumer.
 */
public class MqMessageConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(MqMessageConsumer.class);

  private final MqMessageConverter messageConverter;

  public MqMessageConsumer(MqMessageConverter messageConverter) {
    this.messageConverter = messageConverter;
  }

  public MqReceiveResult<Object> receive(
      ManagedConnection connection, String queueName, long timeoutMs) {
    try (Connection jmsConnection = connection.connectionFactory().createConnection();
        Session session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
      jmsConnection.start();
      Destination destination = session.createQueue(queueName);
      try (MessageConsumer consumer = session.createConsumer(destination)) {
        Message message = consumer.receive(timeoutMs);
        if (message == null) {
          return new MqReceiveResult<>(
              null, null, connection.name(), queueName, null, null);
        }
        Object payload = messageConverter.fromMessage(message);
        return new MqReceiveResult<>(
            payload,
            message,
            connection.name(),
            queueName,
            message.getJMSCorrelationID(),
            message.getJMSMessageID());
      }
    } catch (JMSException ex) {
      LOG.debug("Receive failed for queue {} on {}", queueName, connection.name(), ex);
      throw mapReceiveException(ex, connection.name(), queueName);
    }
  }

  public <T> MqReceiveResult<T> receive(
      ManagedConnection connection, String queueName, long timeoutMs, Class<T> targetType) {
    MqReceiveResult<Object> result = receive(connection, queueName, timeoutMs);
    if (result.payload() == null || result.rawMessage() == null) {
      return new MqReceiveResult<>(
          null, result.rawMessage(), result.queueManagerName(), result.queueName(),
          result.correlationId(), result.messageId());
    }
    try {
      T converted = messageConverter.fromMessage(result.rawMessage(), targetType);
      return new MqReceiveResult<>(
          converted,
          result.rawMessage(),
          result.queueManagerName(),
          result.queueName(),
          result.correlationId(),
          result.messageId());
    } catch (JMSException ex) {
      throw mapReceiveException(ex, connection.name(), queueName);
    }
  }

  private RuntimeException mapReceiveException(JMSException ex, String queueManager, String queue) {
    if (isRetryable(ex)) {
      return new RetryableMqException("Retryable receive failure", ex, queueManager, queue);
    }
    return new MqReceiveException("Receive failure", ex, queueManager, queue);
  }

  private boolean isRetryable(JMSException ex) {
    return ex.getErrorCode() != null
        && (ex.getErrorCode().contains("2033") || ex.getErrorCode().contains("2195"));
  }
}
