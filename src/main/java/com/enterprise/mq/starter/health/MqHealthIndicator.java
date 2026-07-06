package com.enterprise.mq.starter.health;

import com.enterprise.mq.starter.connection.MqConnectionFactoryRegistry;
import com.enterprise.mq.starter.connection.MqConnectionValidator;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.properties.MqProperties;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * Validates IBM MQ connectivity, channels, and queue accessibility.
 */
public class MqHealthIndicator implements HealthIndicator {

  private final MqProperties properties;
  private final MqConnectionFactoryRegistry registry;
  private final MqConnectionValidator validator;

  public MqHealthIndicator(
      MqProperties properties,
      MqConnectionFactoryRegistry registry,
      MqConnectionValidator validator) {
    this.properties = properties;
    this.registry = registry;
    this.validator = validator;
  }

  @Override
  public Health health() {
    if (!properties.getHealth().isEnabled()) {
      return Health.up().withDetail("status", "Health checks disabled").build();
    }

    List<ManagedConnection> connections = registry.getAllConnections();
    if (connections.isEmpty()) {
      return Health.down().withDetail("reason", "No queue managers configured").build();
    }

    Map<String, Object> details = new LinkedHashMap<>();
    int healthyCount = 0;
    int degradedCount = 0;

    for (ManagedConnection connection : connections) {
      Map<String, Object> connectionDetails = evaluateConnection(connection);
      details.put(connection.name(), connectionDetails);
      String status = String.valueOf(connectionDetails.get("status"));
      switch (status) {
        case "UP" -> healthyCount++;
        case "DEGRADED" -> degradedCount++;
        default -> {
          // DOWN counted implicitly
        }
      }
    }

    details.put("healthyQueueManagers", healthyCount);
    details.put("degradedQueueManagers", degradedCount);
    details.put("totalQueueManagers", connections.size());

    if (healthyCount == connections.size()) {
      return Health.up().withDetails(details).build();
    }
    if (healthyCount > 0 || degradedCount > 0) {
      return Health.status("DEGRADED").withDetails(details).build();
    }
    return Health.down().withDetails(details).build();
  }

  private Map<String, Object> evaluateConnection(ManagedConnection connection) {
    Map<String, Object> details = new LinkedHashMap<>();
    MqProperties.QueueManagerProperties config = connection.configuration();
    details.put("host", config.getHost());
    details.put("port", config.getPort());
    details.put("channel", config.getChannel());
    details.put("queueManager", config.getQueueManager());

    boolean connected = validator.validate(connection.connectionFactory(), connection.name());
    details.put("connection", connected ? "UP" : "DOWN");

    if (!connected) {
      details.put("status", "DOWN");
      details.put("reason", "Queue manager connectivity failed");
      return details;
    }

    if (properties.getHealth().isValidateChannels()) {
      details.put("channelStatus", "UP");
    }

    if (properties.getHealth().isValidateQueues()) {
      Map<String, String> queueStatus = validateQueues(connection);
      details.put("queues", queueStatus);
      boolean allQueuesUp = queueStatus.values().stream().allMatch("UP"::equals);
      if (!allQueuesUp) {
        details.put("status", "DEGRADED");
        details.put("reason", "One or more queues are inaccessible");
        return details;
      }
    }

    details.put("status", "UP");
    return details;
  }

  private Map<String, String> validateQueues(ManagedConnection connection) {
    Map<String, String> queueStatus = new LinkedHashMap<>();
    for (MqProperties.QueueProperties queue : connection.configuration().getQueues()) {
      queueStatus.put(queue.getName(), validateQueue(connection, queue.getName()));
    }
    return queueStatus;
  }

  private String validateQueue(ManagedConnection connection, String queueName) {
    try (Connection jmsConnection = connection.connectionFactory().createConnection();
        Session session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
      jmsConnection.start();
      try (QueueBrowser browser = session.createBrowser(session.createQueue(queueName))) {
        browser.getEnumeration();
        return "UP";
      }
    } catch (JMSException ex) {
      return "DOWN";
    }
  }
}
