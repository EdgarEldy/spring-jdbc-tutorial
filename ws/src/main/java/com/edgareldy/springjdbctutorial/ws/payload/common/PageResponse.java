package com.edgareldy.springjdbctutorial.ws.payload.common;

import java.util.List;

/**
 * Paged list payload. size must be at least 1 so totalPages is never a fake value.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PageResponse() {
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        PageResponse<T> response = new PageResponse<>();
        response.content = content;
        response.page = page;
        response.size = size;
        response.totalElements = totalElements;
        response.totalPages = (int) ((totalElements + size - 1) / size);
        return response;
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
