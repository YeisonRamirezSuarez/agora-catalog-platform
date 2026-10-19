package com.agora.catalog.common.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int totalElements;
    private final int totalPages;

    private final int page;
    private final int size;

    private PageResponse(List<T> content, int totalElements, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    }

    public static <T> PageResponse<T> of(List<T> content, int totalElements, int page, int size) {
        return new PageResponse<>(content, totalElements, page, size);
    }
}
