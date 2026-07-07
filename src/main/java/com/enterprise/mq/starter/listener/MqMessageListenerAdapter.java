package com.enterprise.mq.starter.listener;

import com.enterprise.mq.starter.converter.MqMessageConverter;
import com.enterprise.mq.starter.service.QueueMessagingService;
import jakarta.jms.Message;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts JMS message consumption to typed Java handlers.
 */
public class MqMessageListenerAdapter<T> {

  private static final Logger LOG = LoggerFactory.getLogger(MqMessageListenerAdapter.class);

  private final QueueMessagingService messagingService;
  private final MqMessageConverter messageConverter;
  private final String queueName;
  private final Class<T> payloadType;
  private final Consumer<T> handler;

  public MqMessageListenerAdapter(
      QueueMessagingService messagingService,
      MqMessageConverter messageConverter,
      String queueName,
      Class<T> payloadType,
      Consumer<T> handler) {
    this.messagingService = messagingService;
    this.messageConverter = messageConverter;
    this.queueName = queueName;
    this.payloadType = payloadType;
    this.handler = handler;
  }

  public void pollAndHandle() {
    var result = messagingService.receive(queueName, payloadType);
    result.payloadOptional().ifPresent(handler);
  }

  public void onMessage(Message message) {
    try {
      T payload = messageConverter.fromMessage(message, payloadType);
      handler.accept(payload);
    } catch (Exception ex) {
      LOG.error("Failed to process message from queue {}", queueName, ex);
      throw new IllegalStateException("Message processing failed for queue " + queueName, ex);
    }
  }
}
