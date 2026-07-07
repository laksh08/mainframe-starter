package com.enterprise.mq.starter.config;

import com.enterprise.mq.starter.connection.MqConnectionFactoryBuilder;
import com.enterprise.mq.starter.connection.MqConnectionFactoryRegistry;
import com.enterprise.mq.starter.connection.MqConnectionValidator;
import com.enterprise.mq.starter.converter.MqMessageConverter;
import com.enterprise.mq.starter.consumer.MqMessageConsumer;
import com.enterprise.mq.starter.health.MqHealthIndicator;
import com.enterprise.mq.starter.listener.MqFailureListener;
import com.enterprise.mq.starter.metrics.MqMetricsRecorder;
import com.enterprise.mq.starter.producer.MqMessageProducer;
import com.enterprise.mq.starter.properties.MqProperties;
import com.enterprise.mq.starter.retry.MqRetryTemplateFactory;
import com.enterprise.mq.starter.routing.ConnectionRoutingStrategy;
import com.enterprise.mq.starter.routing.RoutingStrategyFactory;
import com.enterprise.mq.starter.service.MqFailureHandler;
import com.enterprise.mq.starter.service.QueueMessagingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import java.util.List;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;

/**
 * Core IBM MQ Spring configuration.
 */
@Configuration
@EnableRetry
@EnableConfigurationProperties(MqProperties.class)
@ConditionalOnProperty(prefix = "mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MqAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public MqConnectionFactoryBuilder mqConnectionFactoryBuilder(MqProperties properties) {
    return new MqConnectionFactoryBuilder(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  public MqConnectionValidator mqConnectionValidator() {
    return new MqConnectionValidator();
  }

  @Bean
  @ConditionalOnMissingBean
  public MqConnectionFactoryRegistry mqConnectionFactoryRegistry(
      MqProperties properties,
      MqConnectionFactoryBuilder builder,
      MqConnectionValidator validator) {
    return new MqConnectionFactoryRegistry(properties, builder, validator);
  }

  @Bean
  @ConditionalOnMissingBean
  public ConnectionRoutingStrategy connectionRoutingStrategy(
      MqProperties properties, BeanFactory beanFactory) {
    return new RoutingStrategyFactory(properties, beanFactory).create();
  }

  @Bean
  @ConditionalOnMissingBean
  public MqMessageConverter mqMessageConverter(ObjectProvider<ObjectMapper> objectMapperProvider) {
    ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
    return new MqMessageConverter(objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public MqMessageProducer mqMessageProducer(MqMessageConverter messageConverter) {
    return new MqMessageProducer(messageConverter);
  }

  @Bean
  @ConditionalOnMissingBean
  public MqMessageConsumer mqMessageConsumer(MqMessageConverter messageConverter) {
    return new MqMessageConsumer(messageConverter);
  }

  @Bean
  @ConditionalOnMissingBean
  public MqRetryTemplateFactory mqRetryTemplateFactory(MqProperties properties) {
    return new MqRetryTemplateFactory(properties);
  }

  @Bean
  @ConditionalOnMissingBean
  public RetryTemplate mqRetryTemplate(MqRetryTemplateFactory factory) {
    return factory.createRetryTemplate();
  }

  @Bean
  @ConditionalOnMissingBean
  public Tracer mqTracer(MqProperties properties) {
    return GlobalOpenTelemetry.getTracer("com.enterprise.mq.starter");
  }

  @Bean
  @ConditionalOnMissingBean
  public MqMetricsRecorder mqMetricsRecorder(
      MeterRegistry meterRegistry, Tracer tracer, MqProperties properties) {
    return new MqMetricsRecorder(
        meterRegistry, tracer, properties.getObservability().isOpenTelemetryEnabled());
  }

  @Bean
  @ConditionalOnMissingBean
  public MqFailureHandler mqFailureHandler(
      MqProperties properties,
      ApplicationEventPublisher eventPublisher,
      MqMetricsRecorder metricsRecorder,
      List<MqFailureListener> failureListeners) {
    return new MqFailureHandler(properties, eventPublisher, metricsRecorder, failureListeners);
  }

  @Bean
  @ConditionalOnMissingBean
  public QueueMessagingService queueMessagingService(
      MqProperties properties,
      MqConnectionFactoryRegistry registry,
      ConnectionRoutingStrategy routingStrategy,
      MqMessageProducer producer,
      MqMessageConsumer consumer,
      RetryTemplate mqRetryTemplate,
      MqMetricsRecorder metricsRecorder,
      MqFailureHandler failureHandler) {
    return new QueueMessagingService(
        properties,
        registry,
        routingStrategy,
        producer,
        consumer,
        mqRetryTemplate,
        metricsRecorder,
        failureHandler);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(prefix = "mq.health", name = "enabled", havingValue = "true", matchIfMissing = true)
  public MqHealthIndicator mqHealthIndicator(
      MqProperties properties,
      MqConnectionFactoryRegistry registry,
      MqConnectionValidator validator) {
    return new MqHealthIndicator(properties, registry, validator);
  }
}
