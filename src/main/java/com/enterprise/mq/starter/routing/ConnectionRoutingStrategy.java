package com.enterprise.mq.starter.routing;

import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.RoutingContext;
import java.util.List;

/**
 * Strategy for selecting an MQ connection from available endpoints.
 */
public interface ConnectionRoutingStrategy {

  ManagedConnection select(List<ManagedConnection> connections, RoutingContext context);

  void markUnhealthy(ManagedConnection connection);

  void markHealthy(ManagedConnection connection);
}
