package com.enterprise.mq.starter.routing;

import com.enterprise.mq.starter.exception.MqRoutingException;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.RoutingContext;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selects the highest-priority healthy connection.
 */
public class PriorityRoutingStrategy implements ConnectionRoutingStrategy {

  private final Set<String> unhealthy = ConcurrentHashMap.newKeySet();

  @Override
  public ManagedConnection select(List<ManagedConnection> connections, RoutingContext context) {
    return filterCandidates(connections, context).stream()
        .filter(c -> c.healthy() && !unhealthy.contains(c.name()))
        .max(Comparator.comparingInt(c -> c.configuration().getPriority()))
        .orElseThrow(
            () -> new MqRoutingException("No healthy queue manager connections available for priority routing"));
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
