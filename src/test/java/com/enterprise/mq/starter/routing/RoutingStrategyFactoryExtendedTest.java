package com.enterprise.mq.starter.routing;

import com.enterprise.mq.starter.exception.MqRoutingException;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.RoutingContext;
import com.enterprise.mq.starter.properties.MqProperties;
import jakarta.jms.ConnectionFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutingStrategyFactoryExtendedTest {

  @Test
  void throwsWhenCustomBeanHasWrongType() {
    MqProperties properties = new MqProperties();
    properties.getRouting().setCustomStrategyBean("wrongType");

    StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    beanFactory.addBean("wrongType", new Object());

    assertThatThrownBy(() -> new RoutingStrategyFactory(properties, beanFactory).create())
        .isInstanceOf(MqRoutingException.class);
  }

  @Test
  void failoverSkipsUnhealthyMarkedConnections() {
    FailoverRoutingStrategy strategy = new FailoverRoutingStrategy();
    ManagedConnection primary = managedConnection("primary");
    ManagedConnection secondary = managedConnection("secondary");
    strategy.markUnhealthy(primary);

    ManagedConnection selected =
        strategy.select(List.of(primary, secondary), new RoutingContext("Q1", null, null));

    org.assertj.core.api.Assertions.assertThat(selected.name()).isEqualTo("secondary");
  }

  private ManagedConnection managedConnection(String name) {
    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName(name);
    config.setEnabled(true);
    return new ManagedConnection(name, config, Mockito.mock(ConnectionFactory.class), true);
  }
}
