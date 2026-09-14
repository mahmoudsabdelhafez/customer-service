package com.bank.customer.dto;

import com.bank.customer.persistence.entity.Customer;
import com.bank.customer.persistence.entity.CustomerType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Representation of a customer returned by the API. */
@Schema(description = "A bank customer")
public record CustomerResponse(

        @Schema(example = "1000001") Long id,
        @Schema(example = "Mahmoud Abdelhafez") String name,
        @Schema(example = "29001011234567") String legalId,
        @Schema(example = "RETAIL") CustomerType type,
        AddressDto address,
        @Schema(example = "mahmoud@example.com") String email,
        @Schema(example = "+201234567890") String phoneNumber,
        Instant createdAt,
        Instant updatedAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getLegalId(),
                customer.getType(),
                AddressDto.from(customer.getAddress()),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getCreatedAt(),
                customer.getUpdatedAt());
    }
}
