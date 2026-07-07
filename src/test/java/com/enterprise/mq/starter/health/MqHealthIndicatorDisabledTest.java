package com.enterprise.mq.starter.health;

import com.enterprise.mq.starter.connection.MqConnectionFactoryRegistry;
import com.enterprise.mq.starter.connection.MqConnectionValidator;
import com.enterprise.mq.starter.properties.MqProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.health.contributor.Health;

import static org.assertj.core.api.Assertions.assertThat;

class MqHealthIndicatorDisabledTest {

  @Test
  void returnsUpWhenHealthChecksDisabled() {
    MqProperties properties = new MqProperties();
    properties.getHealth().setEnabled(false);

    MqHealthIndicator indicator =
        new MqHealthIndicator(
            properties,
            Mockito.mock(MqConnectionFactoryRegistry.class),
            Mockito.mock(MqConnectionValidator.class));

    Health health = indicator.health();
    assertThat(health.getStatus().getCode()).isEqualTo("UP");
    assertThat(health.getDetails()).containsEntry("status", "Health checks disabled");
  }
}
