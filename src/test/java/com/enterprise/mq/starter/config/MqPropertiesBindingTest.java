package com.enterprise.mq.starter.config;

import com.enterprise.mq.starter.properties.MqProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@EnableConfigurationProperties(MqProperties.class)
class MqPropertiesBindingTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(MqPropertiesBindingTest.class)
          .withPropertyValues(
              "mq.queueManagers[0].name=test-qm",
              "mq.queueManagers[0].host=localhost",
              "mq.queueManagers[0].port=1414",
              "mq.queueManagers[0].queueManager=QM1",
              "mq.queueManagers[0].channel=DEV.APP.SVRCONN",
              "mq.routing.strategy=FAILOVER",
              "mq.retry.maxAttempts=2");

  @Test
  void bindsQueueManagerConfiguration() {
    contextRunner.run(
        context -> {
          MqProperties properties = context.getBean(MqProperties.class);
          assertThat(properties.getQueueManagers()).hasSize(1);
          assertThat(properties.getQueueManagers().getFirst().getName()).isEqualTo("test-qm");
          assertThat(properties.getRouting().getStrategy())
              .isEqualTo(MqProperties.RoutingStrategy.FAILOVER);
          assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(2);
        });
  }
}
