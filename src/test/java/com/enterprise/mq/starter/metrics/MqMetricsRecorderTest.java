package com.enterprise.mq.starter.metrics;

import com.enterprise.mq.starter.model.FailureType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqMetricsRecorderTest {

  @Test
  void recordsSendReceiveRetryAndFailureMetrics() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    MqMetricsRecorder recorder =
        new MqMetricsRecorder(registry, GlobalOpenTelemetry.getTracer("test"), true);

    recorder.recordSendSuccess("primary", "DEV.QUEUE.1");
    recorder.recordReceiveSuccess("primary", "DEV.QUEUE.1");
    recorder.recordRetry("primary", "DEV.QUEUE.1", "send");
    recorder.recordFailure(FailureType.CONNECTION, "primary", "DEV.QUEUE.1");
    recorder.recordConnectionFailure("primary");

    assertThat(registry.find("mq.send.success").counter().count()).isEqualTo(1.0);
    assertThat(registry.find("mq.receive.success").counter().count()).isEqualTo(1.0);
    assertThat(registry.find("mq.retry.count").counter().count()).isEqualTo(1.0);
    assertThat(registry.find("mq.failure.count").counter().count()).isEqualTo(1.0);
    assertThat(recorder.getQueueManagerStatus("primary")).isZero();
  }
}
