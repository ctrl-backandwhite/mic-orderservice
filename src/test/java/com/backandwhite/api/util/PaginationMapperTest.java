package com.backandwhite.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.backandwhite.api.dto.PaginationDtoOut;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaginationMapper")
class PaginationMapperTest {

    @Test
    @DisplayName("maps content using the supplied function and preserves pagination metadata")
    void mapsContentAndCopiesMetadata() {
        PaginationDtoOut<Integer> source = PaginationDtoOut.<Integer>builder().content(List.of(1, 2, 3))
                .totalElements(3).totalPages(1).currentPage(0).pageSize(10).hasNext(false).hasPrevious(false).build();

        PaginationDtoOut<String> mapped = PaginationMapper.map(source, n -> "n-" + n);

        assertThat(mapped.getContent()).containsExactly("n-1", "n-2", "n-3");
        assertThat(mapped.getTotalElements()).isEqualTo(3);
        assertThat(mapped.getTotalPages()).isEqualTo(1);
        assertThat(mapped.getCurrentPage()).isZero();
        assertThat(mapped.getPageSize()).isEqualTo(10);
        assertThat(mapped.isHasNext()).isFalse();
        assertThat(mapped.isHasPrevious()).isFalse();
    }

    @Test
    @DisplayName("returns empty content when the source page has no items")
    void mapsEmptyContent() {
        PaginationDtoOut<Integer> source = PaginationDtoOut.<Integer>builder().content(List.of()).totalElements(0)
                .totalPages(0).currentPage(0).pageSize(20).hasNext(false).hasPrevious(false).build();

        PaginationDtoOut<String> mapped = PaginationMapper.map(source, Object::toString);
        assertThat(mapped.getContent()).isEmpty();
    }
}
