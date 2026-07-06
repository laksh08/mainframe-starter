package com.enterprise.mq.starter.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.BytesMessage;
import jakarta.jms.MapMessage;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MqMessageConverterTest {

  private MqMessageConverter converter;
  private Session session;

  @BeforeEach
  void setUp() throws Exception {
    converter = new MqMessageConverter(new ObjectMapper());
    session = Mockito.mock(Session.class);
    TextMessage textMessage = Mockito.mock(TextMessage.class);
    when(session.createTextMessage(anyString())).thenReturn(textMessage);
  }

  @Test
  void convertsStringToTextMessage() throws Exception {
    TextMessage message = (TextMessage) converter.toMessage(session, "payload");
    assertThat(message).isNotNull();
  }

  @Test
  void convertsPojoToJsonTextMessage() throws Exception {
    TextMessage message = (TextMessage) converter.toMessage(session, new SamplePayload("order"));
    assertThat(message).isNotNull();
  }

  @Test
  void convertsMapToMapMessage() throws Exception {
    MapMessage mapMessage = Mockito.mock(MapMessage.class);
    when(session.createMapMessage()).thenReturn(mapMessage);
    assertThat(converter.toMessage(session, Map.of("key", "value"))).isInstanceOf(MapMessage.class);
  }

  @Test
  void convertsBytesMessage() throws Exception {
    BytesMessage bytesMessage = Mockito.mock(BytesMessage.class);
    when(session.createBytesMessage()).thenReturn(bytesMessage);
    when(bytesMessage.getBodyLength()).thenReturn(3L);
    when(bytesMessage.readBytes(Mockito.any(byte[].class))).thenAnswer(invocation -> {
      byte[] data = invocation.getArgument(0);
      data[0] = 1;
      data[1] = 2;
      data[2] = 3;
      return 3;
    });

    assertThat(converter.toMessage(session, new byte[] {1, 2, 3})).isInstanceOf(BytesMessage.class);
    assertThat(converter.fromMessage(bytesMessage)).isInstanceOf(byte[].class);
  }

  @Test
  void convertsMapMessage() throws Exception {
    MapMessage mapMessage = Mockito.mock(MapMessage.class);
    when(session.createMapMessage()).thenReturn(mapMessage);
    when(mapMessage.getMapNames()).thenReturn(java.util.Collections.enumeration(java.util.List.of("a")));
    when(mapMessage.getObject("a")).thenReturn("b");

    Object payload = converter.fromMessage(mapMessage);
    assertThat(payload).isInstanceOf(Map.class);
  }

  @Test
  void convertsObjectMessage() throws Exception {
    ObjectMessage objectMessage = Mockito.mock(ObjectMessage.class);
    when(objectMessage.getObject()).thenReturn("serializable");
    assertThat(converter.fromMessage(objectMessage)).isEqualTo("serializable");
  }

  @Test
  void convertsJsonTextToTargetType() throws Exception {
    TextMessage textMessage = Mockito.mock(TextMessage.class);
    when(textMessage.getText()).thenReturn("{\"name\":\"order\"}");

    SamplePayload payload = converter.fromMessage(textMessage, SamplePayload.class);
    assertThat(payload.name()).isEqualTo("order");
  }

  private record SamplePayload(String name) {}
}
