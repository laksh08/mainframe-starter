package com.enterprise.mq.starter.exception;

/**
 * Base exception for all IBM MQ starter errors.
 */
public class MqException extends RuntimeException {

  private final String queueManagerName;
  private final String queueName;

  public MqException(String message) {
    this(message, null, null, null);
  }

  public MqException(String message, Throwable cause) {
    this(message, cause, null, null);
  }

  public MqException(String message, Throwable cause, String queueManagerName, String queueName) {
    super(message, cause);
    this.queueManagerName = queueManagerName;
    this.queueName = queueName;
  }

  public String getQueueManagerName() {
    return queueManagerName;
  }

  public String getQueueName() {
    return queueName;
  }
}
