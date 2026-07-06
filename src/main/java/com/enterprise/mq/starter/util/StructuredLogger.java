package com.enterprise.mq.starter.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * Structured logging helper for MQ operations.
 */
public final class StructuredLogger {

  private StructuredLogger() {}

  public static void info(Logger logger, String event, Map<String, Object> fields) {
    withMdc(fields, () -> logger.info(buildMessage(event, fields)));
  }

  public static void warn(Logger logger, String event, Map<String, Object> fields) {
    withMdc(fields, () -> logger.warn(buildMessage(event, fields)));
  }

  public static void error(Logger logger, String event, Map<String, Object> fields, Throwable cause) {
    withMdc(fields, () -> logger.error(buildMessage(event, fields), cause));
  }

  public static Map<String, Object> baseFields(
      String queueManager, String queue, String operation) {
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put("event", operation);
    fields.put("queueManager", queueManager);
    fields.put("queue", queue);
    fields.put("correlationId", CorrelationIdHolder.currentOrGenerate());
    return fields;
  }

  private static String buildMessage(String event, Map<String, Object> fields) {
    try {
      Map<String, Object> payload = new LinkedHashMap<>(fields);
      payload.putIfAbsent("event", event);
      return new ObjectMapper().writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      return event + " " + fields;
    }
  }

  private static void withMdc(Map<String, Object> fields, Runnable action) {
    Map<String, String> previous = MDC.getCopyOfContextMap();
    try {
      fields.forEach((key, value) -> {
        if (value != null) {
          MDC.put(key, String.valueOf(value));
        }
      });
      action.run();
    } finally {
      MDC.clear();
      if (previous != null) {
        MDC.setContextMap(previous);
      }
    }
  }
}
