package com.enterprise.mq.starter.health;

import com.enterprise.mq.starter.connection.MqConnectionFactoryRegistry;
import com.enterprise.mq.starter.connection.MqConnectionValidator;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.properties.MqProperties;
import jakarta.jms.ConnectionFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.health.contributor.Health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MqHealthIndicatorTest {

  @Test
  void reportsUpWhenAllConnectionsHealthy() {
    MqProperties properties = new MqProperties();
    properties.getHealth().setEnabled(true);
    properties.getHealth().setValidateQueues(false);

    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setHost("localhost");
    config.setPort(1414);
    config.setQueueManager("QM1");
    config.setChannel("DEV.APP.SVRCONN");

    ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
    ManagedConnection connection = new ManagedConnection("primary", config, connectionFactory, true);

    MqConnectionFactoryRegistry registry = Mockito.mock(MqConnectionFactoryRegistry.class);
    when(registry.getAllConnections()).thenReturn(List.of(connection));

    MqConnectionValidator validator = Mockito.mock(MqConnectionValidator.class);
    when(validator.validate(connectionFactory, "primary")).thenReturn(true);

    MqHealthIndicator indicator = new MqHealthIndicator(properties, registry, validator);
    Health health = indicator.health();

    assertThat(health.getStatus().getCode()).isEqualTo("UP");
    assertThat(health.getDetails()).containsKey("primary");
  }

  @Test
  void reportsDownWhenNoQueueManagersConfigured() {
    MqProperties properties = new MqProperties();
    MqConnectionFactoryRegistry registry = Mockito.mock(MqConnectionFactoryRegistry.class);
    when(registry.getAllConnections()).thenReturn(List.of());

    MqHealthIndicator indicator =
        new MqHealthIndicator(properties, registry, new MqConnectionValidator());

    Health health = indicator.health();
    assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
  }

  @Test
  void reportsDegradedWhenPartialFailure() {
    MqProperties properties = new MqProperties();
    properties.getHealth().setValidateQueues(false);

    ManagedConnection healthy = managedConnection("healthy", true);
    ManagedConnection degraded = managedConnection("degraded", true);

    MqConnectionFactoryRegistry registry = Mockito.mock(MqConnectionFactoryRegistry.class);
    when(registry.getAllConnections()).thenReturn(List.of(healthy, degraded));

    MqConnectionValidator validator = Mockito.mock(MqConnectionValidator.class);
    when(validator.validate(healthy.connectionFactory(), "healthy")).thenReturn(true);
    when(validator.validate(degraded.connectionFactory(), "degraded")).thenReturn(false);

    MqHealthIndicator indicator = new MqHealthIndicator(properties, registry, validator);
    Health health = indicator.health();

    assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
  }

  private ManagedConnection managedConnection(String name, boolean healthy) {
    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName(name);
    config.setHost("localhost");
    config.setPort(1414);
    config.setQueueManager("QM1");
    config.setChannel("DEV.APP.SVRCONN");
    return new ManagedConnection(name, config, Mockito.mock(ConnectionFactory.class), healthy);
  }
}
