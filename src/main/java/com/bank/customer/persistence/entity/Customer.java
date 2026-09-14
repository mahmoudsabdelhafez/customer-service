package com.bank.customer.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A bank customer.
 *
 * <p>Accounts belong to the separate account service and are not mapped here;
 * they reference a customer only by id.
 */
@Entity
@Table(
        name = "customers",
        uniqueConstraints = @UniqueConstraint(name = "uk_customers_legal_id", columnNames = "legal_id"))
public class Customer {

    /** Lowest 7-digit customer id. */
    public static final long MIN_ID = 1_000_000L;

    /** Highest 7-digit customer id. */
    public static final long MAX_ID = 9_999_999L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_id_generator")
    @SequenceGenerator(
            name = "customer_id_generator",
            sequenceName = "customer_id_seq",
            allocationSize = 1)
    private Long id;

    @NotBlank(message = "name is required")
    @Size(min = 3, max = 150, message = "name must be between {min} and {max} characters")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** National id or commercial registration number. Unique, never changes once set. */
    @NotBlank(message = "legalId is required")
    @Size(min = 5, max = 50, message = "legalId must be between {min} and {max} characters")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "legalId may contain only letters, digits and hyphens")
    @Column(name = "legal_id", nullable = false, length = 50, updatable = false)
    private String legalId;

    @NotNull(message = "type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CustomerType type;

    @Valid
    @NotNull(message = "address is required")
    @Embedded
    private Address address;

    @Email(message = "email must be a well-formed address")
    @Size(max = 150, message = "email must not exceed {max} characters")
    @Column(name = "email", length = 150)
    private String email;

    @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "phoneNumber must be in E.164 format, e.g. +201234567890")
    @Column(name = "phone_number", length = 16)
    private String phoneNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected Customer() {
    }

    public Customer(String name, String legalId, CustomerType type, Address address) {
        this.name = name;
        this.legalId = legalId;
        this.type = type;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLegalId() {
        return legalId;
    }

    public CustomerType getType() {
        return type;
    }

    public void setType(CustomerType type) {
        this.type = type;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Identity is the database id; two unsaved customers are never equal. */
    @Override
    public boolean equals(Object other) {
        return other instanceof Customer customer && id != null && id.equals(customer.id);
    }

    @Override
    public int hashCode() {
        return Customer.class.hashCode();
    }

    @Override
    public String toString() {
        return "Customer[id=%d, type=%s]".formatted(id, type);
    }
}
