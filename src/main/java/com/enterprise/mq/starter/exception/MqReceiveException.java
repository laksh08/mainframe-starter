package com.enterprise.mq.starter.exception;

/**
 * Thrown when a message cannot be received from a queue.
 */
public class MqReceiveException extends MqException {

  public MqReceiveException(String message) {
    super(message);
  }

  public MqReceiveException(String message, Throwable cause) {
    super(message, cause);
  }

  public MqReceiveException(String message, Throwable cause, String queueManagerName, String queueName) {
    super(message, cause, queueManagerName, queueName);
  }
}
