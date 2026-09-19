package com.edgareldy.springjdbctutorial.core.common.dto;

import java.util.List;
import java.util.Objects;

/**
 * One page of a paginated list, as returned by the services. The web layer converts it to its own response type.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PageDto<T> {

    private final List<T> content;

    private final int page;

    private final int size;

    private final long totalElements;

    public PageDto(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PageDto<?> that = (PageDto<?>) o;
        return page == that.page && size == that.size && totalElements == that.totalElements
                && Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, page, size, totalElements);
    }

    @Override
    public String toString() {
        return "PageDto{" +
                "content=" + content +
                ", page=" + page +
                ", size=" + size +
                ", totalElements=" + totalElements +
                "}";
    }
}
