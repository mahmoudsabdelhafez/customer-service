package com.bank.customer.controller;

import com.bank.customer.persistence.entity.CustomerType;
import com.bank.customer.service.CustomerService;
import com.bank.customer.dto.CreateCustomerRequest;
import com.bank.customer.dto.CustomerResponse;
import com.bank.customer.dto.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for customers. */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Registration and management of bank customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @Operation(summary = "Register a new customer",
            description = "The 7-digit customer id is issued by the service, not supplied by the caller.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer registered"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "409", description = "Legal id already registered", content = @Content)
    })
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse created = customerService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/customers/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one customer by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer found"),
            @ApiResponse(responseCode = "404", description = "No such customer", content = @Content)
    })
    public CustomerResponse getById(
            @Parameter(description = "7-digit customer id", example = "1000001")
            @PathVariable Long id) {
        return customerService.getById(id);
    }

    @GetMapping
    @Operation(summary = "List customers",
            description = "Paged. Add ?type=RETAIL to narrow the list to one customer type.")
    public PagedModel<CustomerResponse> list(
            @Parameter(description = "Optional customer type filter")
            @RequestParam(required = false) CustomerType type,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        // Page itself is not directly serializable; PagedModel wraps it for JSON.
        return new PagedModel<>(customerService.list(type, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing customer",
            description = "The legal id cannot be changed and is not part of the request body.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @ApiResponse(responseCode = "404", description = "No such customer", content = @Content)
    })
    public CustomerResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        return customerService.update(id, request);
    }
}
