package com.edgareldy.springjdbctutorial.ws.payload.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of PageResponse (total pages arithmetic and size validation).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class PageResponseTest {

    @Test
    void _01_ShouldRoundTotalPagesUp_WhenLastPageIsPartial() {
        PageResponse<String> page = PageResponse.of(List.of("a", "b"), 0, 2, 5);

        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getPage()).isZero();
        assertThat(page.getSize()).isEqualTo(2);
    }

    @Test
    void _02_ShouldHaveZeroTotalPages_WhenThereAreNoElements() {
        assertThat(PageResponse.of(List.of(), 0, 10, 0).getTotalPages()).isZero();
    }

    @Test
    void _03_ShouldHaveExactTotalPages_WhenTotalIsAMultipleOfSize() {
        assertThat(PageResponse.of(List.of("a"), 0, 5, 10).getTotalPages()).isEqualTo(2);
    }

    @Test
    void _04_ShouldThrow_WhenSizeIsZero() {
        assertThatThrownBy(() -> PageResponse.of(List.of(), 0, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void _05_ShouldThrow_WhenSizeIsNegative() {
        assertThatThrownBy(() -> PageResponse.of(List.of(), 0, -3, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void _06_ShouldKeepEmptyContent_WhenPageIsOutOfRange() {
        PageResponse<String> page = PageResponse.of(List.of(), 99, 10, 5);

        assertThat(page.getContent()).isNotNull().isEmpty();
        assertThat(page.getPage()).isEqualTo(99);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }
}
