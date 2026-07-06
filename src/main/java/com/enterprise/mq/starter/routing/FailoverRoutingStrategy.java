package com.enterprise.mq.starter.routing;

import com.enterprise.mq.starter.exception.MqRoutingException;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.RoutingContext;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selects the first healthy connection, failing over on errors.
 */
public class FailoverRoutingStrategy implements ConnectionRoutingStrategy {

  private final Set<String> unhealthy = ConcurrentHashMap.newKeySet();

  @Override
  public ManagedConnection select(List<ManagedConnection> connections, RoutingContext context) {
    List<ManagedConnection> candidates = filterCandidates(connections, context);
    for (ManagedConnection connection : candidates) {
      if (connection.healthy() && !unhealthy.contains(connection.name())) {
        return connection;
      }
    }
    throw new MqRoutingException("No healthy queue manager connections available for failover routing");
  }

  @Override
  public void markUnhealthy(ManagedConnection connection) {
    unhealthy.add(connection.name());
  }

  @Override
  public void markHealthy(ManagedConnection connection) {
    unhealthy.remove(connection.name());
  }

  private List<ManagedConnection> filterCandidates(
      List<ManagedConnection> connections, RoutingContext context) {
    if (context.preferredQueueManager() != null) {
      return connections.stream()
          .filter(c -> c.name().equals(context.preferredQueueManager()))
          .toList();
    }
    return connections.stream().filter(c -> c.configuration().isEnabled()).toList();
  }
}
