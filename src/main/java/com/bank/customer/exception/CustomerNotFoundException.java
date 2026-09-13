package com.bank.customer.exception;

/** Thrown when a customer id or legal id does not match any stored customer. */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(Long id) {
        super("customer %d was not found".formatted(id));
    }
}
