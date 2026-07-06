package com.enterprise.mq.starter.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdHolderTest {

  @Test
  void generatesCorrelationIdWhenNotBound() {
    assertThat(CorrelationIdHolder.currentOrGenerate()).isNotBlank();
  }

  @Test
  void usesScopedCorrelationId() {
    CorrelationIdHolder.runWithCorrelationId(
        "test-correlation-id",
        () -> assertThat(CorrelationIdHolder.currentOrGenerate()).isEqualTo("test-correlation-id"));
  }

  @Test
  void callWithCorrelationIdReturnsValue() throws Exception {
    String value =
        CorrelationIdHolder.callWithCorrelationId(
            "abc", () -> CorrelationIdHolder.currentOrGenerate());
    assertThat(value).isEqualTo("abc");
  }
}
