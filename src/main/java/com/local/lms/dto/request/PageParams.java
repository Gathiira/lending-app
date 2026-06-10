package com.local.lms.dto.request;


public class PageParams {
    Integer page = 1;
    Integer size = 10;

    public Integer getOffset() {
        return page > 0 ? (page - 1) * size : 0;
    }
}
