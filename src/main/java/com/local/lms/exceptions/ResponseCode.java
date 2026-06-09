package com.local.lms.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResponseCode {
    SUCCESS("200", "success"),
    BAD_REQUEST("400", "Bad Request"),
    NOT_FOUND("404", "Not Found"),
    UNAUTHORIZED("401", "Unauthorized"),
    FORBIDDEN("403", "Forbidden"),
    INTERNAL_ERROR("500", "Internal Server Error");

    private final String code;
    private final String message;
}
