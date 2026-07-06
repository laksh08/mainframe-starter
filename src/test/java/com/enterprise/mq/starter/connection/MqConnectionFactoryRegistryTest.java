package com.enterprise.mq.starter.connection;

import com.enterprise.mq.starter.properties.MqProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class MqConnectionFactoryRegistryTest {

  @Test
  void registersEnabledQueueManagers() {
    MqProperties properties = new MqProperties();
    MqProperties.QueueManagerProperties enabled = new MqProperties.QueueManagerProperties();
    enabled.setName("primary");
    enabled.setHost("localhost");
    enabled.setPort(1414);
    enabled.setQueueManager("QM1");
    enabled.setChannel("DEV.APP.SVRCONN");
    enabled.setEnabled(true);

    MqProperties.QueueManagerProperties disabled = new MqProperties.QueueManagerProperties();
    disabled.setName("secondary");
    disabled.setHost("localhost");
    disabled.setPort(1415);
    disabled.setQueueManager("QM2");
    disabled.setChannel("DEV.APP.SVRCONN");
    disabled.setEnabled(false);

    properties.setQueueManagers(java.util.List.of(enabled, disabled));
    properties.getPool().setEnabled(false);

    MqConnectionValidator validator = Mockito.mock(MqConnectionValidator.class);
    Mockito.when(validator.validate(Mockito.any(), Mockito.eq("primary"))).thenReturn(true);

    MqConnectionFactoryRegistry registry =
        new MqConnectionFactoryRegistry(
            properties, new MqConnectionFactoryBuilder(properties), validator);

    assertThat(registry.getAllConnections()).hasSize(1);
    assertThat(registry.getConnection("primary")).isPresent();
    assertThat(registry.getConnection("secondary")).isEmpty();
  }
}
