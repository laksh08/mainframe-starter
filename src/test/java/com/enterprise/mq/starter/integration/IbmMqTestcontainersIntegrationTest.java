package com.enterprise.mq.starter.integration;

import com.ibm.mq.testcontainers.MQContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class IbmMqTestcontainersIntegrationTest {

  @Container
  static final MQContainer MQ = new MQContainer("icr.io/ibm-messaging/mq:10.0.0.0");

  @Test
  void startsMqContainer() {
    assertThat(MQ.getConnName()).isNotBlank();
    assertThat(MQ.getQueueManager()).isNotBlank();
  }
}
