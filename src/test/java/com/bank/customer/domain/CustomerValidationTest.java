package com.bank.customer.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Bean Validation rules declared on {@link Customer}. */
class CustomerValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Customer validCustomer() {
        Customer customer = new Customer(
                "Mahmoud Abdelhafez",
                "29001011234567",
                CustomerType.RETAIL,
                new Address("12 Nile St", "Cairo", "11511", "EG"));
        customer.setEmail("mahmoud@example.com");
        customer.setPhoneNumber("+201234567890");
        return customer;
    }

    private static Set<String> violatedFieldsOf(Customer customer) {
        return validator.validate(customer).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("a fully populated customer has no violations")
    void validCustomerPasses() {
        assertThat(validator.validate(validCustomer())).isEmpty();
    }

    @Test
    @DisplayName("the optional fields may be omitted")
    void optionalFieldsMayBeNull() {
        Customer customer = new Customer(
                "Acme Holdings", "CR-4455661", CustomerType.CORPORATE,
                new Address("1 Tahrir Sq", "Cairo", null, "EG"));

        assertThat(validator.validate(customer)).isEmpty();
    }

    @ParameterizedTest(name = "name \"{0}\" is rejected")
    @ValueSource(strings = {"", "   ", "ab"})
    @DisplayName("name must be present and at least 3 characters")
    void rejectsBadName(String name) {
        Customer customer = validCustomer();
        customer.setName(name);

        assertThat(violatedFieldsOf(customer)).contains("name");
    }

    @ParameterizedTest(name = "email \"{0}\" is rejected")
    @ValueSource(strings = {"not-an-email", "@example.com", "two@@example.com"})
    void rejectsMalformedEmail(String email) {
        Customer customer = validCustomer();
        customer.setEmail(email);

        assertThat(violatedFieldsOf(customer)).contains("email");
    }

    @ParameterizedTest(name = "phone \"{0}\" is rejected")
    @ValueSource(strings = {"01234567890", "+0123456789", "+20 123 456 7890", "12345"})
    @DisplayName("phone number must be in E.164 format")
    void rejectsNonE164Phone(String phone) {
        Customer customer = validCustomer();
        customer.setPhoneNumber(phone);

        assertThat(violatedFieldsOf(customer)).contains("phoneNumber");
    }

    @Test
    @DisplayName("address is required")
    void rejectsMissingAddress() {
        Customer customer = validCustomer();
        customer.setAddress(null);

        assertThat(violatedFieldsOf(customer)).contains("address");
    }

    @ParameterizedTest(name = "{1} is reported on the nested address")
    @MethodSource("invalidAddresses")
    @DisplayName("violations inside the embedded address are cascaded by @Valid")
    void cascadesIntoAddress(Address address, String expectedPath) {
        Customer customer = validCustomer();
        customer.setAddress(address);

        assertThat(violatedFieldsOf(customer)).contains(expectedPath);
    }

    private static Stream<Arguments> invalidAddresses() {
        return Stream.of(
                Arguments.of(new Address("", "Cairo", null, "EG"), "address.street"),
                Arguments.of(new Address("1 Main St", "", null, "EG"), "address.city"),
                Arguments.of(new Address("1 Main St", "Cairo", null, "EGY"), "address.country"),
                Arguments.of(new Address("1 Main St", "Cairo", null, "eg"), "address.country"));
    }
}
