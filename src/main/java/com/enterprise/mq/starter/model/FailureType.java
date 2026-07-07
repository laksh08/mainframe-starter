package com.enterprise.mq.starter.model;

/**
 * Types of MQ failure events for notification and metrics.
 */
public enum FailureType {
  CONNECTION,
  QUEUE_MANAGER,
  QUEUE,
  CHANNEL,
  ROUTING,
  PUBLISH,
  RECEIVE
}
