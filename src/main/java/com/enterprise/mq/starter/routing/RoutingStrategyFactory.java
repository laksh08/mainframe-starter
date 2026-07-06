package com.enterprise.mq.starter.routing;

import com.enterprise.mq.starter.exception.MqRoutingException;
import com.enterprise.mq.starter.properties.MqProperties;
import org.springframework.beans.factory.BeanFactory;

/**
 * Factory for creating routing strategies from configuration.
 */
public class RoutingStrategyFactory {

  private final MqProperties properties;
  private final BeanFactory beanFactory;

  public RoutingStrategyFactory(MqProperties properties, BeanFactory beanFactory) {
    this.properties = properties;
    this.beanFactory = beanFactory;
  }

  public ConnectionRoutingStrategy create() {
    MqProperties.RoutingProperties routing = properties.getRouting();
    if (routing.getCustomStrategyBean() != null && !routing.getCustomStrategyBean().isBlank()) {
      return resolveCustomStrategy(routing.getCustomStrategyBean());
    }
    return switch (routing.getStrategy()) {
      case ROUND_ROBIN -> new RoundRobinRoutingStrategy();
      case FAILOVER -> new FailoverRoutingStrategy();
      case PRIORITY -> new PriorityRoutingStrategy();
    };
  }

  private ConnectionRoutingStrategy resolveCustomStrategy(String beanName) {
    if (!beanFactory.containsBean(beanName)) {
      throw new MqRoutingException("Custom routing strategy bean not found: " + beanName);
    }
    Object bean = beanFactory.getBean(beanName);
    if (!(bean instanceof ConnectionRoutingStrategy strategy)) {
      throw new MqRoutingException("Bean is not a ConnectionRoutingStrategy: " + beanName);
    }
    return strategy;
  }
}
