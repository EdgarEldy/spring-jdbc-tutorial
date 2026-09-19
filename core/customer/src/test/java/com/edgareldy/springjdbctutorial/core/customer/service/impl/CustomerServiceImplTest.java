package com.edgareldy.springjdbctutorial.core.customer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.edgareldy.springjdbctutorial.core.customer.dao.CustomerDao;
import com.edgareldy.springjdbctutorial.core.customer.dto.CustomerDto;
import com.edgareldy.springjdbctutorial.core.customer.entity.Customer;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Unit tests of CustomerServiceImpl: the DAO is a Mockito mock, assertions are made on the dto objects.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// MockitoExtension builds the @Mock fields before each test and fails a test whose stubbing is never used
// (strict stubs), so no test keeps a stub it does not need. No Spring context and no database here.
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerDao customerDao;

    private CustomerServiceImpl service() {
        return new CustomerServiceImpl(customerDao);
    }

    private static CustomerDto input(String first, String last, String telephone, String email, String address) {
        return new CustomerDto(null, first, last, telephone, email, address);
    }

    private static CustomerDto withEmail(String email) {
        return input("Alice", "Martin", null, email, null);
    }

    private Customer created() {
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerDao).insert(captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------- list

    @Test
    void _01_ShouldReturnPageOfDtos_WhenListing() {
        when(customerDao.findPage(1, 2)).thenReturn(List.of(new Customer(3L, "Carol", "Diaz", null, null, null)));
        when(customerDao.countAll()).thenReturn(3L);

        PageDto<CustomerDto> page = service().list(1, 2);

        assertThat(page.getContent()).containsExactly(new CustomerDto(3L, "Carol", "Diaz", null, null, null));
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(3L);
    }

    @Test
    void _02_ShouldReturnEmptyContent_WhenPageIsBeyondTheLastOne() {
        when(customerDao.findPage(50, 10)).thenReturn(List.of());
        when(customerDao.countAll()).thenReturn(3L);

        PageDto<CustomerDto> page = service().list(50, 10);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3L);
    }

    @Test
    void _03_ShouldThrowBusinessRuleException_WhenPageIsNegative() {
        assertThatThrownBy(() -> service().list(-1, 10)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(customerDao);
    }

    @Test
    void _04_ShouldThrowBusinessRuleException_WhenSizeIsZero() {
        assertThatThrownBy(() -> service().list(0, 0)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(customerDao);
    }

    @Test
    void _05_ShouldThrowBusinessRuleException_WhenSizeIsAbove100() {
        assertThatThrownBy(() -> service().list(0, 101)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(customerDao);
    }

    @Test
    void _06_ShouldAcceptBoundarySizes_WhenSizeIs1Or100() {
        when(customerDao.findPage(0, 1)).thenReturn(List.of());
        when(customerDao.findPage(0, 100)).thenReturn(List.of());
        when(customerDao.countAll()).thenReturn(0L);

        assertThat(service().list(0, 1).getSize()).isEqualTo(1);
        assertThat(service().list(0, 100).getSize()).isEqualTo(100);
    }

    // ---------------------------------------------------------------- get

    @Test
    void _07_ShouldReturnDto_WhenCustomerExists() {
        when(customerDao.findById(2L)).thenReturn(Optional.of(new Customer(2L, "Bob", "Stone", "1", "b@s.io", "Rd")));

        assertThat(service().get(2L)).isEqualTo(new CustomerDto(2L, "Bob", "Stone", "1", "b@s.io", "Rd"));
    }

    @Test
    void _08_ShouldThrowResourceNotFoundException_WhenCustomerDoesNotExist() {
        when(customerDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(99L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Customer not found: 99");
    }

    // ---------------------------------------------------------------- create

    @Test
    void _09_ShouldTrimEveryFieldAndReturnGeneratedId_WhenCreatingACustomer() {
        when(customerDao.insert(any(Customer.class))).thenAnswer(call -> {
            Customer customer = call.getArgument(0);
            customer.setId(7L);
            return customer;
        });

        CustomerDto dto = service().create(input("  Alice ", " Martin  ", " 555 ", " alice@example.com ", " 1 Main "));

        assertThat(dto).isEqualTo(new CustomerDto(7L, "Alice", "Martin", "555", "alice@example.com", "1 Main"));
        assertThat(created().getId()).isEqualTo(7L);
    }

    @Test
    void _10_ShouldStoreNull_WhenOptionalFieldsAreBlankOrAbsent() {
        when(customerDao.insert(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        service().create(input("Alice", "Martin", "   ", "", null));

        Customer stored = created();
        assertThat(stored.getTelephone()).isNull();
        assertThat(stored.getEmail()).isNull();
        assertThat(stored.getAddress()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void _11_ShouldThrowBusinessRuleException_WhenFirstOrLastNameIsBlank(String blank) {
        assertThatThrownBy(() -> service().create(input(blank, "Martin", null, null, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("First name must not be blank");
        assertThatThrownBy(() -> service().create(input("Alice", blank, null, null, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Last name must not be blank");
        assertThatThrownBy(() -> service().create(input(null, "Martin", null, null, null)))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(customerDao);
    }

    @Test
    void _12_ShouldAcceptNamesOf100AndRefuse101_WhenCheckingTheNameLimit() {
        when(customerDao.insert(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        service().create(input("x".repeat(100), "y".repeat(100), null, null, null));

        assertThatThrownBy(() -> service().create(input("x".repeat(101), "Martin", null, null, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("First name must be at most 100 characters");
        assertThatThrownBy(() -> service().create(input("Alice", "y".repeat(101), null, null, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Last name must be at most 100 characters");
    }

    @Test
    void _13_ShouldAcceptTelephoneOf30AndRefuse31_WhenCheckingTheTelephoneLimit() {
        when(customerDao.insert(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        service().create(input("Alice", "Martin", "1".repeat(30), null, null));

        assertThatThrownBy(() -> service().create(input("Alice", "Martin", "1".repeat(31), null, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Telephone must be at most 30 characters");
    }

    @Test
    void _14_ShouldAcceptAddressOf255AndRefuse256_WhenCheckingTheAddressLimit() {
        when(customerDao.insert(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        service().create(input("Alice", "Martin", null, null, "a".repeat(255)));

        assertThatThrownBy(() -> service().create(input("Alice", "Martin", null, null, "a".repeat(256))))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Address must be at most 255 characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice@example.com", "a.b+c@sub.example.org", "a@b.c"})
    void _15_ShouldAcceptEmail_WhenFormatIsValid(String email) {
        when(customerDao.insert(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service().create(withEmail(email)).getEmail()).isEqualTo(email);
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice.example.com", "a@@example.com", "a@b@example.com", "alice@example", "alice@.com",
            "alice@example.", "ali ce@example.com", "alice@exa mple.com", "@example.com"})
    void _16_ShouldThrowBusinessRuleException_WhenEmailFormatIsInvalid(String email) {
        assertThatThrownBy(() -> service().create(withEmail(email)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Email is not valid");
        verify(customerDao, never()).insert(any());
    }

    @Test
    void _17_ShouldThrowBusinessRuleException_WhenEmailIsLongerThan255() {
        String email = "a".repeat(250) + "@b.com";

        assertThatThrownBy(() -> service().create(withEmail(email)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Email must be at most 255 characters");
    }

    // ---------------------------------------------------------------- update

    @Test
    void _18_ShouldUpdateAndReturnCleanedDto_WhenCustomerExists() {
        when(customerDao.findById(2L)).thenReturn(Optional.of(new Customer(2L, "Bob", "Stone", null, null, null)));
        when(customerDao.update(any(Customer.class))).thenReturn(1);

        CustomerDto dto = service().update(2L, input(" Robert ", "Stone", " ", "rob@example.com", null));

        assertThat(dto).isEqualTo(new CustomerDto(2L, "Robert", "Stone", null, "rob@example.com", null));
        ArgumentCaptor<Customer> sent = ArgumentCaptor.forClass(Customer.class);
        verify(customerDao).update(sent.capture());
        assertThat(sent.getValue().getId()).isEqualTo(2L);
    }

    @Test
    void _19_ShouldThrowResourceNotFoundExceptionWithoutUpdating_WhenCustomerDoesNotExist() {
        when(customerDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(99L, input("Alice", "Martin", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Customer not found: 99");
        verify(customerDao, never()).update(any());
    }

    @Test
    void _20_ShouldThrowResourceNotFoundException_WhenNoRowIsAffectedByTheUpdate() {
        when(customerDao.findById(2L)).thenReturn(Optional.of(new Customer(2L, "Bob", "Stone", null, null, null)));
        when(customerDao.update(any(Customer.class))).thenReturn(0);

        assertThatThrownBy(() -> service().update(2L, input("Bob", "Stone", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Customer not found: 2");
    }

    @Test
    void _21_ShouldThrowBusinessRuleExceptionWithoutUpdating_WhenUpdatedEmailIsInvalid() {
        when(customerDao.findById(2L)).thenReturn(Optional.of(new Customer(2L, "Bob", "Stone", null, null, null)));

        assertThatThrownBy(() -> service().update(2L, withEmail("nope")))
                .isInstanceOf(BusinessRuleException.class);
        verify(customerDao, never()).update(any());
    }

    // ---------------------------------------------------------------- delete

    @Test
    void _22_ShouldDelete_WhenCustomerExistsAndIsNotReferenced() {
        when(customerDao.findById(2L)).thenReturn(Optional.of(new Customer(2L, "Bob", "Stone", null, null, null)));

        service().delete(2L);

        verify(customerDao).delete(2L);
    }

    @Test
    void _23_ShouldThrowResourceNotFoundExceptionWithoutDeleting_WhenCustomerDoesNotExist() {
        when(customerDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(customerDao, never()).delete(any());
    }

    @Test
    void _24_ShouldThrowBusinessRuleException_WhenTheForeignKeyOfOrdersRefusesTheDelete() {
        when(customerDao.findById(2L)).thenReturn(Optional.of(new Customer(2L, "Bob", "Stone", null, null, null)));
        doThrow(new DataIntegrityViolationException("fk_orders_customer")).when(customerDao).delete(2L);

        assertThatThrownBy(() -> service().delete(2L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Customer is referenced by orders");
    }
}
