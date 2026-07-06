package com.enterprise.mq.starter.exception;

/**
 * Thrown when a message cannot be published to a queue.
 */
public class MqPublishException extends MqException {

  public MqPublishException(String message) {
    super(message);
  }

  public MqPublishException(String message, Throwable cause) {
    super(message, cause);
  }

  public MqPublishException(String message, Throwable cause, String queueManagerName, String queueName) {
    super(message, cause, queueManagerName, queueName);
  }
}
