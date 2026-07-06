package com.enterprise.mq.starter.retry;

import com.enterprise.mq.starter.exception.RetryableMqException;
import com.enterprise.mq.starter.properties.MqProperties;
import org.junit.jupiter.api.Test;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MqRetryTemplateFactoryTest {

  @Test
  void createsRetryTemplateWithConfiguredAttempts() {
    MqProperties properties = new MqProperties();
    properties.getRetry().setEnabled(true);
    properties.getRetry().setMaxAttempts(3);
    properties.getRetry().setInitialDelayMs(100L);
    properties.getRetry().setMaxDelayMs(1000L);
    properties.getRetry().setMultiplier(2.0);
    properties.getRetry().setExponentialBackoff(true);

    MqRetryTemplateFactory factory = new MqRetryTemplateFactory(properties);
    RetryTemplate retryTemplate = factory.createRetryTemplate();

    assertThat(retryTemplate).isNotNull();
    assertThat(factory.retryableClassifier().classify(new RetryableMqException("retry"))).isTrue();
    assertThat(factory.retryableClassifier().classify(new IllegalStateException("fail"))).isFalse();
  }

  @Test
  void retriesRetryableExceptions() {
    MqProperties properties = new MqProperties();
    properties.getRetry().setEnabled(true);
    properties.getRetry().setMaxAttempts(3);
    properties.getRetry().setInitialDelayMs(10L);
    properties.getRetry().setExponentialBackoff(false);

    RetryTemplate retryTemplate = new MqRetryTemplateFactory(properties).createRetryTemplate();
    int[] attempts = {0};

    assertThatThrownBy(
            () ->
                retryTemplate.execute(
                    context -> {
                      attempts[0]++;
                      throw new RetryableMqException("temporary");
                    }))
        .isInstanceOf(RetryableMqException.class);

    assertThat(attempts[0]).isEqualTo(3);
  }
}
