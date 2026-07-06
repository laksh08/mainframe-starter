package com.enterprise.mq.starter.exception;

/**
 * Thrown when connection routing fails across queue managers.
 */
public class MqRoutingException extends MqException {

  public MqRoutingException(String message) {
    super(message);
  }

  public MqRoutingException(String message, Throwable cause) {
    super(message, cause);
  }
}
