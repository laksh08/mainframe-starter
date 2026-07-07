package com.enterprise.mq.starter.connection;

import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.properties.MqProperties;
import jakarta.jms.ConnectionFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry of all configured MQ connection factories.
 */
public class MqConnectionFactoryRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(MqConnectionFactoryRegistry.class);

  private final Map<String, ManagedConnection> connections = new ConcurrentHashMap<>();
  private final MqConnectionFactoryBuilder builder;
  private final MqConnectionValidator validator;

  public MqConnectionFactoryRegistry(
      MqProperties properties,
      MqConnectionFactoryBuilder builder,
      MqConnectionValidator validator) {
    this.builder = builder;
    this.validator = validator;
    initialize(properties);
  }

  public List<ManagedConnection> getAllConnections() {
    return List.copyOf(connections.values());
  }

  public Optional<ManagedConnection> getConnection(String name) {
    return Optional.ofNullable(connections.get(name));
  }

  public void refreshHealth() {
    connections.replaceAll(
        (name, connection) -> {
          boolean healthy = validator.validate(connection.connectionFactory(), name);
          return connection.withHealth(healthy);
        });
  }

  public void markHealthy(String name) {
    connections.computeIfPresent(
        name,
        (key, connection) -> connection.withHealth(true));
  }

  public void markUnhealthy(String name) {
    connections.computeIfPresent(
        name,
        (key, connection) -> connection.withHealth(false));
  }

  private void initialize(MqProperties properties) {
    List<ManagedConnection> initialized = new ArrayList<>();
    for (MqProperties.QueueManagerProperties config : properties.getQueueManagers()) {
      if (!config.isEnabled()) {
        LOG.info("Skipping disabled queue manager: {}", config.getName());
        continue;
      }
      ConnectionFactory connectionFactory = builder.build(config);
      boolean healthy = validator.validate(connectionFactory, config.getName());
      ManagedConnection managed =
          new ManagedConnection(config.getName(), config, connectionFactory, healthy);
      connections.put(config.getName(), managed);
      initialized.add(managed);
      LOG.info(
          "Registered queue manager '{}' at {}:{} [healthy={}]",
          config.getName(),
          config.getHost(),
          config.getPort(),
          healthy);
    }
    if (initialized.isEmpty()) {
      LOG.warn("No enabled queue managers were registered");
    }
  }

  public Map<String, ManagedConnection> snapshot() {
    return Collections.unmodifiableMap(connections);
  }
}
