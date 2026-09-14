package com.bank.customer.event;

import com.bank.customer.persistence.entity.Customer;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes customer events to Kafka.
 *
 * <p>The send runs off the request thread and both failure modes (an
 * unreachable broker, or a rejected send) are caught and logged, so a Kafka
 * outage never slows down or fails the request that triggered the event.
 */
@Component
public class CustomerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CustomerEventPublisher.class);

    private final KafkaTemplate<String, CustomerEvent> kafkaTemplate;
    private final Executor executor;
    private final String topic;

    public CustomerEventPublisher(
            KafkaTemplate<String, CustomerEvent> kafkaTemplate,
            @Qualifier("applicationTaskExecutor") Executor executor,
            @Value("${app.kafka.customer-events-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.executor = executor;
        this.topic = topic;
    }

    public void publishCreated(Customer customer) {
        publish(CustomerEvent.created(customer));
    }

    public void publishUpdated(Customer customer) {
        publish(CustomerEvent.updated(customer));
    }

    private void publish(CustomerEvent event) {
        executor.execute(() -> send(event));
    }

    private void send(CustomerEvent event) {
        try {
            // Keyed by customer id so events for one customer stay in order.
            kafkaTemplate.send(topic, String.valueOf(event.customerId()), event)
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            logFailure(event, failure);
                        } else {
                            log.debug("published {} for customer {} to {}",
                                    event.eventType(), event.customerId(), topic);
                        }
                    });
        } catch (Exception exception) {
            logFailure(event, exception);
        }
    }

    private void logFailure(CustomerEvent event, Throwable cause) {
        log.error("failed to publish {} for customer {}", event.eventType(), event.customerId(), cause);
    }
}
