package com.enterprise.mq.starter.util;

import com.enterprise.mq.starter.model.FailureType;
import com.enterprise.mq.starter.event.MqFailureEvent;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredLoggerTest {

  @Test
  void logsWithoutThrowing() {
    var logger = LoggerFactory.getLogger(StructuredLoggerTest.class);
    StructuredLogger.info(logger, "test.event", Map.of("key", "value"));
    StructuredLogger.warn(logger, "test.event", StructuredLogger.baseFields("QM1", "Q1", "send"));
    StructuredLogger.error(
        logger,
        "test.event",
        Map.of("detail", "failed"),
        new IllegalStateException("boom"));
  }

  @Test
  void failureEventExposesMetadata() {
    MqFailureEvent event =
        new MqFailureEvent(
            this,
            FailureType.QUEUE,
            "QM1",
            "DEV.QUEUE.1",
            "CHANNEL",
            "failed",
            new RuntimeException("cause"));

    assertThat(event.getFailureType()).isEqualTo(FailureType.QUEUE);
    assertThat(event.getQueueManagerName()).isEqualTo("QM1");
    assertThat(event.getQueueName()).isEqualTo("DEV.QUEUE.1");
    assertThat(event.getChannelName()).isEqualTo("CHANNEL");
    assertThat(event.getMessage()).isEqualTo("failed");
    assertThat(event.getCause()).isInstanceOf(RuntimeException.class);
  }
}
