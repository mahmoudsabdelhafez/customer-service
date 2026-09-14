package com.bank.customer.dto;

import com.bank.customer.persistence.entity.Customer;
import com.bank.customer.persistence.entity.CustomerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of {@code POST /api/v1/customers}. The id is issued by the service, not supplied here. */
@Schema(description = "Details needed to register a new customer")
public record CreateCustomerRequest(

        @Schema(example = "Mahmoud Abdelhafez")
        @NotBlank(message = "name is required")
        @Size(min = 3, max = 150, message = "name must be between {min} and {max} characters")
        String name,

        @Schema(example = "29001011234567", description = "National id or commercial registration number")
        @NotBlank(message = "legalId is required")
        @Size(min = 5, max = 50, message = "legalId must be between {min} and {max} characters")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "legalId may contain only letters, digits and hyphens")
        String legalId,

        @Schema(example = "RETAIL")
        @NotNull(message = "type is required")
        CustomerType type,

        @Valid
        @NotNull(message = "address is required")
        AddressDto address,

        @Schema(example = "mahmoud@example.com", description = "Optional")
        @Email(message = "email must be a well-formed address")
        @Size(max = 150)
        String email,

        @Schema(example = "+201234567890", description = "Optional, E.164 format")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "phoneNumber must be in E.164 format, e.g. +201234567890")
        String phoneNumber) {

    public Customer toCustomer() {
        Customer customer = new Customer(name, legalId, type, address.toAddress());
        customer.setEmail(email);
        customer.setPhoneNumber(phoneNumber);
        return customer;
    }
}
