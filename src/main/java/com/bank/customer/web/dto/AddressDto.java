package com.bank.customer.web.dto;

import com.bank.customer.domain.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Address as it appears in the API, kept separate from the {@link Address} embeddable. */
@Schema(description = "Postal address of the customer")
public record AddressDto(

        @Schema(example = "12 Nile St")
        @NotBlank(message = "street is required")
        @Size(max = 150)
        String street,

        @Schema(example = "Cairo")
        @NotBlank(message = "city is required")
        @Size(max = 100)
        String city,

        @Schema(example = "11511", description = "Optional")
        @Size(max = 20)
        String postalCode,

        @Schema(example = "EG", description = "ISO 3166-1 alpha-2 country code, uppercase")
        @NotBlank(message = "country is required")
        @Pattern(regexp = "^[A-Z]{2}$", message = "country must be an ISO 3166-1 alpha-2 code, e.g. EG")
        String country) {

    public Address toAddress() {
        return new Address(street, city, postalCode, country);
    }

    public static AddressDto from(Address address) {
        return new AddressDto(address.street(), address.city(), address.postalCode(), address.country());
    }
}
