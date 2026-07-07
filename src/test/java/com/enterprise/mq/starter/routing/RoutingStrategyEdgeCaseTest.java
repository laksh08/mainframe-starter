package com.enterprise.mq.starter.routing;

import com.enterprise.mq.starter.exception.MqRoutingException;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.RoutingContext;
import com.enterprise.mq.starter.properties.MqProperties;
import jakarta.jms.ConnectionFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutingStrategyEdgeCaseTest {

  @Test
  void failoverThrowsWhenAllUnhealthy() {
    FailoverRoutingStrategy strategy = new FailoverRoutingStrategy();
    ManagedConnection unhealthy = managedConnection("primary", false);
    strategy.markUnhealthy(unhealthy);

    assertThatThrownBy(
            () ->
                strategy.select(
                    List.of(unhealthy), new RoutingContext("DEV.QUEUE.1", null, null)))
        .isInstanceOf(MqRoutingException.class);
  }

  @Test
  void priorityMarksHealthyAndUnhealthy() {
    PriorityRoutingStrategy strategy = new PriorityRoutingStrategy();
    ManagedConnection connection = managedConnection("primary", true);
    strategy.markUnhealthy(connection);
    strategy.markHealthy(connection);
    assertThatThrownBy(
            () ->
                strategy.select(
                    List.of(connection.withHealth(false)),
                    new RoutingContext("DEV.QUEUE.1", null, null)))
        .isInstanceOf(MqRoutingException.class);
  }

  @Test
  void roundRobinUsesPreferredQueueManager() {
    RoundRobinRoutingStrategy strategy = new RoundRobinRoutingStrategy();
    ManagedConnection primary = managedConnection("primary", true);
    ManagedConnection secondary = managedConnection("secondary", true);

    ManagedConnection selected =
        strategy.select(
            List.of(primary, secondary),
            new RoutingContext("DEV.QUEUE.1", "secondary", null));

    org.assertj.core.api.Assertions.assertThat(selected.name()).isEqualTo("secondary");
  }

  @Test
  void factoryThrowsForInvalidCustomBean() {
    MqProperties properties = new MqProperties();
    properties.getRouting().setCustomStrategyBean("missing");
    RoutingStrategyFactory factory = new RoutingStrategyFactory(properties, new org.springframework.beans.factory.support.StaticListableBeanFactory());

    assertThatThrownBy(factory::create).isInstanceOf(MqRoutingException.class);
  }

  private ManagedConnection managedConnection(String name, boolean healthy) {
    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName(name);
    config.setEnabled(true);
    config.setPriority(1);
    return new ManagedConnection(name, config, Mockito.mock(ConnectionFactory.class), healthy);
  }
}
