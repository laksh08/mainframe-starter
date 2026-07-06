package com.enterprise.mq.starter.util;

import com.enterprise.mq.starter.model.MqMessageHeaders;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqHeaderUtilsTest {

  @Test
  void appliesAndExtractsHeaders() throws Exception {
    Session session = Mockito.mock(Session.class);
    TextMessage message = Mockito.mock(TextMessage.class);
    Queue replyQueue = Mockito.mock(Queue.class);
    when(session.createQueue("DEV.QUEUE.2")).thenReturn(replyQueue);
    when(message.getJMSCorrelationID()).thenReturn("corr-1");
    when(message.getJMSMessageID()).thenReturn("msg-1");
    when(message.getJMSReplyTo()).thenReturn(replyQueue);
    when(replyQueue.toString()).thenReturn("DEV.QUEUE.2");
    when(message.getJMSExpiration()).thenReturn(1000L);
    when(message.getJMSPriority()).thenReturn(5);
    when(message.getJMSDeliveryMode()).thenReturn(jakarta.jms.DeliveryMode.PERSISTENT);
    when(message.getPropertyNames()).thenReturn(java.util.Collections.emptyEnumeration());

    MqMessageHeaders headers =
        MqMessageHeaders.builder()
            .correlationId("corr-1")
            .messageId("msg-1")
            .replyTo("DEV.QUEUE.2")
            .expiration(1000L)
            .priority(5)
            .persistent(true)
            .build();

    MqHeaderUtils.applyHeaders(session, message, headers);

    verify(message).setJMSCorrelationID("corr-1");
    verify(message).setJMSReplyTo(replyQueue);

    MqMessageHeaders extracted = MqHeaderUtils.extractHeaders(message);
    assertThat(extracted.correlationId()).isEqualTo("corr-1");
    assertThat(extracted.priority()).isEqualTo(5);
  }
}
