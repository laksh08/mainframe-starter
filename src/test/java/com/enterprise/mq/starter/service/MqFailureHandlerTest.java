package com.enterprise.mq.starter.service;

import com.enterprise.mq.starter.event.MqFailureEvent;
import com.enterprise.mq.starter.listener.MqFailureListener;
import com.enterprise.mq.starter.metrics.MqMetricsRecorder;
import com.enterprise.mq.starter.model.FailureType;
import com.enterprise.mq.starter.properties.MqProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class MqFailureHandlerTest {

  @Test
  void publishesEventAndInvokesListener() {
    MqProperties properties = new MqProperties();
    ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    MqMetricsRecorder metricsRecorder =
        new MqMetricsRecorder(meterRegistry, GlobalOpenTelemetry.getTracer("test"), false);
    MqFailureListener listener = Mockito.mock(MqFailureListener.class);

    MqFailureHandler handler =
        new MqFailureHandler(properties, publisher, metricsRecorder, List.of(listener));

    handler.handleFailure(
        FailureType.CONNECTION,
        "primary",
        "DEV.QUEUE.1",
        "DEV.APP.SVRCONN",
        "connection failed",
        new RuntimeException("boom"));

    ArgumentCaptor<MqFailureEvent> captor = ArgumentCaptor.forClass(MqFailureEvent.class);
    verify(publisher).publishEvent(captor.capture());
    assertThat(captor.getValue().getFailureType()).isEqualTo(FailureType.CONNECTION);
    verify(listener)
        .onFailure(
            FailureType.CONNECTION,
            "primary",
            "DEV.QUEUE.1",
            "DEV.APP.SVRCONN",
            "connection failed",
            captor.getValue().getCause());
    assertThat(meterRegistry.find("mq.failure.count").counter()).isNotNull();
  }
}
