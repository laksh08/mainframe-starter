package com.enterprise.mq.starter.connection;

import com.enterprise.mq.starter.properties.MqProperties;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MqConnectionValidatorTest {

  @Test
  void returnsFalseWhenConnectionFails() throws Exception {
    ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
    when(connectionFactory.createConnection()).thenThrow(new JMSException("failed"));

    MqConnectionValidator validator = new MqConnectionValidator();
    assertThat(validator.validate(connectionFactory, "primary")).isFalse();
  }

  @Test
  void returnsTrueWhenConnectionSucceeds() throws Exception {
    ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
    Connection connection = Mockito.mock(Connection.class);
    Session session = Mockito.mock(Session.class);
    when(connectionFactory.createConnection()).thenReturn(connection);
    when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);

    MqConnectionValidator validator = new MqConnectionValidator();
    assertThat(validator.validate(connectionFactory, "primary")).isTrue();
  }
}
