package com.company.ops.api.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void mapsSpringPageToStableApiContract() {
        var page = new PageImpl<>(List.of("first", "second"), PageRequest.of(2, 2), 7);

        var response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("first", "second");
        assertThat(response.number()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(7);
        assertThat(response.totalPages()).isEqualTo(4);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
        assertThat(response.numberOfElements()).isEqualTo(2);
        assertThat(response.empty()).isFalse();
    }
}
