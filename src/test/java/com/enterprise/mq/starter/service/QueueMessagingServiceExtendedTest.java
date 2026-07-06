package com.enterprise.mq.starter.service;

import com.enterprise.mq.starter.connection.MqConnectionFactoryRegistry;
import com.enterprise.mq.starter.consumer.MqMessageConsumer;
import com.enterprise.mq.starter.exception.MqRoutingException;
import com.enterprise.mq.starter.exception.RetryableMqException;
import com.enterprise.mq.starter.metrics.MqMetricsRecorder;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.MqReceiveResult;
import com.enterprise.mq.starter.producer.MqMessageProducer;
import com.enterprise.mq.starter.properties.MqProperties;
import com.enterprise.mq.starter.routing.FailoverRoutingStrategy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class QueueMessagingServiceExtendedTest {

  private QueueMessagingService service;
  private MqMessageProducer producer;
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
    MqMessageConsumer consumer = Mockito.mock(MqMessageConsumer.class);

    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    MqMetricsRecorder metricsRecorder =
        new MqMetricsRecorder(meterRegistry, GlobalOpenTelemetry.getTracer("test"), false);
    MqFailureHandler failureHandler =
        new MqFailureHandler(
            properties, Mockito.mock(ApplicationEventPublisher.class), metricsRecorder, List.of());

    service =
        new QueueMessagingService(
            properties,
            registry,
            new FailoverRoutingStrategy(),
            producer,
            consumer,
            new RetryTemplate(),
            metricsRecorder,
            failureHandler);
  }

  @Test
  void sendWithHeadersDelegatesToProducer() {
    service.sendWithHeaders("DEV.QUEUE.1", "payload", null);
    Mockito.verify(producer).send(eq(connection), eq("DEV.QUEUE.1"), eq("payload"), any());
  }

  @Test
  void requestReplyReturnsMessage() {
    Message reply = Mockito.mock(Message.class);
    when(producer.requestReply(
            eq(connection), eq("DEV.QUEUE.1"), eq("payload"), any(), eq("DEV.QUEUE.REPLY"), eq(1000L)))
        .thenReturn(reply);

    Message result = service.requestReply("DEV.QUEUE.1", "payload", "DEV.QUEUE.REPLY", 1000L);
    org.assertj.core.api.Assertions.assertThat(result).isSameAs(reply);
  }

  @Test
  void throwsRoutingExceptionWhenNoConnections() {
    MqProperties properties = new MqProperties();
    MqConnectionFactoryRegistry registry = Mockito.mock(MqConnectionFactoryRegistry.class);
    when(registry.getAllConnections()).thenReturn(List.of());

    QueueMessagingService emptyService =
        new QueueMessagingService(
            properties,
            registry,
            new FailoverRoutingStrategy(),
            producer,
            Mockito.mock(MqMessageConsumer.class),
            new RetryTemplate(),
            new MqMetricsRecorder(
                new SimpleMeterRegistry(), GlobalOpenTelemetry.getTracer("test"), false),
            new MqFailureHandler(
                properties,
                Mockito.mock(ApplicationEventPublisher.class),
                new MqMetricsRecorder(
                    new SimpleMeterRegistry(), GlobalOpenTelemetry.getTracer("test"), false),
                List.of()));

    assertThatThrownBy(() -> emptyService.send("DEV.QUEUE.1", "payload"))
        .isInstanceOf(MqRoutingException.class);
  }

}
