package com.edgareldy.springjdbctutorial.core.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.catalog.dto.ProductDto;
import com.edgareldy.springjdbctutorial.core.catalog.service.ProductService;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.edgareldy.springjdbctutorial.core.customer.dto.CustomerDto;
import com.edgareldy.springjdbctutorial.core.customer.service.CustomerService;
import com.edgareldy.springjdbctutorial.core.order.dao.OrderDao;
import com.edgareldy.springjdbctutorial.core.order.dto.OrderDto;
import com.edgareldy.springjdbctutorial.core.order.entity.Order;
import java.math.BigDecimal;
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
 * Unit tests of OrderServiceImpl: the DAO and the customer and product SERVICES are Mockito mocks (the order module only sees service interfaces), assertions are made on the dto objects.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// MockitoExtension builds the @Mock fields before each test and fails a test whose stubbing is never used
// (strict stubs), so no test keeps a stub it does not need. No Spring context and no database here.
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderDao orderDao;
    @Mock
    private ProductService productService;
    @Mock
    private CustomerService customerService;

    private OrderServiceImpl service() {
        return new OrderServiceImpl(orderDao, productService, customerService);
    }

    private static OrderDto input(Long customerId, Long productId, int quantity) {
        return new OrderDto(null, customerId, productId, quantity, null);
    }

    private void productAt(String unitPrice) {
        when(productService.get(2L)).thenReturn(new ProductDto(2L, 1L, "Atlas", new BigDecimal(unitPrice)));
    }

    private void insertEchoesWithId() {
        when(orderDao.insert(any(Order.class))).thenAnswer(call -> {
            Order order = call.getArgument(0);
            order.setId(50L);
            return order;
        });
    }

    private Order inserted() {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderDao).insert(captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------- create: total

    @Test
    void _01_ShouldComputeTheExactTotal_WhenQuantityIsThreeAndPriceIs1999() {
        when(customerService.get(1L)).thenReturn(new CustomerDto(1L, "Alice", "Martin", null, null, null));
        productAt("19.99");
        insertEchoesWithId();

        OrderDto created = service().create(input(1L, 2L, 3));

        assertThat(created).isEqualTo(new OrderDto(50L, 1L, 2L, 3, new BigDecimal("59.97")));
        assertThat(inserted().getTotal()).isEqualTo(new BigDecimal("59.97"));
    }

    @Test
    void _02_ShouldStoreTotalZeroWithTwoDecimals_WhenProductIsFree() {
        productAt("0.00");
        insertEchoesWithId();

        service().create(input(1L, 2L, 1));

        assertThat(inserted().getTotal()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void _03_ShouldAcceptTheLargestTotal_WhenMillionUnitsAtTheHighestSafePrice() {
        productAt("999999.99");
        insertEchoesWithId();

        service().create(input(1L, 2L, 1_000_000));

        assertThat(inserted().getTotal()).isEqualTo(new BigDecimal("999999990000.00"));
    }

    @Test
    void _04_ShouldRefuseAndInsertNothing_WhenTotalExceedsTheColumnLimit() {
        productAt("1000000.00");

        assertThatThrownBy(() -> service().create(input(1L, 2L, 1_000_000)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Order total must not exceed 999999999999.99");

        verify(orderDao, never()).insert(any());
    }

    @Test
    void _05_ShouldRefuseAndInsertNothing_WhenTotalIsJustAboveTheLimit() {
        productAt("1000000000000.00");

        assertThatThrownBy(() -> service().create(input(1L, 2L, 1))).isInstanceOf(BusinessRuleException.class);

        verify(orderDao, never()).insert(any());
    }

    @Test
    void _06_ShouldRoundHalfUpToTwoDecimals_WhenProductPriceHasMoreDecimals() {
        productAt("0.005");
        insertEchoesWithId();

        service().create(input(1L, 2L, 1));

        assertThat(inserted().getTotal()).isEqualTo(new BigDecimal("0.01"));
    }

    // ---------------------------------------------------------------- create: quantity and ids

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 1_000_001})
    void _07_ShouldRefuseAndTouchNothing_WhenQuantityIsOutOfRange(int quantity) {
        assertThatThrownBy(() -> service().create(input(1L, 2L, quantity)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Quantity must be between 1 and 1000000");

        verifyNoInteractions(orderDao, productService, customerService);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 1_000_000})
    void _08_ShouldAcceptTheBoundary_WhenQuantityIsOneOrAMillion(int quantity) {
        productAt("0.50");
        insertEchoesWithId();

        service().create(input(1L, 2L, quantity));

        assertThat(inserted().getQuantity()).isEqualTo(quantity);
    }

    @Test
    void _09_ShouldRefuseAndTouchNothing_WhenCustomerIdIsNull() {
        assertThatThrownBy(() -> service().create(input(null, 2L, 1)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Customer id must not be null");

        verifyNoInteractions(orderDao, productService, customerService);
    }

    @Test
    void _10_ShouldRefuseAndTouchNothing_WhenProductIdIsNull() {
        assertThatThrownBy(() -> service().create(input(1L, null, 1)))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Product id must not be null");

        verifyNoInteractions(orderDao, productService, customerService);
    }

    // ---------------------------------------------------------------- create: other modules

    @Test
    void _11_ShouldPropagateNotFoundAndNotCallTheProductService_WhenCustomerIsUnknown() {
        when(customerService.get(9L)).thenThrow(new ResourceNotFoundException("Customer not found: 9"));

        assertThatThrownBy(() -> service().create(input(9L, 2L, 1)))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Customer not found: 9");

        verifyNoInteractions(productService, orderDao);
    }

    @Test
    void _12_ShouldPropagateNotFoundAndInsertNothing_WhenProductIsUnknown() {
        when(customerService.get(1L)).thenReturn(new CustomerDto(1L, "Alice", "Martin", null, null, null));
        when(productService.get(9L)).thenThrow(new ResourceNotFoundException("Product not found: 9"));

        assertThatThrownBy(() -> service().create(input(1L, 9L, 1)))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Product not found: 9");

        verifyNoInteractions(orderDao);
    }

    @Test
    void _13_ShouldTurnIntoNotFound_WhenTheForeignKeyFailsAtInsert() {
        productAt("10.00");
        when(orderDao.insert(any(Order.class))).thenThrow(new DataIntegrityViolationException("fk"));

        assertThatThrownBy(() -> service().create(input(1L, 2L, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer or product no longer exists");
    }

    // ---------------------------------------------------------------- list and get

    @Test
    void _14_ShouldReturnDtosAndPassFiltersToTheDao_WhenListingWithFilters() {
        when(orderDao.findPage(1, 5, 3L, 4L)).thenReturn(List.of(new Order(9L, 3L, 4L, 2, new BigDecimal("5.00"))));
        when(orderDao.count(3L, 4L)).thenReturn(6L);

        PageDto<OrderDto> page = service().list(1, 5, 3L, 4L);

        assertThat(page.getContent()).containsExactly(new OrderDto(9L, 3L, 4L, 2, new BigDecimal("5.00")));
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(5);
        assertThat(page.getTotalElements()).isEqualTo(6L);
    }

    @Test
    void _15_ShouldPassNullFiltersToTheDao_WhenListingWithoutFilters() {
        when(orderDao.findPage(0, 20, null, null)).thenReturn(List.of());
        when(orderDao.count(null, null)).thenReturn(0L);

        assertThat(service().list(0, 20, null, null).getContent()).isEmpty();
    }

    @Test
    void _16_ShouldKeepEmptyContentAndTotal_WhenPageIsBeyondTheLastOne() {
        when(orderDao.findPage(99, 10, null, null)).thenReturn(List.of());
        when(orderDao.count(null, null)).thenReturn(3L);

        PageDto<OrderDto> page = service().list(99, 10, null, null);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3L);
    }

    @Test
    void _17_ShouldRefuseAndNotQuery_WhenPageIsNegative() {
        assertThatThrownBy(() -> service().list(-1, 10, null, null)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(orderDao);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void _18_ShouldRefuseAndNotQuery_WhenSizeIsOutOfBounds(int size) {
        assertThatThrownBy(() -> service().list(0, size, null, null)).isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(orderDao);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void _19_ShouldAcceptTheBoundarySizes_WhenSizeIsOneOrOneHundred(int size) {
        when(orderDao.findPage(0, size, null, null)).thenReturn(List.of());
        when(orderDao.count(null, null)).thenReturn(0L);

        assertThat(service().list(0, size, null, null).getSize()).isEqualTo(size);
    }

    @Test
    void _20_ShouldReturnTheDto_WhenOrderExists() {
        when(orderDao.findById(9L)).thenReturn(Optional.of(new Order(9L, 3L, 4L, 2, new BigDecimal("5.00"))));

        assertThat(service().get(9L)).isEqualTo(new OrderDto(9L, 3L, 4L, 2, new BigDecimal("5.00")));
    }

    @Test
    void _21_ShouldThrowNotFound_WhenOrderDoesNotExist() {
        when(orderDao.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(99L))
                .isInstanceOf(ResourceNotFoundException.class).hasMessage("Order not found: 99");
    }
}
