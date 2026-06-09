package com.local.lms.controller;

import com.local.lms.core.RequestUtil;
import com.local.lms.security.JwtContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

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

}
