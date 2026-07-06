package com.enterprise.mq.starter.listener;

import com.enterprise.mq.starter.model.FailureType;

/**
 * Extension point for custom failure notification logic.
 */
@FunctionalInterface
public interface MqFailureListener {

  void onFailure(
      FailureType failureType,
      String queueManagerName,
      String queueName,
      String channelName,
      String message,
      Throwable cause);
}
