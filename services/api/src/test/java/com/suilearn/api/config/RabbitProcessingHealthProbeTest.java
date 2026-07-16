package com.suilearn.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitProcessingHealthProbeTest {
    @Test
    void performsPassiveDeclareToRequireABrokerRoundTrip() throws Exception {
        var template = mock(RabbitTemplate.class);
        var channel = mock(Channel.class);
        doAnswer(invocation -> {
            ChannelCallback<?> callback = invocation.getArgument(0);
            return callback.doInRabbit(channel);
        }).when(template).execute(any(ChannelCallback.class));
        var probe = new RabbitProcessingHealthProbe(template);

        var health = probe.health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        verify(channel).queueDeclarePassive("document.processing");
    }
}
