package com.enterprise.mq.starter.connection;

import com.enterprise.mq.starter.exception.MqConnectionException;
import com.enterprise.mq.starter.properties.MqProperties;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class MqConnectionRegistryExtendedTest {

  @Test
  void refreshHealthUpdatesConnectionState() {
    MqProperties properties = buildProperties();
    MqConnectionValidator validator = Mockito.mock(MqConnectionValidator.class);
    when(validator.validate(Mockito.any(), Mockito.eq("primary"))).thenReturn(false, true);

    MqConnectionFactoryRegistry registry =
        new MqConnectionFactoryRegistry(
            properties, new MqConnectionFactoryBuilder(properties), validator);

    assertThat(registry.getAllConnections().getFirst().healthy()).isFalse();
    registry.refreshHealth();
    assertThat(registry.getAllConnections().getFirst().healthy()).isTrue();
  }

  @Test
  void markHealthyAndUnhealthyUpdatesSnapshot() {
    MqProperties properties = buildProperties();
    MqConnectionValidator validator = Mockito.mock(MqConnectionValidator.class);
    when(validator.validate(Mockito.any(), Mockito.eq("primary"))).thenReturn(true);

    MqConnectionFactoryRegistry registry =
        new MqConnectionFactoryRegistry(
            properties, new MqConnectionFactoryBuilder(properties), validator);

    registry.markUnhealthy("primary");
    assertThat(registry.getConnection("primary").orElseThrow().healthy()).isFalse();
    registry.markHealthy("primary");
    assertThat(registry.getConnection("primary").orElseThrow().healthy()).isTrue();
    assertThat(registry.snapshot()).containsKey("primary");
  }

  @Test
  void validatorThrowsWhenConnectionInvalid() throws Exception {
    ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
    when(connectionFactory.createConnection()).thenThrow(new jakarta.jms.JMSException("down"));

    MqConnectionValidator validator = new MqConnectionValidator();
    assertThatThrownBy(() -> validator.validateOrThrow(connectionFactory, "primary"))
        .isInstanceOf(MqConnectionException.class);
  }

  @Test
  void builderAppliesCredentialsAndCustomProperties() {
    MqProperties properties = new MqProperties();
    properties.getPool().setEnabled(false);

    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setHost("localhost");
    config.setPort(1414);
    config.setQueueManager("QM1");
    config.setChannel("DEV.APP.SVRCONN");
    config.setUsername("app");
    config.setPassword("password");
    config.getProperties().put("customProperty", "value");

    assertThat(new MqConnectionFactoryBuilder(properties).build(config)).isNotNull();
  }

  @Test
  void validatorSucceedsWithOpenSession() throws Exception {
    ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
    Connection connection = Mockito.mock(Connection.class);
    Session session = Mockito.mock(Session.class);
    when(connectionFactory.createConnection()).thenReturn(connection);
    when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);

    new MqConnectionValidator().validateOrThrow(connectionFactory, "primary");
  }

  private MqProperties buildProperties() {
    MqProperties properties = new MqProperties();
    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setHost("localhost");
    config.setPort(1414);
    config.setQueueManager("QM1");
    config.setChannel("DEV.APP.SVRCONN");
    config.setEnabled(true);
    properties.setQueueManagers(java.util.List.of(config));
    properties.getPool().setEnabled(false);
    return properties;
  }
}
