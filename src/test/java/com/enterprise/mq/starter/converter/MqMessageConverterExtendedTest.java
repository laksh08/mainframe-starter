package com.enterprise.mq.starter.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MqMessageConverterExtendedTest {

  @Test
  void fromMessageUsesToStringForUnknownMessageType() throws Exception {
    MqMessageConverter converter = new MqMessageConverter(new ObjectMapper());
    Message message = Mockito.mock(Message.class);
    when(message.toString()).thenReturn("fallback");

    assertThat(converter.fromMessage(message)).isEqualTo("fallback");
  }

  @Test
  void toMessageHandlesNullPayload() throws Exception {
    MqMessageConverter converter = new MqMessageConverter(new ObjectMapper());
    Session session = Mockito.mock(Session.class);
    var textMessage = Mockito.mock(jakarta.jms.TextMessage.class);
    when(session.createTextMessage("")).thenReturn(textMessage);

    assertThat(converter.toMessage(session, null)).isSameAs(textMessage);
  }
}
