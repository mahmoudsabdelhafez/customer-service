package com.bank.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Postal address of a customer, embedded inline in the customers table. */
@Embeddable
public record Address(

        @NotBlank(message = "street is required")
        @Size(max = 150, message = "street must not exceed {max} characters")
        @Column(name = "street", nullable = false, length = 150)
        String street,

        @NotBlank(message = "city is required")
        @Size(max = 100, message = "city must not exceed {max} characters")
        @Column(name = "city", nullable = false, length = 100)
        String city,

        @Size(max = 20, message = "postalCode must not exceed {max} characters")
        @Column(name = "postal_code", length = 20)
        String postalCode,

        @NotBlank(message = "country is required")
        @Pattern(regexp = "^[A-Z]{2}$", message = "country must be an ISO 3166-1 alpha-2 code, e.g. EG")
        @Column(name = "country", nullable = false, length = 2)
        String country) {
}
