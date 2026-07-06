package com.enterprise.mq.starter.routing;

import com.enterprise.mq.starter.properties.MqProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingStrategyFactoryTest {

  @Test
  void createsConfiguredStrategy() {
    MqProperties properties = new MqProperties();
    properties.getRouting().setStrategy(MqProperties.RoutingStrategy.PRIORITY);

    RoutingStrategyFactory factory = new RoutingStrategyFactory(properties, new StaticListableBeanFactory());

    assertThat(factory.create()).isInstanceOf(PriorityRoutingStrategy.class);
  }

  @Test
  void resolvesCustomStrategyBean() {
    MqProperties properties = new MqProperties();
    properties.getRouting().setCustomStrategyBean("customStrategy");

    StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    RoundRobinRoutingStrategy customStrategy = new RoundRobinRoutingStrategy();
    beanFactory.addBean("customStrategy", customStrategy);

    RoutingStrategyFactory factory = new RoutingStrategyFactory(properties, beanFactory);

    assertThat(factory.create()).isSameAs(customStrategy);
  }
}
