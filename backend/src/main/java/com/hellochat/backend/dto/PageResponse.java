package com.hellochat.backend.dto;

import java.util.List;

public class PageResponse<T> {

    private final List<T> list;
    private final int page;
    private final int pageSize;
    private final long total;
    private final boolean hasMore;

    public PageResponse(List<T> list, int page, int pageSize, long total) {
        this.list = list;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.hasMore = (long) page * pageSize < total;
    }

    public List<T> getList() {
        return list;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }

    public boolean isHasMore() {
        return hasMore;
    }
}
