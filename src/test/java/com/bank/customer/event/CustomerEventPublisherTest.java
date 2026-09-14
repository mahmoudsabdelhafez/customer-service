package com.bank.customer.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.bank.customer.persistence.entity.Address;
import com.bank.customer.persistence.entity.Customer;
import com.bank.customer.persistence.entity.CustomerType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * What {@link CustomerEventPublisher} puts on the topic, and what it does when
 * the broker rejects the send.
 */
@ExtendWith(MockitoExtension.class)
class CustomerEventPublisherTest {

    private static final String TOPIC = "customer-events";

    @Mock
    private KafkaTemplate<String, CustomerEvent> kafkaTemplate;

    /** Runs the send inline, so the assertions below do not have to wait. */
    private static final Executor DIRECT = Runnable::run;

    private CustomerEventPublisher publisher;

    private static Customer customer() {
        return new Customer("Acme Holdings", "CR-4455661", CustomerType.CORPORATE,
                new Address("1 Tahrir Sq", "Cairo", null, "EG"));
    }

    private ArgumentCaptor<CustomerEvent> captureSend() {
        given(kafkaTemplate.send(eq(TOPIC), any(), any(CustomerEvent.class)))
                .willReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));
        publisher = new CustomerEventPublisher(kafkaTemplate, DIRECT, TOPIC);
        return ArgumentCaptor.forClass(CustomerEvent.class);
    }

    @Test
    @DisplayName("publishes CUSTOMER_CREATED keyed by the customer id")
    void publishesCreated() {
        ArgumentCaptor<CustomerEvent> event = captureSend();

        publisher.publishCreated(customer());

        verify(kafkaTemplate).send(eq(TOPIC), eq("null"), event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(CustomerEvent.CREATED);
        assertThat(event.getValue().name()).isEqualTo("Acme Holdings");
        assertThat(event.getValue().type()).isEqualTo(CustomerType.CORPORATE);
        assertThat(event.getValue().occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("publishes CUSTOMER_UPDATED")
    void publishesUpdated() {
        ArgumentCaptor<CustomerEvent> event = captureSend();

        publisher.publishUpdated(customer());

        verify(kafkaTemplate).send(eq(TOPIC), any(), event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(CustomerEvent.UPDATED);
    }

    @Test
    @DisplayName("a send that fails later is logged, not thrown at the caller")
    void asyncFailureDoesNotPropagate() {
        given(kafkaTemplate.send(eq(TOPIC), any(), any(CustomerEvent.class)))
                .willReturn(CompletableFuture.failedFuture(new IllegalStateException("broker is down")));
        publisher = new CustomerEventPublisher(kafkaTemplate, DIRECT, TOPIC);

        assertThatNoException().isThrownBy(() -> publisher.publishCreated(customer()));
    }

    @Test
    @DisplayName("an unreachable broker does not fail the caller either")
    void synchronousFailureDoesNotPropagate() {
        // send() can throw synchronously when no broker is reachable.
        given(kafkaTemplate.send(eq(TOPIC), any(), any(CustomerEvent.class)))
                .willThrow(new KafkaException("Send failed"));
        publisher = new CustomerEventPublisher(kafkaTemplate, DIRECT, TOPIC);

        assertThatNoException().isThrownBy(() -> publisher.publishUpdated(customer()));
    }
}
