package com.enterprise.mq.starter.connection;

import com.enterprise.mq.starter.exception.MqConnectionException;
import com.enterprise.mq.starter.properties.MqProperties;
import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import com.ibm.msg.client.jakarta.jms.JmsConstants;
import com.ibm.msg.client.jakarta.wmq.common.CommonConstants;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds IBM MQ connection factories with pooling and reconnect support.
 */
public class MqConnectionFactoryBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(MqConnectionFactoryBuilder.class);

  private final MqProperties properties;

  public MqConnectionFactoryBuilder(MqProperties properties) {
    this.properties = properties;
  }

  public ConnectionFactory build(MqProperties.QueueManagerProperties config) {
    try {
      MQConnectionFactory mqConnectionFactory = createMqConnectionFactory(config);
      MqProperties.ConnectionPoolProperties pool = properties.getPool();
      if (!pool.isEnabled()) {
        return mqConnectionFactory;
      }
      JmsPoolConnectionFactory pooled = new JmsPoolConnectionFactory();
      pooled.setConnectionFactory(mqConnectionFactory);
      pooled.setMaxConnections(pool.getMaxConnections());
      pooled.setMaxSessionsPerConnection(pool.getMaxSessionsPerConnection());
      pooled.setBlockIfSessionPoolIsFull(pool.isBlockIfFull());
      pooled.setBlockIfSessionPoolIsFullTimeout(pool.getConnectionTimeoutMs());
      pooled.setConnectionIdleTimeout((int) pool.getIdleTimeoutMs());
      if (pool.isReconnectOnException()) {
        pooled.setFaultTolerantConnections(true);
      }
      return pooled;
    } catch (JMSException ex) {
      throw new MqConnectionException(
          "Failed to create connection factory for queue manager: " + config.getName(), ex);
    }
  }

  private MQConnectionFactory createMqConnectionFactory(MqProperties.QueueManagerProperties config)
      throws JMSException {
    MQConnectionFactory factory = new MQConnectionFactory();
    factory.setHostName(config.getHost());
    factory.setPort(config.getPort());
    factory.setQueueManager(config.getQueueManager());
    factory.setChannel(config.getChannel());
    factory.setTransportType(CommonConstants.WMQ_CM_CLIENT);
    factory.setClientReconnectOptions(CommonConstants.WMQ_CLIENT_RECONNECT);
    factory.setIntProperty(CommonConstants.WMQ_CLIENT_RECONNECT_TIMEOUT, 300);

    if (config.getUsername() != null && !config.getUsername().isBlank()) {
      factory.setStringProperty(JmsConstants.USERID, config.getUsername());
    }
    if (config.getPassword() != null && !config.getPassword().isBlank()) {
      factory.setStringProperty(JmsConstants.PASSWORD, config.getPassword());
    }

    config.getProperties().forEach((key, value) -> {
      try {
        factory.setStringProperty(key, value);
      } catch (JMSException ex) {
        LOG.warn("Unable to set MQ property {} for queue manager {}", key, config.getName(), ex);
      }
    });

    return factory;
  }
}
