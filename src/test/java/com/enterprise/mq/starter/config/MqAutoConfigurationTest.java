package com.enterprise.mq.starter.config;

import com.enterprise.mq.starter.properties.MqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MqAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(MqAutoConfiguration.class))
          .withBean(SimpleMeterRegistry.class)
          .withPropertyValues(
              "mq.queueManagers[0].name=primary",
              "mq.queueManagers[0].host=localhost",
              "mq.queueManagers[0].port=1414",
              "mq.queueManagers[0].queueManager=QM1",
              "mq.queueManagers[0].channel=DEV.APP.SVRCONN",
              "mq.pool.enabled=false",
              "mq.health.validateQueues=false");

  @BeforeEach
  void setUp() {
    contextRunner.withPropertyValues("spring.main.banner-mode=off");
  }

  @Test
  void autoConfigurationCreatesBeansWhenEnabled() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(MqProperties.class);
          assertThat(context).hasSingleBean(com.enterprise.mq.starter.service.QueueMessagingService.class);
        });
  }

  @Test
  void autoConfigurationDisabledWhenMqDisabled() {
    contextRunner
        .withPropertyValues("mq.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(MqProperties.class));
  }
}
