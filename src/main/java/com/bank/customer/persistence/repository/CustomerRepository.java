package com.bank.customer.persistence.repository;

import com.bank.customer.persistence.entity.Customer;
import com.bank.customer.persistence.entity.CustomerType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Customer}. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /** Looks a customer up by its natural key. */
    Optional<Customer> findByLegalId(String legalId);

    /** Pre-check used to return a clean 409 instead of a raw integrity violation. */
    boolean existsByLegalId(String legalId);

    Page<Customer> findByType(CustomerType type, Pageable pageable);
}
