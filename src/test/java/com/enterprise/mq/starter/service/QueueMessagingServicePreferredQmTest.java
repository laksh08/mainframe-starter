package com.enterprise.mq.starter.service;

import com.enterprise.mq.starter.connection.MqConnectionFactoryRegistry;
import com.enterprise.mq.starter.consumer.MqMessageConsumer;
import com.enterprise.mq.starter.metrics.MqMetricsRecorder;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.MqReceiveResult;
import com.enterprise.mq.starter.producer.MqMessageProducer;
import com.enterprise.mq.starter.properties.MqProperties;
import com.enterprise.mq.starter.routing.PriorityRoutingStrategy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import jakarta.jms.ConnectionFactory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.support.RetryTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueMessagingServicePreferredQmTest {

  @Test
  void sendUsesPreferredQueueManager() {
    MqProperties properties = new MqProperties();
    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setEnabled(true);
    config.setPriority(1);
    properties.setQueueManagers(List.of(config));

    ManagedConnection connection =
        new ManagedConnection("primary", config, Mockito.mock(ConnectionFactory.class), true);
    MqConnectionFactoryRegistry registry = Mockito.mock(MqConnectionFactoryRegistry.class);
    when(registry.getAllConnections()).thenReturn(List.of(connection));
    when(registry.getConnection("primary")).thenReturn(java.util.Optional.of(connection));

    MqMessageProducer producer = Mockito.mock(MqMessageProducer.class);
    MqMessageConsumer consumer = Mockito.mock(MqMessageConsumer.class);
    MqMetricsRecorder metrics =
        new MqMetricsRecorder(
            new SimpleMeterRegistry(), GlobalOpenTelemetry.getTracer("test"), false);
    MqFailureHandler failureHandler =
        new MqFailureHandler(
            properties, Mockito.mock(ApplicationEventPublisher.class), metrics, List.of());

    QueueMessagingService service =
        new QueueMessagingService(
            properties,
            registry,
            new PriorityRoutingStrategy(),
            producer,
            consumer,
            new RetryTemplate(),
            metrics,
            failureHandler);

    service.send("DEV.QUEUE.1", "payload", null, "primary");
    verify(producer).send(eq(connection), eq("DEV.QUEUE.1"), eq("payload"), any());
  }

  @Test
  void receiveUsesConfiguredTimeoutFromQueueDefinition() {
    MqProperties properties = new MqProperties();
    MqProperties.QueueProperties queue = new MqProperties.QueueProperties();
    queue.setName("DEV.QUEUE.1");
    queue.setReceiveTimeoutMs(2500L);
    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setEnabled(true);
    config.setQueues(List.of(queue));
    properties.setQueueManagers(List.of(config));

    ManagedConnection connection =
        new ManagedConnection("primary", config, Mockito.mock(ConnectionFactory.class), true);
    MqConnectionFactoryRegistry registry = Mockito.mock(MqConnectionFactoryRegistry.class);
    when(registry.getAllConnections()).thenReturn(List.of(connection));

    MqMessageConsumer consumer = Mockito.mock(MqMessageConsumer.class);
    when(consumer.receive(connection, "DEV.QUEUE.1", 2500L))
        .thenReturn(new MqReceiveResult<>("x", null, "primary", "DEV.QUEUE.1", null, null));

    MqMetricsRecorder metrics =
        new MqMetricsRecorder(
            new SimpleMeterRegistry(), GlobalOpenTelemetry.getTracer("test"), false);
    QueueMessagingService service =
        new QueueMessagingService(
            properties,
            registry,
            new PriorityRoutingStrategy(),
            Mockito.mock(MqMessageProducer.class),
            consumer,
            new RetryTemplate(),
            metrics,
            new MqFailureHandler(
                properties, Mockito.mock(ApplicationEventPublisher.class), metrics, List.of()));

    service.receive("DEV.QUEUE.1");
    verify(consumer).receive(connection, "DEV.QUEUE.1", 2500L);
  }
}
