package com.suilearn.api.runtimefixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.task.application.PersistentInboundMessageStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RuntimeFixtureDuplicateMessageProbeTest {
    @Test
    void claimsAnInternallyGeneratedMessageOnceAndRejectsItsDuplicateWithoutDisclosingTheIdentity() {
        var messages = mock(PersistentInboundMessageStore.class);
        when(messages.claim(anyString())).thenReturn(true, false);
        var probe = new RuntimeFixtureDuplicateMessageProbe(messages);

        var response = probe.trigger();

        assertThat(response.firstDeliveryClaimed()).isTrue();
        assertThat(response.duplicateDeliveryRejected()).isTrue();
        assertThat(response.getClass().getRecordComponents()).extracting(component -> component.getName())
            .containsExactly("firstDeliveryClaimed", "duplicateDeliveryRejected");
        var identities = ArgumentCaptor.forClass(String.class);
        verify(messages, times(2)).claim(identities.capture());
        verify(messages).complete(identities.getValue());
        assertThat(identities.getAllValues()).hasSize(2).allSatisfy(identity -> assertThat(identity).startsWith("runtime-fixture-"));
        assertThat(identities.getAllValues()).allSatisfy(identity -> assertThat(identity).isEqualTo(identities.getValue()));
    }
}
