package com.enterprise.mq.starter.metrics;

import com.enterprise.mq.starter.model.FailureType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records MQ metrics and OpenTelemetry spans.
 */
public class MqMetricsRecorder {

  private final MeterRegistry meterRegistry;
  private final Tracer tracer;
  private final boolean openTelemetryEnabled;
  private final ConcurrentMap<String, AtomicInteger> queueManagerStatus = new ConcurrentHashMap<>();

  public MqMetricsRecorder(
      MeterRegistry meterRegistry, Tracer tracer, boolean openTelemetryEnabled) {
    this.meterRegistry = meterRegistry;
    this.tracer = tracer;
    this.openTelemetryEnabled = openTelemetryEnabled;
  }

  public void recordSendSuccess(String queueManager, String queue) {
    counter("mq.send.success", queueManager, queue).increment();
    updateQueueManagerStatus(queueManager, 1);
  }

  public void recordReceiveSuccess(String queueManager, String queue) {
    counter("mq.receive.success", queueManager, queue).increment();
    updateQueueManagerStatus(queueManager, 1);
  }

  public void recordRetry(String queueManager, String queue, String operation) {
    meterRegistry.counter(
        "mq.retry.count",
        Tags.of("queueManager", queueManager, "queue", queue, "operation", operation))
        .increment();
  }

  public void recordFailure(FailureType failureType, String queueManager, String queue) {
    meterRegistry.counter(
        "mq.failure.count",
        Tags.of("type", failureType.name(), "queueManager", safe(queueManager), "queue", safe(queue)))
        .increment();
    updateQueueManagerStatus(queueManager, 0);
  }

  public void recordConnectionFailure(String queueManager) {
    meterRegistry.counter("mq.connection.failure", "queueManager", safe(queueManager)).increment();
    updateQueueManagerStatus(queueManager, 0);
  }

  public Context startSpan(String operation, String queueManager, String queue) {
    if (!openTelemetryEnabled) {
      return Context.current();
    }
    Span span =
        tracer.spanBuilder("mq." + operation)
            .setAttribute("mq.queueManager", safe(queueManager))
            .setAttribute("mq.queue", safe(queue))
            .startSpan();
    return Context.current().with(span);
  }

  public void endSpan(Context context) {
    if (!openTelemetryEnabled || context == Context.current()) {
      return;
    }
    Span span = Span.fromContext(context);
    span.end();
  }

  public int getQueueManagerStatus(String queueManager) {
    return queueManagerStatus
        .computeIfAbsent(queueManager, key -> new AtomicInteger(1))
        .get();
  }

  private Counter counter(String name, String queueManager, String queue) {
    return meterRegistry.counter(
        name, Tags.of("queueManager", safe(queueManager), "queue", safe(queue)));
  }

  private void updateQueueManagerStatus(String queueManager, int status) {
    queueManagerStatus
        .computeIfAbsent(queueManager, key -> new AtomicInteger(status))
        .set(status);
  }

  private String safe(String value) {
    return value == null ? "unknown" : value;
  }
}
