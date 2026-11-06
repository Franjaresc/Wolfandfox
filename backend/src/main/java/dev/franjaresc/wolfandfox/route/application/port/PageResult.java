package dev.franjaresc.wolfandfox.route.application.port;

import java.util.List;

/** One page of results, so that Spring Data's {@code Page} never crosses a port. */
public record PageResult<T>(List<T> items, PageQuery query, long totalElements) {

    public PageResult {
        items = List.copyOf(items);
        if (totalElements < 0) {
            throw new IllegalArgumentException("Total elements must not be negative: " + totalElements);
        }
    }

    public int totalPages() {
        return Math.toIntExact((totalElements + query.size() - 1) / query.size());
    }
}
