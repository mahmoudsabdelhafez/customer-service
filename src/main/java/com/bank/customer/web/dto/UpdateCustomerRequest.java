package com.bank.customer.web.dto;

import com.bank.customer.domain.CustomerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of {@code PUT /api/v1/customers/{id}}. Has no {@code legalId}: it never changes. */
@Schema(description = "Fields of an existing customer that may be changed")
public record UpdateCustomerRequest(

        @Schema(example = "Mahmoud A. Abdelhafez")
        @NotBlank(message = "name is required")
        @Size(min = 3, max = 150, message = "name must be between {min} and {max} characters")
        String name,

        @Schema(example = "CORPORATE")
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
}
