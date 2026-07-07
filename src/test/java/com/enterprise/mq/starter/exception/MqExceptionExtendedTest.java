package com.enterprise.mq.starter.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqExceptionExtendedTest {

  @Test
  void typedExceptionsRetainContext() {
    MqPublishException publish =
        new MqPublishException("pub", new IllegalStateException("x"), "QM1", "Q1");
    MqReceiveException receive =
        new MqReceiveException("recv", new IllegalStateException("x"), "QM1", "Q1");
    RetryableMqException retry =
        new RetryableMqException("retry", new IllegalStateException("x"), "QM1", "Q1");

    assertThat(publish.getQueueName()).isEqualTo("Q1");
    assertThat(receive.getQueueManagerName()).isEqualTo("QM1");
    assertThat(retry.getCause()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void routingExceptionSupportsCause() {
    MqRoutingException exception = new MqRoutingException("route", new IllegalStateException("x"));
    assertThat(exception.getCause()).isNotNull();
  }
}
