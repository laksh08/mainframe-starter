package com.enterprise.mq.starter.converter;

import com.enterprise.mq.starter.exception.MqException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.io.Serializable;
import java.util.Map;

/**
 * Converts between JMS messages and Java objects.
 */
public class MqMessageConverter {

  private final ObjectMapper objectMapper;

  public MqMessageConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Message toMessage(Session session, Object payload) throws JMSException {
    if (payload == null) {
      return session.createTextMessage("");
    }
    return switch (payload) {
      case String text -> session.createTextMessage(text);
      case byte[] bytes -> {
        BytesMessage message = session.createBytesMessage();
        message.writeBytes(bytes);
        yield message;
      }
      case Map<?, ?> map -> createMapMessage(session, map);
      case Serializable serializable -> {
        ObjectMessage message = session.createObjectMessage();
        message.setObject(serializable);
        yield message;
      }
      default -> session.createTextMessage(toJson(payload));
    };
  }

  public Object fromMessage(Message message) throws JMSException {
    return switch (message) {
      case TextMessage textMessage -> extractTextPayload(textMessage);
      case BytesMessage bytesMessage -> extractBytes(bytesMessage);
      case MapMessage mapMessage -> extractMap(mapMessage);
      case ObjectMessage objectMessage -> objectMessage.getObject();
      default -> message.toString();
    };
  }

  public <T> T fromMessage(Message message, Class<T> targetType) throws JMSException {
    Object payload = fromMessage(message);
    if (payload == null) {
      return null;
    }
    if (targetType.isInstance(payload)) {
      return targetType.cast(payload);
    }
    if (payload instanceof String text) {
      return fromJson(text, targetType);
    }
    return objectMapper.convertValue(payload, targetType);
  }

  private Message createMapMessage(Session session, Map<?, ?> map) throws JMSException {
    MapMessage message = session.createMapMessage();
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      message.setObject(String.valueOf(entry.getKey()), entry.getValue());
    }
    return message;
  }

  private Object extractTextPayload(TextMessage textMessage) throws JMSException {
    String text = textMessage.getText();
    if (text == null || text.isBlank()) {
      return text;
    }
    if (text.startsWith("{") || text.startsWith("[")) {
      return text;
    }
    return text;
  }

  private byte[] extractBytes(BytesMessage bytesMessage) throws JMSException {
    bytesMessage.reset();
    byte[] data = new byte[(int) bytesMessage.getBodyLength()];
    bytesMessage.readBytes(data);
    return data;
  }

  private Map<String, Object> extractMap(MapMessage mapMessage) throws JMSException {
    Map<String, Object> map = new java.util.LinkedHashMap<>();
    var names = mapMessage.getMapNames();
    while (names.hasMoreElements()) {
      String name = String.valueOf(names.nextElement());
      map.put(name, mapMessage.getObject(name));
    }
    return map;
  }

  private String toJson(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new MqException("Failed to serialize payload to JSON", ex);
    }
  }

  private <T> T fromJson(String json, Class<T> targetType) {
    try {
      return objectMapper.readValue(json, targetType);
    } catch (JsonProcessingException ex) {
      throw new MqException("Failed to deserialize JSON payload", ex);
    }
  }
}
