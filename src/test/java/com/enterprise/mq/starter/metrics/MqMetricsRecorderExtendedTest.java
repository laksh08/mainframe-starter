package com.enterprise.mq.starter.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqMetricsRecorderExtendedTest {

  @Test
  void openTelemetrySpanLifecycleWhenEnabled() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MqMetricsRecorder recorder =
        new MqMetricsRecorder(registry, GlobalOpenTelemetry.getTracer("test"), true);

    var context = recorder.startSpan("send", "primary", "DEV.QUEUE.1");
    recorder.endSpan(context);
    recorder.endSpan(io.opentelemetry.context.Context.current());

    assertThat(registry.find("mq.send.success").counter()).isNull();
  }
}
