package com.bank.customer;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.customer.repository.CustomerRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Smoke test: the whole application context, including Flyway and JPA, must come up. */
@SpringBootTest
class CustomerServiceApplicationTests {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("the application context loads with the migrated schema")
    void contextLoads() {
        assertThat(customerRepository).isNotNull();
        assertThat(dataSource).isNotNull();
        assertThat(customerRepository.count()).isZero();
    }
}
