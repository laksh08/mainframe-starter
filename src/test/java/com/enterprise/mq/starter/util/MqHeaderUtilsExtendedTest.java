package com.enterprise.mq.starter.util;

import com.enterprise.mq.starter.model.MqMessageHeaders;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqHeaderUtilsExtendedTest {

  @Test
  void applyHeadersIgnoresNullHeaders() throws Exception {
    var session = Mockito.mock(jakarta.jms.Session.class);
    TextMessage message = Mockito.mock(TextMessage.class);
    MqHeaderUtils.applyHeaders(session, message, null);
    verify(message, never()).setJMSCorrelationID(Mockito.anyString());
  }

  @Test
  void applyHeadersSkipsNullFields() throws Exception {
    var session = Mockito.mock(jakarta.jms.Session.class);
    TextMessage message = Mockito.mock(TextMessage.class);
    MqHeaderUtils.applyHeaders(
        session, message, MqMessageHeaders.builder().correlationId("only-corr").build());
    verify(message).setJMSCorrelationID("only-corr");
  }

  @Test
  void extractHeadersHandlesNonPersistentMessage() throws Exception {
    TextMessage message = Mockito.mock(TextMessage.class);
    when(message.getJMSCorrelationID()).thenReturn("c1");
    when(message.getJMSMessageID()).thenReturn("m1");
    when(message.getJMSReplyTo()).thenReturn(null);
    when(message.getJMSExpiration()).thenReturn(0L);
    when(message.getJMSPriority()).thenReturn(4);
    when(message.getJMSDeliveryMode()).thenReturn(jakarta.jms.DeliveryMode.NON_PERSISTENT);
    when(message.getPropertyNames()).thenReturn(java.util.Collections.emptyEnumeration());

    MqMessageHeaders headers = MqHeaderUtils.extractHeaders(message);
    assertThat(headers.persistent()).isFalse();
  }
}
