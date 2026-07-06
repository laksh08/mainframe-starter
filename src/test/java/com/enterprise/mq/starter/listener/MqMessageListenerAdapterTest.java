package com.enterprise.mq.starter.listener;

import com.enterprise.mq.starter.converter.MqMessageConverter;
import com.enterprise.mq.starter.model.MqReceiveResult;
import com.enterprise.mq.starter.service.QueueMessagingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.TextMessage;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class MqMessageListenerAdapterTest {

  @Test
  void pollAndHandleInvokesHandler() {
    QueueMessagingService messagingService = Mockito.mock(QueueMessagingService.class);
    MqMessageConverter converter = new MqMessageConverter(new ObjectMapper());
    AtomicReference<String> captured = new AtomicReference<>();

    when(messagingService.receive("DEV.QUEUE.1", String.class))
        .thenReturn(new MqReceiveResult<>("hello", null, "primary", "DEV.QUEUE.1", null, null));

    MqMessageListenerAdapter<String> adapter =
        new MqMessageListenerAdapter<>(
            messagingService, converter, "DEV.QUEUE.1", String.class, captured::set);

    adapter.pollAndHandle();

    assertThat(captured.get()).isEqualTo("hello");
  }

  @Test
  void onMessageConvertsAndHandles() throws Exception {
    QueueMessagingService messagingService = Mockito.mock(QueueMessagingService.class);
    MqMessageConverter converter = new MqMessageConverter(new ObjectMapper());
    TextMessage message = Mockito.mock(TextMessage.class);
    when(message.getText()).thenReturn("payload");
    AtomicReference<String> captured = new AtomicReference<>();

    MqMessageListenerAdapter<String> adapter =
        new MqMessageListenerAdapter<>(
            messagingService, converter, "DEV.QUEUE.1", String.class, captured::set);

    adapter.onMessage(message);

    assertThat(captured.get()).isEqualTo("payload");
  }

  @Test
  void onMessageWrapsProcessingFailure() throws Exception {
    QueueMessagingService messagingService = Mockito.mock(QueueMessagingService.class);
    MqMessageConverter converter = Mockito.mock(MqMessageConverter.class);
    jakarta.jms.Message message = Mockito.mock(jakarta.jms.Message.class);
    when(converter.fromMessage(message, String.class)).thenThrow(new RuntimeException("bad"));

    MqMessageListenerAdapter<String> adapter =
        new MqMessageListenerAdapter<>(
            messagingService, converter, "DEV.QUEUE.1", String.class, value -> {});

    assertThatThrownBy(() -> adapter.onMessage(message))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DEV.QUEUE.1");
  }
}
