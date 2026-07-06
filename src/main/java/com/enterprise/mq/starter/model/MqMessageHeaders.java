package com.enterprise.mq.starter.model;

import java.time.Duration;
import java.util.Map;

/**
 * JMS message headers for send operations.
 */
public record MqMessageHeaders(
    String correlationId,
    String messageId,
    String replyTo,
    Long expiration,
    Integer priority,
    Boolean persistent,
    Map<String, Object> customHeaders) {

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private String correlationId;
    private String messageId;
    private String replyTo;
    private Long expiration;
    private Integer priority;
    private Boolean persistent;
    private Map<String, Object> customHeaders;

    public Builder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder messageId(String messageId) {
      this.messageId = messageId;
      return this;
    }

    public Builder replyTo(String replyTo) {
      this.replyTo = replyTo;
      return this;
    }

    public Builder expiration(Long expiration) {
      this.expiration = expiration;
      return this;
    }

    public Builder priority(Integer priority) {
      this.priority = priority;
      return this;
    }

    public Builder persistent(Boolean persistent) {
      this.persistent = persistent;
      return this;
    }

    public Builder customHeaders(Map<String, Object> customHeaders) {
      this.customHeaders = customHeaders;
      return this;
    }

    public MqMessageHeaders build() {
      return new MqMessageHeaders(
          correlationId, messageId, replyTo, expiration, priority, persistent, customHeaders);
    }
  }
}
