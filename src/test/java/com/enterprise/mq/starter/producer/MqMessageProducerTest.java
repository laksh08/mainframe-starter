package com.enterprise.mq.starter.producer;

import com.enterprise.mq.starter.converter.MqMessageConverter;
import com.enterprise.mq.starter.exception.MqPublishException;
import com.enterprise.mq.starter.exception.RetryableMqException;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.MqMessageHeaders;
import com.enterprise.mq.starter.properties.MqProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqMessageProducerTest {

  private MqMessageProducer producer;
  private ConnectionFactory connectionFactory;
  private Connection connection;
  private Session session;
  private ManagedConnection managedConnection;

  @BeforeEach
  void setUp() throws Exception {
    producer = new MqMessageProducer(new MqMessageConverter(new ObjectMapper()));
    connectionFactory = Mockito.mock(ConnectionFactory.class);
    connection = Mockito.mock(Connection.class);
    session = Mockito.mock(Session.class);
    Destination destination = Mockito.mock(Queue.class);
    MessageProducer messageProducer = Mockito.mock(MessageProducer.class);
    TextMessage textMessage = Mockito.mock(TextMessage.class);

    when(connectionFactory.createConnection()).thenReturn(connection);
    when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);
    when(session.createQueue("DEV.QUEUE.1")).thenReturn((Queue) destination);
    when(session.createTextMessage(anyString())).thenReturn(textMessage);
    when(textMessage.getJMSMessageID()).thenReturn("ID:123");
    when(session.createProducer(destination)).thenReturn(messageProducer);

    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setChannel("DEV.APP.SVRCONN");
    managedConnection = new ManagedConnection("primary", config, connectionFactory, true);
  }

  @Test
  void sendPublishesMessage() throws Exception {
    producer.send(managedConnection, "DEV.QUEUE.1", "payload", null);
    verify(session).createProducer(any());
  }

  @Test
  void sendWithHeadersAppliesMetadata() throws Exception {
    MqMessageHeaders headers =
        MqMessageHeaders.builder().correlationId("corr").persistent(true).priority(3).build();
    producer.send(managedConnection, "DEV.QUEUE.1", "payload", headers);
    verify(session).createProducer(any());
  }

  @Test
  void sendWrapsJmsExceptionAsPublishException() throws Exception {
    when(connectionFactory.createConnection()).thenThrow(new JMSException("failed"));
    assertThatThrownBy(() -> producer.send(managedConnection, "DEV.QUEUE.1", "payload", null))
        .isInstanceOf(MqPublishException.class);
  }

  @Test
  void sendWrapsRetryableJmsException() throws Exception {
    JMSException retryable = new JMSException("reason", "2033");
    when(connectionFactory.createConnection()).thenThrow(retryable);
    assertThatThrownBy(() -> producer.send(managedConnection, "DEV.QUEUE.1", "payload", null))
        .isInstanceOf(RetryableMqException.class);
  }

  @Test
  void requestReplyReturnsResponse() throws Exception {
    Queue replyQueue = Mockito.mock(Queue.class);
    when(session.createQueue("DEV.QUEUE.REPLY")).thenReturn(replyQueue);
    TextMessage requestMessage = Mockito.mock(TextMessage.class);
    when(session.createTextMessage(anyString())).thenReturn(requestMessage);
    when(requestMessage.getJMSMessageID()).thenReturn("ID:123");

    jakarta.jms.MessageConsumer consumer = Mockito.mock(jakarta.jms.MessageConsumer.class);
    Message reply = Mockito.mock(Message.class);
    when(session.createConsumer(any(), anyString())).thenReturn(consumer);
    when(consumer.receive(anyLong())).thenReturn(reply);

    Message result =
        producer.requestReply(
            managedConnection, "DEV.QUEUE.1", "payload", null, "DEV.QUEUE.REPLY", 1000L);

    assertThat(result).isSameAs(reply);
  }

  @Test
  void requestReplyTimesOut() throws Exception {
    Queue replyQueue = Mockito.mock(Queue.class);
    when(session.createQueue("DEV.QUEUE.REPLY")).thenReturn(replyQueue);
    TextMessage requestMessage = Mockito.mock(TextMessage.class);
    when(session.createTextMessage(anyString())).thenReturn(requestMessage);
    when(requestMessage.getJMSMessageID()).thenReturn("ID:123");

    jakarta.jms.MessageConsumer consumer = Mockito.mock(jakarta.jms.MessageConsumer.class);
    when(session.createConsumer(any(), anyString())).thenReturn(consumer);
    when(consumer.receive(anyLong())).thenReturn(null);

    assertThatThrownBy(
            () ->
                producer.requestReply(
                    managedConnection, "DEV.QUEUE.1", "payload", null, "DEV.QUEUE.REPLY", 100L))
        .isInstanceOf(MqPublishException.class);
  }
}
