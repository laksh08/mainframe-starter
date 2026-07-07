package com.enterprise.mq.starter.consumer;

import com.enterprise.mq.starter.converter.MqMessageConverter;
import com.enterprise.mq.starter.exception.MqReceiveException;
import com.enterprise.mq.starter.exception.RetryableMqException;
import com.enterprise.mq.starter.model.ManagedConnection;
import com.enterprise.mq.starter.model.MqReceiveResult;
import com.enterprise.mq.starter.properties.MqProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class MqMessageConsumerTest {

  private MqMessageConsumer consumer;
  private ConnectionFactory connectionFactory;
  private Connection connection;
  private Session session;
  private ManagedConnection managedConnection;

  @BeforeEach
  void setUp() throws Exception {
    consumer = new MqMessageConsumer(new MqMessageConverter(new ObjectMapper()));
    connectionFactory = Mockito.mock(ConnectionFactory.class);
    connection = Mockito.mock(Connection.class);
    session = Mockito.mock(Session.class);
    Queue queue = Mockito.mock(Queue.class);

    when(connectionFactory.createConnection()).thenReturn(connection);
    when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);
    when(session.createQueue("DEV.QUEUE.1")).thenReturn(queue);

    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    managedConnection = new ManagedConnection("primary", config, connectionFactory, true);
  }

  @Test
  void receiveReturnsPayload() throws Exception {
    MessageConsumer messageConsumer = Mockito.mock(MessageConsumer.class);
    TextMessage textMessage = Mockito.mock(TextMessage.class);
    when(session.createConsumer(Mockito.any())).thenReturn(messageConsumer);
    when(messageConsumer.receive(anyLong())).thenReturn(textMessage);
    when(textMessage.getText()).thenReturn("payload");
    when(textMessage.getJMSCorrelationID()).thenReturn("corr");
    when(textMessage.getJMSMessageID()).thenReturn("msg");

    MqReceiveResult<Object> result = consumer.receive(managedConnection, "DEV.QUEUE.1", 1000L);

    assertThat(result.payload()).isEqualTo("payload");
    assertThat(result.correlationId()).isEqualTo("corr");
  }

  @Test
  void receiveReturnsEmptyWhenNoMessage() throws Exception {
    MessageConsumer messageConsumer = Mockito.mock(MessageConsumer.class);
    when(session.createConsumer(Mockito.any())).thenReturn(messageConsumer);
    when(messageConsumer.receive(anyLong())).thenReturn(null);

    MqReceiveResult<Object> result = consumer.receive(managedConnection, "DEV.QUEUE.1", 1000L);

    assertThat(result.payload()).isNull();
  }

  @Test
  void receiveTypedPayload() throws Exception {
    MessageConsumer messageConsumer = Mockito.mock(MessageConsumer.class);
    TextMessage textMessage = Mockito.mock(TextMessage.class);
    when(session.createConsumer(Mockito.any())).thenReturn(messageConsumer);
    when(messageConsumer.receive(anyLong())).thenReturn(textMessage);
    when(textMessage.getText()).thenReturn("{\"name\":\"value\"}");
    when(textMessage.getJMSCorrelationID()).thenReturn("corr");
    when(textMessage.getJMSMessageID()).thenReturn("msg");

    MqReceiveResult<SamplePayload> result =
        consumer.receive(managedConnection, "DEV.QUEUE.1", 1000L, SamplePayload.class);

    assertThat(result.payload().name()).isEqualTo("value");
  }

  @Test
  void receiveWrapsJmsException() throws Exception {
    when(connectionFactory.createConnection()).thenThrow(new JMSException("failed"));
    assertThatThrownBy(() -> consumer.receive(managedConnection, "DEV.QUEUE.1", 1000L))
        .isInstanceOf(MqReceiveException.class);
  }

  @Test
  void receiveWrapsRetryableJmsException() throws Exception {
    when(connectionFactory.createConnection()).thenThrow(new JMSException("reason", "2033"));
    assertThatThrownBy(() -> consumer.receive(managedConnection, "DEV.QUEUE.1", 1000L))
        .isInstanceOf(RetryableMqException.class);
  }

  private record SamplePayload(String name) {}
}
