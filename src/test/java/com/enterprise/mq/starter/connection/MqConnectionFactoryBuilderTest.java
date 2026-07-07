package com.enterprise.mq.starter.connection;

import com.enterprise.mq.starter.properties.MqProperties;
import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqConnectionFactoryBuilderTest {

  @Test
  void buildsConnectionFactoryWithoutPooling() {
    MqProperties properties = new MqProperties();
    properties.getPool().setEnabled(false);

    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setHost("localhost");
    config.setPort(1414);
    config.setQueueManager("QM1");
    config.setChannel("DEV.APP.SVRCONN");

    ConnectionFactory connectionFactory = new MqConnectionFactoryBuilder(properties).build(config);

    assertThat(connectionFactory).isNotNull();
  }

  @Test
  void buildsPooledConnectionFactoryWhenEnabled() {
    MqProperties properties = new MqProperties();
    properties.getPool().setEnabled(true);
    properties.getPool().setMaxConnections(5);

    MqProperties.QueueManagerProperties config = new MqProperties.QueueManagerProperties();
    config.setName("primary");
    config.setHost("localhost");
    config.setPort(1414);
    config.setQueueManager("QM1");
    config.setChannel("DEV.APP.SVRCONN");

    ConnectionFactory connectionFactory = new MqConnectionFactoryBuilder(properties).build(config);

    assertThat(connectionFactory).isNotNull();
    assertThat(connectionFactory.getClass().getName()).contains("JmsPoolConnectionFactory");
  }
}
