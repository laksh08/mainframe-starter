package com.enterprise.mq.starter.connection;

import com.enterprise.mq.starter.exception.MqConnectionException;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates MQ connection factory health.
 */
public class MqConnectionValidator {

  private static final Logger LOG = LoggerFactory.getLogger(MqConnectionValidator.class);

  public boolean validate(ConnectionFactory connectionFactory, String queueManagerName) {
    try (Connection connection = connectionFactory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
      connection.start();
      return session != null;
    } catch (JMSException ex) {
      LOG.debug("Connection validation failed for queue manager {}", queueManagerName, ex);
      return false;
    }
  }

  public void validateOrThrow(ConnectionFactory connectionFactory, String queueManagerName) {
    if (!validate(connectionFactory, queueManagerName)) {
      throw new MqConnectionException(
          "Connection validation failed for queue manager: " + queueManagerName);
    }
  }
}
