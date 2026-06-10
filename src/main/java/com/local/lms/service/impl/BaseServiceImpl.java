package com.local.lms.service.impl;

import com.local.lms.core.GenericSpecificationBuilder;
import com.local.lms.core.SearchRule;
import com.local.lms.dto.response.PaginatedResponse;
import com.local.lms.mapper.FilterMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;
import java.util.function.Function;

public class BaseServiceImpl<E> {

    private final GenericSpecificationBuilder<E> builder = new GenericSpecificationBuilder<>();

    public <T, R> PaginatedResponse<R> toResponse(Page<T> page, Function<T, R> mapper) {
        PaginatedResponse<R> response = new PaginatedResponse<>();
        response.setCurrent(page.getNumber() + 1);
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setList(page.getContent().stream().map(mapper).toList());
        return response;
    }

    public <T> Specification<E> getSpecifications(T request) {
        Map<String, SearchRule> filters = FilterMapper.toFilters(request);
        return builder.build(filters);
    }
}
