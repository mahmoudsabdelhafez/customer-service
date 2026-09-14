package com.bank.customer.service;

import com.bank.customer.persistence.entity.Customer;
import com.bank.customer.persistence.entity.CustomerType;
import com.bank.customer.event.CustomerEventPublisher;
import com.bank.customer.exception.CustomerNotFoundException;
import com.bank.customer.exception.DuplicateLegalIdException;
import com.bank.customer.persistence.repository.CustomerRepository;
import com.bank.customer.dto.CreateCustomerRequest;
import com.bank.customer.dto.CustomerResponse;
import com.bank.customer.dto.UpdateCustomerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for managing customers. */
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository repository;
    private final CustomerEventPublisher eventPublisher;

    public CustomerService(CustomerRepository repository, CustomerEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registers a new customer and announces it on Kafka.
     *
     * @throws DuplicateLegalIdException if the legal id is already registered
     */
    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (repository.existsByLegalId(request.legalId())) {
            throw new DuplicateLegalIdException(request.legalId());
        }

        // Flush so createdAt/updatedAt are populated before the response is built.
        Customer saved = repository.saveAndFlush(request.toCustomer());
        log.info("registered customer {} of type {}", saved.getId(), saved.getType());

        eventPublisher.publishCreated(saved);
        return CustomerResponse.from(saved);
    }

    /**
     * @throws CustomerNotFoundException if no customer has that id
     */
    public CustomerResponse getById(Long id) {
        return CustomerResponse.from(findOrThrow(id));
    }

    /**
     * Lists customers, one page at a time, optionally narrowed to a single type.
     *
     * @param type may be {@code null}, meaning "every type"
     */
    public Page<CustomerResponse> list(CustomerType type, Pageable pageable) {
        Page<Customer> customers = type == null
                ? repository.findAll(pageable)
                : repository.findByType(type, pageable);
        return customers.map(CustomerResponse::from);
    }

    /**
     * Replaces the changeable fields of an existing customer. The legal id and
     * the generated id are not part of the update contract.
     *
     * @throws CustomerNotFoundException if no customer has that id
     */
    @Transactional
    public CustomerResponse update(Long id, UpdateCustomerRequest request) {
        Customer customer = findOrThrow(id);
        customer.setName(request.name());
        customer.setType(request.type());
        customer.setAddress(request.address().toAddress());
        customer.setEmail(request.email());
        customer.setPhoneNumber(request.phoneNumber());

        // Flush so updatedAt is populated before the response is built.
        repository.saveAndFlush(customer);
        log.info("updated customer {}", id);

        eventPublisher.publishUpdated(customer);
        return CustomerResponse.from(customer);
    }

    private Customer findOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }
}
