package com.local.lms.controller;

import com.local.lms.core.RequestUtil;
import com.local.lms.security.JwtContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Slf4j
public class BaseController {

    @Resource
    private JwtContext jwtContext;

    protected Integer getPageNo() {
        return RequestUtil.getParaToInt("current", 1);
    }

    protected Integer getPageSize() {
        return RequestUtil.getParaToInt("pageSize", 10);
    }

    protected Long getUserId() {
        return jwtContext.getUserId();
    }

    protected Long getCustomerId() {
        return jwtContext.getCustomerId();
    }

    protected Pageable getPageable() {
        int page = Math.max(getPageNo(), 1) - 1;
        int size = Math.min(Math.max(getPageSize(), 1), 20);
        return PageRequest.of(page,size);
    }

}
