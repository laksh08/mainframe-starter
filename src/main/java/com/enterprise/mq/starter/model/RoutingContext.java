package com.enterprise.mq.starter.model;

import com.enterprise.mq.starter.properties.MqProperties;

/**
 * Context for routing a message to a queue manager connection.
 */
public record RoutingContext(
    String queueName,
    String preferredQueueManager,
    MqProperties.QueueManagerProperties queueManager) {}
