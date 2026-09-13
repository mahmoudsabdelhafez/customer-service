package com.bank.customer.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.customer.config.SecurityConfig;
import com.bank.customer.domain.CustomerType;
import com.bank.customer.exception.CustomerNotFoundException;
import com.bank.customer.exception.DuplicateLegalIdException;
import com.bank.customer.service.CustomerService;
import com.bank.customer.web.dto.AddressDto;
import com.bank.customer.web.dto.CreateCustomerRequest;
import com.bank.customer.web.dto.CustomerResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/** The web layer on its own: the service is mocked, status codes and JSON shape are tested. */
@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final AddressDto ADDRESS = new AddressDto("12 Nile St", "Cairo", "11511", "EG");

    private static CreateCustomerRequest createRequest() {
        return new CreateCustomerRequest(
                "Mahmoud Abdelhafez", "29001011234567", CustomerType.RETAIL, ADDRESS,
                "mahmoud@example.com", "+201234567890");
    }

    private static CustomerResponse response() {
        return new CustomerResponse(1000001L, "Mahmoud Abdelhafez", "29001011234567",
                CustomerType.RETAIL, ADDRESS, "mahmoud@example.com", "+201234567890",
                Instant.now(), Instant.now());
    }

    private static String json(Object body) {
        return JSON.writeValueAsString(body);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST returns 201 with a Location header and the created customer")
    void createReturnsCreated() throws Exception {
        given(customerService.create(any())).willReturn(response());

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/customers/1000001"))
                .andExpect(jsonPath("$.id").value(1000001))
                .andExpect(jsonPath("$.legalId").value("29001011234567"))
                .andExpect(jsonPath("$.address.country").value("EG"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST returns 400 and lists every invalid field")
    void createRejectsInvalidBody() throws Exception {
        CreateCustomerRequest invalid = new CreateCustomerRequest(
                "ab", "", null, new AddressDto("", "Cairo", null, "EGY"), "not-an-email", "0100");

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.legalId").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phoneNumber").exists())
                .andExpect(jsonPath("$.errors.['address.street']").exists())
                .andExpect(jsonPath("$.errors.['address.country']").exists());

        verify(customerService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST returns 409 when the legal id is taken")
    void createReportsDuplicateLegalId() throws Exception {
        willThrow(new DuplicateLegalIdException("29001011234567"))
                .given(customerService).create(any());

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate legal id"))
                .andExpect(jsonPath("$.detail").value("a customer with legal id '29001011234567' already exists"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET by id returns the customer")
    void getByIdReturnsCustomer() throws Exception {
        given(customerService.getById(1000001L)).willReturn(response());

        mockMvc.perform(get("/api/v1/customers/1000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1000001))
                .andExpect(jsonPath("$.name").value("Mahmoud Abdelhafez"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET by id returns 404 for an unknown customer")
    void getByIdReturnsNotFound() throws Exception {
        willThrow(new CustomerNotFoundException(9999999L)).given(customerService).getById(9999999L);

        mockMvc.perform(get("/api/v1/customers/9999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET the collection returns a page and passes the type filter through")
    void listReturnsPage() throws Exception {
        given(customerService.list(eq(CustomerType.RETAIL), any()))
                .willReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/customers").param("type", "RETAIL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1000001))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT returns the updated customer")
    void updateReturnsCustomer() throws Exception {
        given(customerService.update(eq(1000001L), any())).willReturn(response());

        String body = """
                {
                  "name": "Acme Holdings",
                  "type": "CORPORATE",
                  "address": {"street": "1 Tahrir Sq", "city": "Cairo", "country": "EG"}
                }
                """;

        mockMvc.perform(put("/api/v1/customers/1000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1000001));
    }

    @Test
    @DisplayName("an anonymous caller gets 401")
    void anonymousIsUnauthorised() throws Exception {
        mockMvc.perform(get("/api/v1/customers/1000001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("a read-only caller may not create customers")
    void viewerMayNotWrite() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createRequest())))
                .andExpect(status().isForbidden());

        verify(customerService, never()).create(any());
    }
}
