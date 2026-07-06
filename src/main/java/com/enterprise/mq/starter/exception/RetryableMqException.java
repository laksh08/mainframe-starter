package com.enterprise.mq.starter.exception;

/**
 * Indicates an MQ operation failure that may succeed on retry.
 */
public class RetryableMqException extends MqException {

  public RetryableMqException(String message) {
    super(message);
  }

  public RetryableMqException(String message, Throwable cause) {
    super(message, cause);
  }

  public RetryableMqException(String message, Throwable cause, String queueManagerName, String queueName) {
    super(message, cause, queueManagerName, queueName);
  }
}
