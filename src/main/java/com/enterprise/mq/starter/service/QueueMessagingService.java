package com.enterprise.mq.starter.service;

import com.enterprise.mq.starter.connection.MqConnectionFactoryRegistry;
import com.enterprise.mq.starter.consumer.MqMessageConsumer;
import com.enterprise.mq.starter.exception.MqException;
import com.enterprise.mq.starter.exception.MqRoutingException;
import com.enterprise.mq.starter.exception.RetryableMqException;
import com.enterprise.mq.starter.metrics.MqMetricsRecorder;
import com.enterprise.mq.starter.model.FailureType;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.MqMessageHeaders;
import com.enterprise.mq.starter.model.MqReceiveResult;
import com.enterprise.mq.starter.model.RoutingContext;
import com.enterprise.mq.starter.producer.MqMessageProducer;
import com.enterprise.mq.starter.properties.MqProperties;
import com.enterprise.mq.starter.routing.ConnectionRoutingStrategy;
import com.enterprise.mq.starter.util.CorrelationIdHolder;
import com.enterprise.mq.starter.util.StructuredLogger;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.jms.Message;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;

/**
 * High-level messaging service for sending and receiving IBM MQ messages.
 */
public class QueueMessagingService {

  private static final Logger LOG = LoggerFactory.getLogger(QueueMessagingService.class);

  private final MqProperties properties;
  private final MqConnectionFactoryRegistry registry;
  private final ConnectionRoutingStrategy routingStrategy;
  private final MqMessageProducer producer;
  private final MqMessageConsumer consumer;
  private final RetryTemplate retryTemplate;
  private final MqMetricsRecorder metricsRecorder;
  private final MqFailureHandler failureHandler;

  public QueueMessagingService(
      MqProperties properties,
      MqConnectionFactoryRegistry registry,
      ConnectionRoutingStrategy routingStrategy,
      MqMessageProducer producer,
      MqMessageConsumer consumer,
      RetryTemplate retryTemplate,
      MqMetricsRecorder metricsRecorder,
      MqFailureHandler failureHandler) {
    this.properties = properties;
    this.registry = registry;
    this.routingStrategy = routingStrategy;
    this.producer = producer;
    this.consumer = consumer;
    this.retryTemplate = retryTemplate;
    this.metricsRecorder = metricsRecorder;
    this.failureHandler = failureHandler;
  }

  public void send(String queueName, Object payload) {
    send(queueName, payload, null, null);
  }

  public void send(String queueName, Object payload, MqMessageHeaders headers) {
    send(queueName, payload, headers, null);
  }

  public void send(String queueName, Object payload, String preferredQueueManager) {
    send(queueName, payload, null, preferredQueueManager);
  }

  public void send(
      String queueName,
      Object payload,
      MqMessageHeaders headers,
      String preferredQueueManager) {
    executeWithRetry(
        "send",
        queueName,
        preferredQueueManager,
        connection -> {
          MqMessageHeaders effectiveHeaders = enrichHeaders(headers);
          producer.send(connection, queueName, payload, effectiveHeaders);
          metricsRecorder.recordSendSuccess(connection.name(), queueName);
          logSuccess("mq.send.success", connection.name(), queueName);
          return null;
        });
  }

  public void convertAndSend(String queueName, Object payload) {
    send(queueName, payload);
  }

  public void sendWithHeaders(String queueName, Object payload, MqMessageHeaders headers) {
    send(queueName, payload, headers);
  }

  public MqReceiveResult<Object> receive(String queueName) {
    return receive(queueName, resolveTimeout(queueName), null);
  }

  public MqReceiveResult<Object> receive(String queueName, long timeoutMs) {
    return receive(queueName, timeoutMs, null);
  }

  public <T> MqReceiveResult<T> receive(String queueName, Class<T> targetType) {
    return receive(queueName, resolveTimeout(queueName), targetType);
  }

  public <T> MqReceiveResult<T> receive(String queueName, long timeoutMs, Class<T> targetType) {
    return executeWithRetry(
        "receive",
        queueName,
        null,
        connection -> {
          MqReceiveResult<T> result =
              targetType == null
                  ? castResult(consumer.receive(connection, queueName, timeoutMs))
                  : consumer.receive(connection, queueName, timeoutMs, targetType);
          if (result.payload() != null) {
            metricsRecorder.recordReceiveSuccess(connection.name(), queueName);
            logSuccess("mq.receive.success", connection.name(), queueName);
          }
          return result;
        });
  }

  public Message requestReply(String queueName, Object payload, String replyQueue, long timeoutMs) {
    return requestReply(queueName, payload, replyQueue, timeoutMs, null, null);
  }

