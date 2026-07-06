package com.enterprise.mq.starter.service;

import com.enterprise.mq.starter.event.MqFailureEvent;
import com.enterprise.mq.starter.listener.MqFailureListener;
import com.enterprise.mq.starter.metrics.MqMetricsRecorder;
import com.enterprise.mq.starter.model.FailureType;
import com.enterprise.mq.starter.properties.MqProperties;
import com.enterprise.mq.starter.util.StructuredLogger;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Handles MQ failure notifications through logging, events, metrics, and listeners.
 */
public class MqFailureHandler {

  private static final Logger LOG = LoggerFactory.getLogger(MqFailureHandler.class);

  private final MqProperties properties;
  private final ApplicationEventPublisher eventPublisher;
  private final MqMetricsRecorder metricsRecorder;
  private final List<MqFailureListener> failureListeners;

  public MqFailureHandler(
      MqProperties properties,
      ApplicationEventPublisher eventPublisher,
      MqMetricsRecorder metricsRecorder,
      List<MqFailureListener> failureListeners) {
    this.properties = properties;
    this.eventPublisher = eventPublisher;
    this.metricsRecorder = metricsRecorder;
    this.failureListeners = failureListeners == null ? List.of() : List.copyOf(failureListeners);
  }

  public void handleFailure(
      FailureType failureType,
      String queueManagerName,
      String queueName,
      String channelName,
      String message,
      Throwable cause) {
    MqProperties.FailureProperties failure = properties.getFailure();

    if (failure.isLogStructuredErrors()) {
      Map<String, Object> fields =
          StructuredLogger.baseFields(queueManagerName, queueName, failureType.name());
      fields.put("channel", channelName);
      fields.put("detail", message);
      StructuredLogger.error(LOG, "mq.failure", fields, cause);
    }

    if (failure.isIncrementMetrics()) {
      metricsRecorder.recordFailure(failureType, queueManagerName, queueName);
      if (failureType == FailureType.CONNECTION || failureType == FailureType.QUEUE_MANAGER) {
        metricsRecorder.recordConnectionFailure(queueManagerName);
      }
    }

    if (failure.isPublishEvents()) {
      eventPublisher.publishEvent(
          new MqFailureEvent(
              this,
              failureType,
              queueManagerName,
              queueName,
              channelName,
              message,
              cause));
    }

    for (MqFailureListener listener : failureListeners) {
      listener.onFailure(failureType, queueManagerName, queueName, channelName, message, cause);
    }
  }
}
