package com.bank.customer.exception;

/** Thrown when registering a customer whose legal id already exists. */
public class DuplicateLegalIdException extends RuntimeException {

    public DuplicateLegalIdException(String legalId) {
        super("a customer with legal id '%s' already exists".formatted(legalId));
    }
}