  public Message requestReply(
      String queueName,
      Object payload,
      String replyQueue,
      long timeoutMs,
      MqMessageHeaders headers,
      String preferredQueueManager) {
    return executeWithRetry(
        "requestReply",
        queueName,
        preferredQueueManager,
        connection -> {
          Message reply =
              producer.requestReply(connection, queueName, payload, headers, replyQueue, timeoutMs);
          metricsRecorder.recordSendSuccess(connection.name(), queueName);
          logSuccess("mq.requestReply.success", connection.name(), queueName);
          return reply;
        });
  }

  private <T> T executeWithRetry(
      String operation,
      String queueName,
      String preferredQueueManager,
      MqOperation<T> mqOperation) {
    Context spanContext = metricsRecorder.startSpan(operation, preferredQueueManager, queueName);
    try (Scope scope = spanContext.makeCurrent()) {
      return retryTemplate.execute(
          context -> {
            ManagedConnection connection = selectConnection(queueName, preferredQueueManager);
            try {
              return mqOperation.execute(connection);
            } catch (RetryableMqException ex) {
              routingStrategy.markUnhealthy(connection);
              registry.markUnhealthy(connection.name());
              metricsRecorder.recordRetry(connection.name(), queueName, operation);
              failureHandler.handleFailure(
                  FailureType.PUBLISH,
                  connection.name(),
                  queueName,
                  connection.configuration().getChannel(),
                  ex.getMessage(),
                  ex);
              throw ex;
            } catch (MqException ex) {
              routingStrategy.markUnhealthy(connection);
              registry.markUnhealthy(connection.name());
              failureHandler.handleFailure(
                  mapFailureType(operation),
                  connection.name(),
                  queueName,
                  connection.configuration().getChannel(),
                  ex.getMessage(),
                  ex);
              throw ex;
            }
          });
    } finally {
      metricsRecorder.endSpan(spanContext);
    }
  }

  private ManagedConnection selectConnection(String queueName, String preferredQueueManager) {
    List<ManagedConnection> connections = registry.getAllConnections();
    if (connections.isEmpty()) {
      throw new MqRoutingException("No queue manager connections configured");
    }
    MqProperties.QueueManagerProperties queueManagerConfig =
        findQueueManagerForQueue(queueName, preferredQueueManager).orElse(null);
    RoutingContext context =
        new RoutingContext(queueName, preferredQueueManager, queueManagerConfig);
    return routingStrategy.select(connections, context);
  }

  private Optional<MqProperties.QueueManagerProperties> findQueueManagerForQueue(
      String queueName, String preferredQueueManager) {
    if (preferredQueueManager != null) {
      return registry.getConnection(preferredQueueManager).map(ManagedConnection::configuration);
    }
    return properties.getQueueManagers().stream()
        .filter(qm -> qm.getQueues().stream().anyMatch(q -> q.getName().equals(queueName)))
        .findFirst();
  }

  private long resolveTimeout(String queueName) {
    return properties.getQueueManagers().stream()
        .flatMap(qm -> qm.getQueues().stream())
        .filter(q -> q.getName().equals(queueName))
        .mapToLong(MqProperties.QueueProperties::getReceiveTimeoutMs)
        .findFirst()
        .orElse(5000L);
  }

  private MqMessageHeaders enrichHeaders(MqMessageHeaders headers) {
    if (headers != null && headers.correlationId() != null) {
      return headers;
    }
    String correlationId = CorrelationIdHolder.currentOrGenerate();
    if (headers == null) {
      return MqMessageHeaders.builder().correlationId(correlationId).build();
    }
    return MqMessageHeaders.builder()
        .correlationId(correlationId)
        .messageId(headers.messageId())
        .replyTo(headers.replyTo())
        .expiration(headers.expiration())
        .priority(headers.priority())
        .persistent(headers.persistent())
        .customHeaders(headers.customHeaders())
        .build();
  }

  private void logSuccess(String event, String queueManager, String queue) {
    if (properties.getObservability().isStructuredLoggingEnabled()) {
      Map<String, Object> fields = StructuredLogger.baseFields(queueManager, queue, event);
      StructuredLogger.info(LOG, event, fields);
    }
  }

  private FailureType mapFailureType(String operation) {
    return switch (operation) {
      case "receive" -> FailureType.RECEIVE;
      case "requestReply" -> FailureType.PUBLISH;
      default -> FailureType.PUBLISH;
    };
  }

  @SuppressWarnings("unchecked")
  private <T> MqReceiveResult<T> castResult(MqReceiveResult<Object> result) {
    return (MqReceiveResult<T>) result;
  }

  @FunctionalInterface
  private interface MqOperation<T> {
    T execute(ManagedConnection connection);
  }
}
