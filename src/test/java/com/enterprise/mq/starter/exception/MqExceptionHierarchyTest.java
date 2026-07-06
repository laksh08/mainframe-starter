package com.enterprise.mq.starter.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqExceptionHierarchyTest {

  @Test
  void baseExceptionStoresContext() {
    MqException exception = new MqException("failed", null, "QM1", "DEV.QUEUE.1");
    assertThat(exception.getMessage()).isEqualTo("failed");
    assertThat(exception.getQueueManagerName()).isEqualTo("QM1");
    assertThat(exception.getQueueName()).isEqualTo("DEV.QUEUE.1");
  }

  @Test
  void connectionExceptionSupportsCause() {
    MqConnectionException exception =
        new MqConnectionException("connection failed", new IllegalStateException("root"), "QM1");
    assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
    assertThat(exception.getQueueManagerName()).isEqualTo("QM1");
  }

  @Test
  void publishReceiveRoutingAndRetryableExceptions() {
    assertThat(new MqPublishException("pub").getMessage()).isEqualTo("pub");
    assertThat(new MqReceiveException("recv").getMessage()).isEqualTo("recv");
    assertThat(new MqRoutingException("route").getMessage()).isEqualTo("route");
    assertThat(new RetryableMqException("retry").getMessage()).isEqualTo("retry");
  }
}
