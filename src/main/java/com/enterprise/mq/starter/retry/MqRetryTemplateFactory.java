package com.enterprise.mq.starter.retry;

import com.enterprise.mq.starter.exception.RetryableMqException;
import com.enterprise.mq.starter.properties.MqProperties;
import org.springframework.classify.Classifier;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Factory for Spring Retry templates configured from application properties.
 */
public class MqRetryTemplateFactory {

  private final MqProperties properties;

  public MqRetryTemplateFactory(MqProperties properties) {
    this.properties = properties;
  }

  public RetryTemplate createRetryTemplate() {
    MqProperties.RetryProperties retry = properties.getRetry();
    RetryTemplate retryTemplate = new RetryTemplate();

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(retry.isEnabled() ? retry.getMaxAttempts() : 1);
    retryTemplate.setRetryPolicy(retryPolicy);

    if (retry.isExponentialBackoff()) {
      ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
      backOffPolicy.setInitialInterval(retry.getInitialDelayMs());
      backOffPolicy.setMaxInterval(retry.getMaxDelayMs());
      backOffPolicy.setMultiplier(retry.getMultiplier());
      retryTemplate.setBackOffPolicy(backOffPolicy);
    } else {
      FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
      backOffPolicy.setBackOffPeriod(retry.getInitialDelayMs());
      retryTemplate.setBackOffPolicy(backOffPolicy);
    }

    return retryTemplate;
  }

  public Classifier<Throwable, Boolean> retryableClassifier() {
    return throwable -> {
      if (throwable instanceof RetryableMqException) {
        return true;
      }
      Throwable cause = throwable.getCause();
      return cause instanceof RetryableMqException;
    };
  }
}
