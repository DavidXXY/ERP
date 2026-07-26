package com.company.ops.api.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable API pagination contract independent of Spring Data's serialization format. */
public record PageResponse<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int number,
    int size,
    int numberOfElements,
    boolean first,
    boolean last,
    boolean empty
) {
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        List.copyOf(page.getContent()),
        page.getTotalElements(),
        page.getTotalPages(),
        page.getNumber(),
        page.getSize(),
        page.getNumberOfElements(),
        page.isFirst(),
        page.isLast(),
        page.isEmpty());
  }
}
