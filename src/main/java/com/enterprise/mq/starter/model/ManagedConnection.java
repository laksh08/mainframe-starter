package com.enterprise.mq.starter.model;

import com.enterprise.mq.starter.properties.MqProperties;

/**
 * Represents a managed MQ connection endpoint.
 */
public record ManagedConnection(
    String name,
    MqProperties.QueueManagerProperties configuration,
    jakarta.jms.ConnectionFactory connectionFactory,
    boolean healthy) {

  public ManagedConnection withHealth(boolean newHealth) {
    return new ManagedConnection(name, configuration, connectionFactory, newHealth);
  }
}
