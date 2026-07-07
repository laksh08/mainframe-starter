package com.enterprise.mq.starter.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelRecordsTest {

  @Test
  void managedConnectionWithHealthCreatesCopy() {
    ManagedConnection original =
        new ManagedConnection("primary", null, null, true);
    ManagedConnection updated = original.withHealth(false);
    assertThat(original.healthy()).isTrue();
    assertThat(updated.healthy()).isFalse();
  }

  @Test
  void receiveResultOptionalHandlesNullPayload() {
    MqReceiveResult<String> empty =
        new MqReceiveResult<>(null, null, "primary", "Q1", null, null);
    assertThat(empty.payloadOptional()).isEmpty();
  }

  @Test
  void messageHeadersBuilderCreatesHeaders() {
    MqMessageHeaders headers =
        MqMessageHeaders.builder()
            .correlationId("c1")
            .messageId("m1")
            .replyTo("Q2")
            .expiration(10L)
            .priority(1)
            .persistent(true)
            .customHeaders(java.util.Map.of("k", "v"))
            .build();

    assertThat(headers.correlationId()).isEqualTo("c1");
    assertThat(headers.customHeaders()).containsEntry("k", "v");
  }

  @Test
  void failureTypeContainsExpectedValues() {
    assertThat(FailureType.values()).contains(FailureType.CONNECTION, FailureType.RECEIVE);
  }
}
