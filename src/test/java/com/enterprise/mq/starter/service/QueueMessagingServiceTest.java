package com.enterprise.mq.starter.service;

import com.enterprise.mq.starter.connection.MqConnectionFactoryRegistry;
import com.enterprise.mq.starter.consumer.MqMessageConsumer;
import com.enterprise.mq.starter.event.MqFailureEvent;
import com.enterprise.mq.starter.metrics.MqMetricsRecorder;
import com.enterprise.mq.starter.model.FailureType;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.MqReceiveResult;
import com.enterprise.mq.starter.producer.MqMessageProducer;
import com.enterprise.mq.starter.properties.MqProperties;
import com.enterprise.mq.starter.routing.RoundRobinRoutingStrategy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import jakarta.jms.ConnectionFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueMessagingServiceTest {

  private QueueMessagingService service;
  private MqMessageProducer producer;
  private MqMessageConsumer consumer;
  private ManagedConnection connection;

  @BeforeEach
  void setUp() {
    MqProperties properties = new MqProperties();
    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setHost("localhost");
    config.setPort(1414);
    config.setQueueManager("QM1");
    config.setChannel("DEV.APP.SVRCONN");
    config.setEnabled(true);
    properties.setQueueManagers(List.of(config));

    connection = new ManagedConnection("primary", config, Mockito.mock(ConnectionFactory.class), true);

    MqConnectionFactoryRegistry registry = Mockito.mock(MqConnectionFactoryRegistry.class);
    when(registry.getAllConnections()).thenReturn(List.of(connection));

    producer = Mockito.mock(MqMessageProducer.class);
    consumer = Mockito.mock(MqMessageConsumer.class);

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    MqMetricsRecorder metricsRecorder =
        new MqMetricsRecorder(meterRegistry, GlobalOpenTelemetry.getTracer("test"), false);

    ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
    MqFailureHandler failureHandler =
        new MqFailureHandler(properties, eventPublisher, metricsRecorder, List.of());

    RetryTemplate retryTemplate = new RetryTemplate();

    service =
        new QueueMessagingService(
            properties,
            registry,
            new RoundRobinRoutingStrategy(),
            producer,
            consumer,
            retryTemplate,
            metricsRecorder,
            failureHandler);
  }

  @Test
  void sendDelegatesToProducer() {
    service.send("DEV.QUEUE.1", "payload");
    verify(producer).send(eq(connection), eq("DEV.QUEUE.1"), eq("payload"), any());
  }

  @Test
  void receiveReturnsPayload() {
    MqReceiveResult<Object> result =
        new MqReceiveResult<>("payload", null, "primary", "DEV.QUEUE.1", "corr", "msg");
    when(consumer.receive(connection, "DEV.QUEUE.1", 5000L)).thenReturn(result);

    MqReceiveResult<Object> received = service.receive("DEV.QUEUE.1");

    assertThat(received.payload()).isEqualTo("payload");
  }

  @Test
  void convertAndSendUsesProducer() {
    service.convertAndSend("DEV.QUEUE.1", Map.of("key", "value"));
    verify(producer).send(eq(connection), eq("DEV.QUEUE.1"), any(), any());
  }
}
