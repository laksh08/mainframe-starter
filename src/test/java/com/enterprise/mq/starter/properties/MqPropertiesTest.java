package com.enterprise.mq.starter.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqPropertiesTest {

  @Test
  void supportsAllConfigurationSections() {
    MqProperties properties = new MqProperties();
    properties.setEnabled(false);

    MqProperties.QueueManagerProperties queueManager = new MqProperties.QueueManagerProperties();
    queueManager.setName("primary");
    queueManager.setHost("host");
    queueManager.setPort(1414);
    queueManager.setQueueManager("QM1");
    queueManager.setChannel("CHANNEL");
    queueManager.setUsername("user");
    queueManager.setPassword("pass");
    queueManager.setPriority(3);
    queueManager.setEnabled(true);
    queueManager.getProperties().put("key", "value");

    MqProperties.QueueProperties queue = new MqProperties.QueueProperties();
    queue.setName("DEV.QUEUE.1");
    queue.setAlias("orders");
    queue.setRequestReplyEnabled(true);
    queue.setReplyQueue("DEV.QUEUE.2");
    queue.setReceiveTimeoutMs(2000L);
    queueManager.setQueues(java.util.List.of(queue));
    properties.setQueueManagers(java.util.List.of(queueManager));

    properties.getRouting().setStrategy(MqProperties.RoutingStrategy.PRIORITY);
    properties.getRouting().setCustomStrategyBean("custom");

    properties.getRetry().setEnabled(true);
    properties.getRetry().setMaxAttempts(5);
    properties.getRetry().setInitialDelayMs(100L);
    properties.getRetry().setMaxDelayMs(2000L);
    properties.getRetry().setMultiplier(3.0);
    properties.getRetry().setExponentialBackoff(false);

    properties.getPool().setEnabled(true);
    properties.getPool().setMaxConnections(20);
    properties.getPool().setMaxSessionsPerConnection(100);
    properties.getPool().setBlockIfFull(false);
    properties.getPool().setConnectionTimeoutMs(1000L);
    properties.getPool().setIdleTimeoutMs(2000L);
    properties.getPool().setValidateOnBorrow(false);
    properties.getPool().setReconnectOnException(false);

    properties.getHealth().setEnabled(true);
    properties.getHealth().setValidateQueues(true);
    properties.getHealth().setValidateChannels(true);
    properties.getHealth().setTimeoutMs(3000L);

    properties.getFailure().setPublishEvents(false);
    properties.getFailure().setLogStructuredErrors(false);
    properties.getFailure().setIncrementMetrics(false);

    properties.getObservability().setMetricsEnabled(false);
    properties.getObservability().setStructuredLoggingEnabled(false);
    properties.getObservability().setCorrelationIdEnabled(false);
    properties.getObservability().setOpenTelemetryEnabled(false);

    assertThat(properties.isEnabled()).isFalse();
    assertThat(properties.getQueueManagers().getFirst().getQueues().getFirst().getAlias()).isEqualTo("orders");
    assertThat(properties.getRouting().getCustomStrategyBean()).isEqualTo("custom");
    assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(5);
    assertThat(properties.getPool().getMaxConnections()).isEqualTo(20);
    assertThat(properties.getHealth().getTimeoutMs()).isEqualTo(3000L);
    assertThat(properties.getFailure().isPublishEvents()).isFalse();
    assertThat(properties.getObservability().isOpenTelemetryEnabled()).isFalse();
  }
}
