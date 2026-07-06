package com.enterprise.mq.starter.util;

import java.util.UUID;

/**
 * Holds correlation identifiers using Java 25 ScopedValue for request-scoped propagation.
 */
public final class CorrelationIdHolder {

  public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

  private CorrelationIdHolder() {}

  public static String currentOrGenerate() {
    if (CORRELATION_ID.isBound()) {
      return CORRELATION_ID.get();
    }
    return UUID.randomUUID().toString();
  }

  public static <T> T callWithCorrelationId(String correlationId, ScopedValue.CallableOp<T, Exception> operation)
      throws Exception {
    return ScopedValue.where(CORRELATION_ID, correlationId).call(operation);
  }

  public static void runWithCorrelationId(String correlationId, Runnable operation) {
    ScopedValue.where(CORRELATION_ID, correlationId).run(operation);
  }
}
