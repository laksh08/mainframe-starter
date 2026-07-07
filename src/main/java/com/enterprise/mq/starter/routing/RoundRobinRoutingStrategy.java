package com.enterprise.mq.starter.routing;

import com.enterprise.mq.starter.exception.MqRoutingException;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.RoutingContext;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Distributes messages across healthy connections in round-robin order.
 */
public class RoundRobinRoutingStrategy implements ConnectionRoutingStrategy {

  private final AtomicInteger counter = new AtomicInteger();
  private final Set<String> unhealthy = ConcurrentHashMap.newKeySet();

  @Override
  public ManagedConnection select(List<ManagedConnection> connections, RoutingContext context) {
    List<ManagedConnection> candidates = filterCandidates(connections, context);
    if (candidates.isEmpty()) {
      throw new MqRoutingException("No healthy queue manager connections available for routing");
    }
    int index = Math.floorMod(counter.getAndIncrement(), candidates.size());
    return candidates.get(index);
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
          .filter(c -> c.healthy() && !unhealthy.contains(c.name()))
          .toList();
    }
    return connections.stream()
        .filter(c -> c.configuration().isEnabled())
        .filter(c -> c.healthy() && !unhealthy.contains(c.name()))
        .toList();
  }
}
