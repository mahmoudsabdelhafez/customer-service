package com.bank.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bank.customer.domain.Address;
import com.bank.customer.domain.Customer;
import com.bank.customer.domain.CustomerType;
import com.bank.customer.event.CustomerEventPublisher;
import com.bank.customer.exception.CustomerNotFoundException;
import com.bank.customer.exception.DuplicateLegalIdException;
import com.bank.customer.repository.CustomerRepository;
import com.bank.customer.web.dto.AddressDto;
import com.bank.customer.web.dto.CreateCustomerRequest;
import com.bank.customer.web.dto.CustomerResponse;
import com.bank.customer.web.dto.UpdateCustomerRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Business rules of {@link CustomerService}, with the repository and publisher mocked. */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerEventPublisher eventPublisher;

    @InjectMocks
    private CustomerService service;

    private static final AddressDto ADDRESS = new AddressDto("12 Nile St", "Cairo", "11511", "EG");

    private static CreateCustomerRequest createRequest() {
        return new CreateCustomerRequest(
                "Mahmoud Abdelhafez", "29001011234567", CustomerType.RETAIL, ADDRESS,
                "mahmoud@example.com", "+201234567890");
    }

    private static Customer storedCustomer() {
        return new Customer("Mahmoud Abdelhafez", "29001011234567", CustomerType.RETAIL,
                new Address("12 Nile St", "Cairo", "11511", "EG"));
    }

    @Test
    @DisplayName("create saves the customer and publishes a created event")
    void createSavesAndPublishes() {
        given(repository.existsByLegalId("29001011234567")).willReturn(false);
        given(repository.saveAndFlush(any(Customer.class))).willAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = service.create(createRequest());

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getLegalId()).isEqualTo("29001011234567");
        assertThat(saved.getValue().getAddress()).isEqualTo(new Address("12 Nile St", "Cairo", "11511", "EG"));
        assertThat(response.name()).isEqualTo("Mahmoud Abdelhafez");
        assertThat(response.email()).isEqualTo("mahmoud@example.com");
        verify(eventPublisher).publishCreated(any(Customer.class));
    }

    @Test
    @DisplayName("create rejects a legal id that is already registered")
    void createRejectsDuplicateLegalId() {
        given(repository.existsByLegalId("29001011234567")).willReturn(true);

        assertThatThrownBy(() -> service.create(createRequest()))
                .isInstanceOf(DuplicateLegalIdException.class)
                .hasMessageContaining("29001011234567");

        verify(repository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishCreated(any());
    }

    @Test
    @DisplayName("getById returns the stored customer")
    void getByIdReturnsCustomer() {
        given(repository.findById(1000001L)).willReturn(Optional.of(storedCustomer()));

        assertThat(service.getById(1000001L).legalId()).isEqualTo("29001011234567");
    }

    @Test
    @DisplayName("getById fails when the customer does not exist")
    void getByIdFailsForUnknownCustomer() {
        given(repository.findById(9999999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(9999999L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("9999999");
    }

    @Test
    @DisplayName("list without a type filter reads every customer")
    void listWithoutFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Customer> page = new PageImpl<>(List.of(storedCustomer()), pageable, 1);
        given(repository.findAll(pageable)).willReturn(page);

        assertThat(service.list(null, pageable).getContent())
                .singleElement()
                .extracting(CustomerResponse::type)
                .isEqualTo(CustomerType.RETAIL);
    }

    @Test
    @DisplayName("list with a type filter delegates to the narrower query")
    void listWithFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        given(repository.findByType(CustomerType.CORPORATE, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(service.list(CustomerType.CORPORATE, pageable)).isEmpty();
        verify(repository, never()).findAll(pageable);
    }

    @Test
    @DisplayName("update changes the mutable fields and publishes an updated event")
    void updateChangesFieldsAndPublishes() {
        Customer stored = storedCustomer();
        given(repository.findById(1000001L)).willReturn(Optional.of(stored));

        CustomerResponse response = service.update(1000001L, new UpdateCustomerRequest(
                "Acme Holdings", CustomerType.CORPORATE,
                new AddressDto("1 Tahrir Sq", "Cairo", null, "EG"),
                null, null));

        assertThat(stored.getName()).isEqualTo("Acme Holdings");
        assertThat(stored.getType()).isEqualTo(CustomerType.CORPORATE);
        assertThat(stored.getEmail()).isNull();
        assertThat(response.address().street()).isEqualTo("1 Tahrir Sq");
        // The legal id is not part of the update contract and must survive it.
        assertThat(stored.getLegalId()).isEqualTo("29001011234567");
        verify(repository).saveAndFlush(stored);
        verify(eventPublisher).publishUpdated(stored);
    }

    @Test
    @DisplayName("update fails when the customer does not exist")
    void updateFailsForUnknownCustomer() {
        given(repository.findById(9999999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(9999999L, new UpdateCustomerRequest(
                "Acme Holdings", CustomerType.CORPORATE, ADDRESS, null, null)))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(repository, never()).saveAndFlush(any());
        verify(eventPublisher, never()).publishUpdated(any());
    }
}
