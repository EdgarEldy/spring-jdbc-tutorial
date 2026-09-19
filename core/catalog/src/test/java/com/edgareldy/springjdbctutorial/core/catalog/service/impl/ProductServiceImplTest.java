package com.edgareldy.springjdbctutorial.core.catalog.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.catalog.dao.CategoryDao;
import com.edgareldy.springjdbctutorial.core.catalog.dao.ProductDao;
import com.edgareldy.springjdbctutorial.core.catalog.dto.ProductDto;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Category;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Product;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Unit tests of ProductServiceImpl: both DAOs are Mockito mocks, assertions are made on the dto objects.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductDao productDao;
    @Mock
    private CategoryDao categoryDao;

    private ProductServiceImpl service() {
        return new ProductServiceImpl(productDao, categoryDao);
    }

    private static ProductDto input(Long categoryId, String name, String price) {
        return new ProductDto(null, categoryId, name, price == null ? null : new BigDecimal(price));
    }

    private void categoryExists(long id) {
        when(categoryDao.findById(id)).thenReturn(Optional.of(new Category(id, "Games")));
    }

    private void insertAssignsId(long id) {
        when(productDao.insert(any(Product.class))).thenAnswer(invocation -> {
            Product created = invocation.getArgument(0);
            created.setId(id);
            return created;
        });
    }

    // ---------------------------------------------------------------- list

    @Test
    void _01_ShouldReturnPageOfDtosAndPassTheFilter_WhenListingByCategory() {
        when(productDao.findPage(0, 10, 2L))
                .thenReturn(List.of(new Product(3L, 2L, "Chess", new BigDecimal("25.99"))));
        when(productDao.count(2L)).thenReturn(1L);

        PageDto<ProductDto> page = service().list(0, 10, 2L);

        assertThat(page.getContent()).containsExactly(new ProductDto(3L, 2L, "Chess", new BigDecimal("25.99")));
        assertThat(page.getTotalElements()).isEqualTo(1L);
    }

    @Test
    void _02_ShouldPassNullFilter_WhenNoCategoryIsGiven() {
        when(productDao.findPage(0, 20, null)).thenReturn(List.of());
        when(productDao.count(null)).thenReturn(4L);

        PageDto<ProductDto> page = service().list(0, 20, null);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(4L);
    }

    @Test
    void _03_ShouldThrowBusinessRuleException_WhenPageIsNegative() {
        assertThatThrownBy(() -> service().list(-1, 10, null)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(productDao);
    }

    @Test
    void _04_ShouldThrowBusinessRuleException_WhenSizeIsZero() {
        assertThatThrownBy(() -> service().list(0, 0, null)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(productDao);
    }

    @Test
    void _05_ShouldThrowBusinessRuleException_WhenSizeIsAbove100() {
        assertThatThrownBy(() -> service().list(0, 101, null)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(productDao);
    }

    @Test
    void _06_ShouldAcceptBoundarySizes_WhenSizeIs1Or100() {
        when(productDao.findPage(0, 1, null)).thenReturn(List.of());
        when(productDao.findPage(0, 100, null)).thenReturn(List.of());

        assertThat(service().list(0, 1, null).getSize()).isEqualTo(1);
        assertThat(service().list(0, 100, null).getSize()).isEqualTo(100);
    }

    // ---------------------------------------------------------------- get

    @Test
    void _07_ShouldReturnDto_WhenProductExists() {
        when(productDao.findById(3L)).thenReturn(Optional.of(new Product(3L, 2L, "Chess", new BigDecimal("25.99"))));

        assertThat(service().get(3L)).isEqualTo(new ProductDto(3L, 2L, "Chess", new BigDecimal("25.99")));
    }

    @Test
    void _08_ShouldThrowResourceNotFoundException_WhenProductDoesNotExist() {
        when(productDao.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------- create

    @Test
    void _09_ShouldInsertTrimmedNameAndReturnDto_WhenInputIsValid() {
        categoryExists(2L);
        insertAssignsId(11L);

        ProductDto created = service().create(input(2L, "  Chess  ", "25.99"));

        assertThat(created).isEqualTo(new ProductDto(11L, 2L, "Chess", new BigDecimal("25.99")));
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productDao).insert(saved.capture());
        assertThat(saved.getValue().getProductName()).isEqualTo("Chess");
        assertThat(saved.getValue().getCategoryId()).isEqualTo(2L);
    }

    @Test
    void _10_ShouldThrowResourceNotFoundException_WhenCategoryDoesNotExist() {
        when(categoryDao.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(input(999L, "Chess", "25.99")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productDao, never()).insert(any());
    }

    @Test
    void _11_ShouldThrowResourceNotFoundException_WhenCategoryIdIsNull() {
        assertThatThrownBy(() -> service().create(input(null, "Chess", "25.99")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productDao, never()).insert(any());
    }

    @Test
    void _12_ShouldThrowBusinessRuleException_WhenNameIsBlank() {
        assertThatThrownBy(() -> service().create(input(2L, "  ", "25.99"))).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(productDao);
    }

    @Test
    void _13_ShouldThrowBusinessRuleException_WhenNameIsLongerThan150Characters() {
        assertThatThrownBy(() -> service().create(input(2L, "x".repeat(151), "25.99")))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(productDao);
    }

    @Test
    void _14_ShouldAcceptName_WhenExactly150Characters() {
        categoryExists(2L);
        insertAssignsId(11L);

        assertThat(service().create(input(2L, "x".repeat(150), "1.00")).getProductName()).hasSize(150);
    }

    @Test
    void _15_ShouldThrowBusinessRuleException_WhenPriceIsNull() {
        assertThatThrownBy(() -> service().create(input(2L, "Chess", null))).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(productDao);
    }

    @Test
    void _16_ShouldThrowBusinessRuleException_WhenPriceIsNegative() {
        assertThatThrownBy(() -> service().create(input(2L, "Chess", "-0.01")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("negative");
        verifyNoInteractions(productDao);
    }

    @Test
    void _17_ShouldThrowBusinessRuleException_WhenPriceHasMoreThanTwoDecimals() {
        assertThatThrownBy(() -> service().create(input(2L, "Chess", "1.005")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("2 decimals");
        verifyNoInteractions(productDao);
    }

    @Test
    void _18_ShouldNormaliseScaleToTwo_WhenPriceHasFewerDecimals() {
        categoryExists(2L);
        insertAssignsId(11L);

        ProductDto created = service().create(input(2L, "Chess", "12.5"));

        assertThat(created.getUnitPrice()).isEqualTo(new BigDecimal("12.50"));
        assertThat(created.getUnitPrice().scale()).isEqualTo(2);
    }

    @Test
    void _19_ShouldAcceptPrice_WhenExtraDecimalsAreOnlyTrailingZeros() {
        categoryExists(2L);
        insertAssignsId(11L);

        ProductDto created = service().create(input(2L, "Chess", "1.500"));

        assertThat(created.getUnitPrice()).isEqualTo(new BigDecimal("1.50"));
    }

    @Test
    void _20_ShouldAcceptZeroAndMaximumPrices_WhenOnTheBoundaries() {
        categoryExists(2L);
        insertAssignsId(11L);

        assertThat(service().create(input(2L, "Free", "0")).getUnitPrice()).isEqualTo(new BigDecimal("0.00"));
        assertThat(service().create(input(2L, "Gold", "9999999999.99")).getUnitPrice())
                .isEqualTo(new BigDecimal("9999999999.99"));
    }

    @Test
    void _21_ShouldThrowBusinessRuleException_WhenPriceExceedsTheColumnCapacity() {
        assertThatThrownBy(() -> service().create(input(2L, "Chess", "10000000000.00")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("exceed");
        verifyNoInteractions(productDao);
    }

    // ---------------------------------------------------------------- update

    @Test
    void _22_ShouldUpdateEveryFieldAndReturnDto_WhenInputIsValid() {
        when(productDao.findById(3L)).thenReturn(Optional.of(new Product(3L, 2L, "Chess", new BigDecimal("25.99"))));
        categoryExists(1L);

        ProductDto updated = service().update(3L, input(1L, " Atlas ", "30"));

        assertThat(updated).isEqualTo(new ProductDto(3L, 1L, "Atlas", new BigDecimal("30.00")));
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productDao).update(saved.capture());
        assertThat(saved.getValue().getCategoryId()).isEqualTo(1L);
        assertThat(saved.getValue().getProductName()).isEqualTo("Atlas");
    }

    @Test
    void _23_ShouldThrowResourceNotFoundException_WhenUpdatingAnUnknownProduct() {
        when(productDao.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(9L, input(2L, "Chess", "1.00")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productDao, never()).update(any());
    }

    @Test
    void _24_ShouldThrowResourceNotFoundException_WhenUpdatingToAnUnknownCategory() {
        when(productDao.findById(3L)).thenReturn(Optional.of(new Product(3L, 2L, "Chess", new BigDecimal("25.99"))));
        when(categoryDao.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(3L, input(999L, "Chess", "1.00")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productDao, never()).update(any());
    }

    @Test
    void _25_ShouldThrowBusinessRuleException_WhenUpdatedPriceIsInvalid() {
        when(productDao.findById(3L)).thenReturn(Optional.of(new Product(3L, 2L, "Chess", new BigDecimal("25.99"))));

        assertThatThrownBy(() -> service().update(3L, input(2L, "Chess", "-1")))
                .isInstanceOf(BusinessRuleException.class);
        verify(productDao, never()).update(any());
    }

    // ---------------------------------------------------------------- delete

    @Test
    void _26_ShouldDeleteProduct_WhenItExists() {
        when(productDao.findById(3L)).thenReturn(Optional.of(new Product(3L, 2L, "Chess", new BigDecimal("25.99"))));

        service().delete(3L);

        verify(productDao).delete(3L);
    }

    @Test
    void _27_ShouldThrowResourceNotFoundException_WhenDeletingAnUnknownProduct() {
        when(productDao.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        verify(productDao, never()).delete(any());
    }

    @Test
    void _28_ShouldTurnDataIntegrityViolationIntoBusinessRuleException_WhenAnOrderReferencesTheProduct() {
        DataIntegrityViolationException fk = new DataIntegrityViolationException("fk_orders_product");
        when(productDao.findById(3L)).thenReturn(Optional.of(new Product(3L, 2L, "Chess", new BigDecimal("25.99"))));
        doThrow(fk).when(productDao).delete(3L);

        assertThatThrownBy(() -> service().delete(3L))
                .isInstanceOf(BusinessRuleException.class).hasCause(fk);
    }
}
