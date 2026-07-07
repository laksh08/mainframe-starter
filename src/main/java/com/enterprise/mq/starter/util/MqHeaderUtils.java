package com.enterprise.mq.starter.util;

import com.enterprise.mq.starter.model.MqMessageHeaders;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for JMS message headers.
 */
public final class MqHeaderUtils {

  private MqHeaderUtils() {}

  public static void applyHeaders(Session session, Message message, MqMessageHeaders headers)
      throws JMSException {
    if (headers == null) {
      return;
    }
    if (headers.correlationId() != null) {
      message.setJMSCorrelationID(headers.correlationId());
    }
    if (headers.messageId() != null) {
      message.setJMSMessageID(headers.messageId());
    }
    if (headers.replyTo() != null) {
      message.setJMSReplyTo(session.createQueue(headers.replyTo()));
    }
    if (headers.expiration() != null) {
      message.setJMSExpiration(headers.expiration());
    }
    if (headers.priority() != null) {
      message.setJMSPriority(headers.priority());
    }
    if (headers.persistent() != null) {
      message.setJMSDeliveryMode(
          headers.persistent() ? jakarta.jms.DeliveryMode.PERSISTENT
              : jakarta.jms.DeliveryMode.NON_PERSISTENT);
    }
    if (headers.customHeaders() != null) {
      for (Map.Entry<String, Object> entry : headers.customHeaders().entrySet()) {
        message.setObjectProperty(entry.getKey(), entry.getValue());
      }
    }
  }

  public static MqMessageHeaders extractHeaders(Message message) throws JMSException {
    Map<String, Object> customHeaders = new HashMap<>();
    Enumeration<?> propertyNames = message.getPropertyNames();
    while (propertyNames.hasMoreElements()) {
      String name = String.valueOf(propertyNames.nextElement());
      customHeaders.put(name, message.getObjectProperty(name));
    }
    Destination replyTo = message.getJMSReplyTo();
    return MqMessageHeaders.builder()
        .correlationId(message.getJMSCorrelationID())
        .messageId(message.getJMSMessageID())
        .replyTo(replyTo != null ? replyTo.toString() : null)
        .expiration(message.getJMSExpiration())
        .priority(message.getJMSPriority())
        .persistent(message.getJMSDeliveryMode() == jakarta.jms.DeliveryMode.PERSISTENT)
        .customHeaders(customHeaders)
        .build();
  }

}
