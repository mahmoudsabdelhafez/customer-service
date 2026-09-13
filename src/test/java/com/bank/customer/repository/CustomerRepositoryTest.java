package com.bank.customer.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.customer.domain.Address;
import com.bank.customer.domain.Customer;
import com.bank.customer.domain.CustomerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/** Persistence behaviour of {@link CustomerRepository}, run against the real Flyway migration. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    private static Customer retailCustomer(String legalId) {
        return new Customer(
                "Mahmoud Abdelhafez", legalId, CustomerType.RETAIL,
                new Address("12 Nile St", "Cairo", "11511", "EG"));
    }

    @Test
    @DisplayName("assigns a distinct 7-digit id from the bounded sequence")
    void assignsSevenDigitId() {
        Customer first = repository.saveAndFlush(retailCustomer("29001011234567"));
        Customer second = repository.saveAndFlush(retailCustomer("29001011234568"));

        assertThat(first.getId()).isNotNull().isBetween(Customer.MIN_ID, Customer.MAX_ID);
        assertThat(String.valueOf(first.getId())).hasSize(7);
        assertThat(second.getId()).isNotEqualTo(first.getId());
    }

    @Test
    @DisplayName("fills in the audit timestamps on insert")
    void populatesTimestamps() {
        Customer saved = repository.saveAndFlush(retailCustomer("29001011234567"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("round-trips the embedded address")
    void roundTripsAddress() {
        Address address = new Address("9 Corniche", "Alexandria", "21500", "EG");
        Customer customer = new Customer("Nile Traders", "CR-778899", CustomerType.CORPORATE, address);

        Long id = repository.saveAndFlush(customer).getId();
        entityManager.clear();

        assertThat(repository.findById(id)).get()
                .extracting(Customer::getAddress)
                .isEqualTo(address);
    }

    @Test
    @DisplayName("stores the type as a readable string, not an ordinal")
    void storesEnumAsString() {
        Long id = repository.saveAndFlush(
                new Customer("Delta Investments", "CR-112233", CustomerType.INVESTMENT,
                        new Address("1 Tahrir Sq", "Cairo", null, "EG"))).getId();

        Object type = entityManager.getEntityManager()
                .createNativeQuery("select type from customers where id = ?1")
                .setParameter(1, id)
                .getSingleResult();

        assertThat(type).isEqualTo("INVESTMENT");
    }

    @Test
    @DisplayName("rejects a duplicate legal id")
    void rejectsDuplicateLegalId() {
        repository.saveAndFlush(retailCustomer("29001011234567"));

        assertThatThrownBy(() -> repository.saveAndFlush(retailCustomer("29001011234567")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("finds a customer by its legal id")
    void findsByLegalId() {
        Long id = repository.saveAndFlush(retailCustomer("29001011234567")).getId();

        assertThat(repository.findByLegalId("29001011234567"))
                .get().extracting(Customer::getId).isEqualTo(id);
        assertThat(repository.findByLegalId("does-not-exist")).isEmpty();
    }

    @Test
    @DisplayName("reports whether a legal id is already taken")
    void existsByLegalId() {
        repository.saveAndFlush(retailCustomer("29001011234567"));

        assertThat(repository.existsByLegalId("29001011234567")).isTrue();
        assertThat(repository.existsByLegalId("29001011234568")).isFalse();
    }
}
