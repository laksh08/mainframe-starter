package com.enterprise.mq.starter.model;

import jakarta.jms.Message;
import java.util.Optional;

/**
 * Result of a receive operation.
 */
public record MqReceiveResult<T>(
    T payload,
    Message rawMessage,
    String queueManagerName,
    String queueName,
    String correlationId,
    String messageId) {

  public Optional<T> payloadOptional() {
    return Optional.ofNullable(payload);
  }
}
