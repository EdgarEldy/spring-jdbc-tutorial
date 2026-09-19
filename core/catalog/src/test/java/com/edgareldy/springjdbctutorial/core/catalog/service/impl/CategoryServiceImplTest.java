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
import com.edgareldy.springjdbctutorial.core.catalog.dto.CategoryDto;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Category;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

/**
 * Unit tests of CategoryServiceImpl: both DAOs are Mockito mocks, assertions are made on the dto objects.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// MockitoExtension builds the @Mock fields before each test and fails a test whose stubbing is never used
// (strict stubs), so no test keeps a stub it does not need. No Spring context and no database here.
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryDao categoryDao;
    @Mock
    private ProductDao productDao;

    private CategoryServiceImpl service() {
        return new CategoryServiceImpl(categoryDao, productDao);
    }

    private static CategoryDto input(String name) {
        return new CategoryDto(null, name);
    }

    // ---------------------------------------------------------------- list

    @Test
    void _01_ShouldReturnPageOfDtos_WhenListing() {
        when(categoryDao.findPage(1, 2)).thenReturn(List.of(new Category(3L, "Toys")));
        when(categoryDao.countAll()).thenReturn(3L);

        PageDto<CategoryDto> page = service().list(1, 2);

        assertThat(page.getContent()).containsExactly(new CategoryDto(3L, "Toys"));
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(3L);
    }

    @Test
    void _02_ShouldReturnEmptyContent_WhenPageIsBeyondTheLastOne() {
        when(categoryDao.findPage(50, 10)).thenReturn(List.of());
        when(categoryDao.countAll()).thenReturn(3L);

        PageDto<CategoryDto> page = service().list(50, 10);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3L);
    }

    @Test
    void _03_ShouldThrowBusinessRuleException_WhenPageIsNegative() {
        assertThatThrownBy(() -> service().list(-1, 10)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(categoryDao);
    }

    @Test
    void _04_ShouldThrowBusinessRuleException_WhenSizeIsZero() {
        assertThatThrownBy(() -> service().list(0, 0)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(categoryDao);
    }

    @Test
    void _05_ShouldThrowBusinessRuleException_WhenSizeIsAbove100() {
        assertThatThrownBy(() -> service().list(0, 101)).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(categoryDao);
    }

    @Test
    void _06_ShouldAcceptBoundarySizes_WhenSizeIs1Or100() {
        when(categoryDao.findPage(0, 1)).thenReturn(List.of());
        when(categoryDao.findPage(0, 100)).thenReturn(List.of());

        assertThat(service().list(0, 1).getSize()).isEqualTo(1);
        assertThat(service().list(0, 100).getSize()).isEqualTo(100);
    }

    // ---------------------------------------------------------------- get

    @Test
    void _07_ShouldReturnDto_WhenCategoryExists() {
        when(categoryDao.findById(2L)).thenReturn(Optional.of(new Category(2L, "Games")));

        assertThat(service().get(2L)).isEqualTo(new CategoryDto(2L, "Games"));
    }

    @Test
    void _08_ShouldThrowResourceNotFoundException_WhenCategoryDoesNotExist() {
        when(categoryDao.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().get(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------- create

    @Test
    void _09_ShouldInsertTrimmedNameAndReturnDto_WhenNameIsNew() {
        when(categoryDao.existsByCategoryName("Books")).thenReturn(false);
        when(categoryDao.insert(any(Category.class))).thenAnswer(invocation -> {
            Category created = invocation.getArgument(0);
            created.setId(11L);
            return created;
        });

        CategoryDto created = service().create(input("  Books  "));

        assertThat(created).isEqualTo(new CategoryDto(11L, "Books"));
        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryDao).insert(saved.capture());
        assertThat(saved.getValue().getCategoryName()).isEqualTo("Books");
    }

    @Test
    void _10_ShouldThrowBusinessRuleException_WhenNameIsBlank() {
        assertThatThrownBy(() -> service().create(input("   "))).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(categoryDao);
    }

    @Test
    void _11_ShouldThrowBusinessRuleException_WhenNameIsNull() {
        assertThatThrownBy(() -> service().create(input(null))).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(categoryDao);
    }

    @Test
    void _12_ShouldThrowBusinessRuleException_WhenNameIsLongerThan100Characters() {
        assertThatThrownBy(() -> service().create(input("x".repeat(101)))).isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(categoryDao);
    }

    @Test
    void _13_ShouldAcceptName_WhenExactly100Characters() {
        String name = "x".repeat(100);
        when(categoryDao.existsByCategoryName(name)).thenReturn(false);
        when(categoryDao.insert(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service().create(input(name)).getCategoryName()).isEqualTo(name);
    }

    @Test
    void _14_ShouldThrowBusinessRuleExceptionWithoutInserting_WhenNameAlreadyExists() {
        when(categoryDao.existsByCategoryName("Books")).thenReturn(true);

        assertThatThrownBy(() -> service().create(input("Books")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Category name already exists");
        verify(categoryDao, never()).insert(any());
    }

    @Test
    void _15_ShouldTurnDuplicateKeyExceptionIntoBusinessRuleException_WhenAConcurrentInsertWonTheRace() {
        DuplicateKeyException race = new DuplicateKeyException("uq_categories_category_name");
        when(categoryDao.existsByCategoryName("Books")).thenReturn(false);
        when(categoryDao.insert(any(Category.class))).thenThrow(race);

        assertThatThrownBy(() -> service().create(input("Books")))
                .isInstanceOf(BusinessRuleException.class).hasCause(race);
    }

    // ---------------------------------------------------------------- update

    @Test
    void _16_ShouldRenameAndReturnDto_WhenNameIsFree() {
        when(categoryDao.findById(2L)).thenReturn(Optional.of(new Category(2L, "Games")));
        when(categoryDao.existsByCategoryName("Video games")).thenReturn(false);

        CategoryDto updated = service().update(2L, input(" Video games "));

        assertThat(updated).isEqualTo(new CategoryDto(2L, "Video games"));
        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryDao).update(saved.capture());
        assertThat(saved.getValue().getCategoryName()).isEqualTo("Video games");
    }

    @Test
    void _17_ShouldThrowResourceNotFoundException_WhenUpdatingAnUnknownCategory() {
        when(categoryDao.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(9L, input("Books"))).isInstanceOf(ResourceNotFoundException.class);
        verify(categoryDao, never()).update(any());
    }

    @Test
    void _18_ShouldAllowIt_WhenTheNameIsUnchanged() {
        when(categoryDao.findById(2L)).thenReturn(Optional.of(new Category(2L, "Games")));

        CategoryDto updated = service().update(2L, input("Games"));

        assertThat(updated.getCategoryName()).isEqualTo("Games");
        // The name is the category's own: no uniqueness lookup, so it never collides with itself
        verify(categoryDao, never()).existsByCategoryName(any());
        verify(categoryDao).update(any(Category.class));
    }

    @Test
    void _19_ShouldThrowBusinessRuleException_WhenRenamingToAnotherCategoryName() {
        when(categoryDao.findById(2L)).thenReturn(Optional.of(new Category(2L, "Games")));
        when(categoryDao.existsByCategoryName("Books")).thenReturn(true);

        assertThatThrownBy(() -> service().update(2L, input("Books")))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Category name already exists");
        verify(categoryDao, never()).update(any());
    }

    @Test
    void _20_ShouldThrowBusinessRuleException_WhenUpdatedNameIsBlank() {
        when(categoryDao.findById(2L)).thenReturn(Optional.of(new Category(2L, "Games")));

        assertThatThrownBy(() -> service().update(2L, input(" "))).isInstanceOf(BusinessRuleException.class);
        verify(categoryDao, never()).update(any());
    }

    @Test
    void _21_ShouldTurnDuplicateKeyExceptionIntoBusinessRuleException_WhenRenameLosesTheRace() {
        DuplicateKeyException race = new DuplicateKeyException("uq_categories_category_name");
        when(categoryDao.findById(2L)).thenReturn(Optional.of(new Category(2L, "Games")));
        when(categoryDao.existsByCategoryName("Books")).thenReturn(false);
        doThrow(race).when(categoryDao).update(any(Category.class));

        assertThatThrownBy(() -> service().update(2L, input("Books")))
                .isInstanceOf(BusinessRuleException.class).hasCause(race);
    }

    // ---------------------------------------------------------------- delete

    @Test
    void _22_ShouldDeleteCategory_WhenNoProductReferencesIt() {
        when(categoryDao.findById(2L)).thenReturn(Optional.of(new Category(2L, "Games")));
        when(productDao.countByCategoryId(2L)).thenReturn(0L);

        service().delete(2L);

        verify(categoryDao).delete(2L);
    }

    @Test
    void _23_ShouldThrowResourceNotFoundException_WhenDeletingAnUnknownCategory() {
        when(categoryDao.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        verify(categoryDao, never()).delete(any());
    }

    @Test
    void _24_ShouldThrowBusinessRuleExceptionWithoutDeleting_WhenProductsStillReferenceTheCategory() {
        when(categoryDao.findById(2L)).thenReturn(Optional.of(new Category(2L, "Games")));
        when(productDao.countByCategoryId(2L)).thenReturn(2L);

        assertThatThrownBy(() -> service().delete(2L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Category still has products");
        verify(categoryDao, never()).delete(any());
    }

    @Test
    void _25_ShouldTurnDataIntegrityViolationIntoBusinessRuleException_WhenAProductAppearsBetweenCheckAndDelete() {
        DataIntegrityViolationException race = new DataIntegrityViolationException("fk_products_category");
        when(categoryDao.findById(2L)).thenReturn(Optional.of(new Category(2L, "Games")));
        when(productDao.countByCategoryId(2L)).thenReturn(0L);
        doThrow(race).when(categoryDao).delete(2L);

        assertThatThrownBy(() -> service().delete(2L))
                .isInstanceOf(BusinessRuleException.class).hasCause(race);
    }
}
