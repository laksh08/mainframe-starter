package com.enterprise.mq.starter.exception;

/**
 * Thrown when a connection to IBM MQ cannot be established or maintained.
 */
public class MqConnectionException extends MqException {

  public MqConnectionException(String message) {
    super(message);
  }

  public MqConnectionException(String message, Throwable cause) {
    super(message, cause);
  }

  public MqConnectionException(String message, Throwable cause, String queueManagerName) {
    super(message, cause, queueManagerName, null);
  }
}
