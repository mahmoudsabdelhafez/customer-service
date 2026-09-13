package com.bank.customer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.customer.domain.CustomerType;
import com.bank.customer.event.CustomerEventPublisher;
import com.bank.customer.web.dto.AddressDto;
import com.bank.customer.web.dto.CreateCustomerRequest;
import com.bank.customer.web.dto.CustomerResponse;
import com.bank.customer.web.dto.UpdateCustomerRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The service against a real database and real transactions. Only Kafka is mocked. */
@SpringBootTest
class CustomerServiceIntegrationTest {

    @Autowired
    private CustomerService service;

    @MockitoBean
    private CustomerEventPublisher eventPublisher;

    private static CreateCustomerRequest request(String legalId) {
        return new CreateCustomerRequest(
                "Mahmoud Abdelhafez", legalId, CustomerType.RETAIL,
                new AddressDto("12 Nile St", "Cairo", "11511", "EG"),
                "mahmoud@example.com", "+201234567890");
    }

    @Test
    @DisplayName("the created customer comes back with its id and timestamps already filled in")
    void createReturnsGeneratedValues() {
        CustomerResponse created = service.create(request("29001011111111"));

        assertThat(String.valueOf(created.id())).hasSize(7);
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("an update moves updatedAt past createdAt")
    void updateRefreshesUpdatedAt() {
        CustomerResponse created = service.create(request("29001012222222"));

        CustomerResponse updated = service.update(created.id(), new UpdateCustomerRequest(
                "Acme Holdings", CustomerType.CORPORATE,
                new AddressDto("1 Tahrir Sq", "Cairo", null, "EG"), null, null));

        assertThat(updated.createdAt()).isEqualTo(created.createdAt());
        assertThat(updated.updatedAt()).isAfter(created.updatedAt());
        assertThat(updated.legalId()).isEqualTo("29001012222222");
    }

    @Test
    @DisplayName("the customer is readable again after the transaction commits")
    void createdCustomerIsPersisted() {
        CustomerResponse created = service.create(request("29001013333333"));

        assertThat(service.getById(created.id()).name()).isEqualTo("Mahmoud Abdelhafez");
    }
}
