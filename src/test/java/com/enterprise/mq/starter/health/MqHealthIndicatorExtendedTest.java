package com.enterprise.mq.starter.health;

import com.enterprise.mq.starter.connection.MqConnectionFactoryRegistry;
import com.enterprise.mq.starter.connection.MqConnectionValidator;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.properties.MqProperties;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.health.contributor.Health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MqHealthIndicatorExtendedTest {

  @Test
  void validatesQueuesWhenEnabled() throws Exception {
    MqProperties properties = new MqProperties();
    properties.getHealth().setValidateQueues(true);

    MqProperties.QueueProperties queue = new MqProperties.QueueProperties();
    queue.setName("DEV.QUEUE.1");
    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setHost("localhost");
    config.setPort(1414);
    config.setQueueManager("QM1");
    config.setChannel("DEV.APP.SVRCONN");
    config.setQueues(List.of(queue));

    ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
    Connection connection = Mockito.mock(Connection.class);
    Session session = Mockito.mock(Session.class);
    Queue mqQueue = Mockito.mock(Queue.class);
    QueueBrowser browser = Mockito.mock(QueueBrowser.class);

    when(connectionFactory.createConnection()).thenReturn(connection);
    when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);
    when(session.createQueue("DEV.QUEUE.1")).thenReturn(mqQueue);
    when(session.createBrowser(mqQueue)).thenReturn(browser);
    when(browser.getEnumeration()).thenReturn(java.util.Collections.emptyEnumeration());

    ManagedConnection managed = new ManagedConnection("primary", config, connectionFactory, true);
    MqConnectionFactoryRegistry registry = Mockito.mock(MqConnectionFactoryRegistry.class);
    when(registry.getAllConnections()).thenReturn(List.of(managed));

    MqConnectionValidator validator = Mockito.mock(MqConnectionValidator.class);
    when(validator.validate(connectionFactory, "primary")).thenReturn(true);

    MqHealthIndicator indicator = new MqHealthIndicator(properties, registry, validator);
    Health health = indicator.health();

    assertThat(health.getStatus().getCode()).isEqualTo("UP");
  }

  @Test
  void reportsDegradedWhenQueueValidationFails() throws Exception {
    MqProperties properties = new MqProperties();
    properties.getHealth().setValidateQueues(true);

    MqProperties.QueueProperties queue = new MqProperties.QueueProperties();
    queue.setName("DEV.QUEUE.1");
    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setHost("localhost");
    config.setPort(1414);
    config.setQueueManager("QM1");
    config.setChannel("DEV.APP.SVRCONN");
    config.setQueues(List.of(queue));

    ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
    when(connectionFactory.createConnection()).thenThrow(new JMSException("down"));

    ManagedConnection managed = new ManagedConnection("primary", config, connectionFactory, true);
    MqConnectionFactoryRegistry registry = Mockito.mock(MqConnectionFactoryRegistry.class);
    when(registry.getAllConnections()).thenReturn(List.of(managed));

    MqConnectionValidator validator = Mockito.mock(MqConnectionValidator.class);
    when(validator.validate(connectionFactory, "primary")).thenReturn(true);

    MqHealthIndicator indicator = new MqHealthIndicator(properties, registry, validator);
    Health health = indicator.health();

    assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
  }
}
