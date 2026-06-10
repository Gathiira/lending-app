package com.local.lms.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PaginatedResponse<T> {
    private Integer current;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private List<T> list;
}
