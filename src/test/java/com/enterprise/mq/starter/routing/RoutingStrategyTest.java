package com.enterprise.mq.starter.routing;

import com.enterprise.mq.starter.exception.MqRoutingException;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.RoutingContext;
import com.enterprise.mq.starter.properties.MqProperties;
import jakarta.jms.ConnectionFactory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutingStrategyTest {

  private MqProperties.QueueManagerProperties primaryConfig;
  private MqProperties.QueueManagerProperties secondaryConfig;
  private ManagedConnection primary;
  private ManagedConnection secondary;

  @BeforeEach
  void setUp() {
    primaryConfig = queueManager("primary", 10);
    secondaryConfig = queueManager("secondary", 5);
    ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
    primary = new ManagedConnection("primary", primaryConfig, connectionFactory, true);
    secondary = new ManagedConnection("secondary", secondaryConfig, connectionFactory, true);
  }

  @Test
  void roundRobinAlternatesConnections() {
    RoundRobinRoutingStrategy strategy = new RoundRobinRoutingStrategy();
    RoutingContext context = new RoutingContext("DEV.QUEUE.1", null, null);

    ManagedConnection first = strategy.select(List.of(primary, secondary), context);
    ManagedConnection second = strategy.select(List.of(primary, secondary), context);

    assertThat(first.name()).isNotEqualTo(second.name());
  }

  @Test
  void failoverSelectsFirstHealthyConnection() {
    FailoverRoutingStrategy strategy = new FailoverRoutingStrategy();
    RoutingContext context = new RoutingContext("DEV.QUEUE.1", null, null);

    ManagedConnection selected = strategy.select(List.of(primary, secondary), context);

    assertThat(selected.name()).isEqualTo("primary");
  }

  @Test
  void prioritySelectsHighestPriorityConnection() {
    PriorityRoutingStrategy strategy = new PriorityRoutingStrategy();
    RoutingContext context = new RoutingContext("DEV.QUEUE.1", null, null);

    ManagedConnection selected = strategy.select(List.of(secondary, primary), context);

    assertThat(selected.name()).isEqualTo("primary");
  }

  @Test
  void roundRobinThrowsWhenNoHealthyConnections() {
    RoundRobinRoutingStrategy strategy = new RoundRobinRoutingStrategy();
    ManagedConnection unhealthy =
        new ManagedConnection("primary", primaryConfig, Mockito.mock(ConnectionFactory.class), false);
    RoutingContext context = new RoutingContext("DEV.QUEUE.1", null, null);

    assertThatThrownBy(() -> strategy.select(List.of(unhealthy), context))
        .isInstanceOf(MqRoutingException.class);
  }

  private MqProperties.QueueManagerProperties queueManager(String name, int priority) {
    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName(name);
    config.setHost("localhost");
    config.setPort(1414);
    config.setQueueManager("QM1");
    config.setChannel("DEV.APP.SVRCONN");
    config.setPriority(priority);
    config.setEnabled(true);
    return config;
  }
}
