package com.bank.customer.event;

import com.bank.customer.domain.Customer;
import com.bank.customer.domain.CustomerType;
import java.time.Instant;

/**
 * What this service publishes to the {@code customer-events} topic.
 *
 * @param eventType  {@link #CREATED} or {@link #UPDATED}
 * @param customerId also used as the Kafka message key
 */
public record CustomerEvent(
        String eventType,
        Long customerId,
        String name,
        CustomerType type,
        Instant occurredAt) {

    public static final String CREATED = "CUSTOMER_CREATED";
    public static final String UPDATED = "CUSTOMER_UPDATED";

    public static CustomerEvent created(Customer customer) {
        return of(CREATED, customer);
    }

    public static CustomerEvent updated(Customer customer) {
        return of(UPDATED, customer);
    }

    private static CustomerEvent of(String eventType, Customer customer) {
        return new CustomerEvent(
                eventType, customer.getId(), customer.getName(), customer.getType(), Instant.now());
    }
}
