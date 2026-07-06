package com.enterprise.mq.starter.event;

import com.enterprise.mq.starter.model.FailureType;
import org.springframework.context.ApplicationEvent;

/**
 * Spring application event published when an MQ failure occurs.
 */
public class MqFailureEvent extends ApplicationEvent {

  private final FailureType failureType;
  private final String queueManagerName;
  private final String queueName;
  private final String channelName;
  private final String message;
  private final Throwable cause;

  public MqFailureEvent(
      Object source,
      FailureType failureType,
      String queueManagerName,
      String queueName,
      String channelName,
      String message,
      Throwable cause) {
    super(source);
    this.failureType = failureType;
    this.queueManagerName = queueManagerName;
    this.queueName = queueName;
    this.channelName = channelName;
    this.message = message;
    this.cause = cause;
  }

  public FailureType getFailureType() {
    return failureType;
  }

  public String getQueueManagerName() {
    return queueManagerName;
  }

  public String getQueueName() {
    return queueName;
  }

  public String getChannelName() {
    return channelName;
  }

  public String getMessage() {
    return message;
  }

  public Throwable getCause() {
    return cause;
  }
}
