package com.local.lms.exceptions;

import com.local.lms.dto.response.FieldErrorResponse;
import com.local.lms.dto.response.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Object> handleBusinessException(BusinessException ex) {
        log.error("error ", ex);
        return ResponseResult.response(
                ex.getCode(),
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseResult<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("error ", ex);
        return ResponseResult.response(
                ResponseCode.BAD_REQUEST.getCode(),
                "Request body is missing or invalid JSON",
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<Object> handleValidation(MethodArgumentNotValidException ex) {
        log.error("error ", ex);
        List<FieldErrorResponse> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> new FieldErrorResponse(e.getField(), e.getDefaultMessage()))
                .toList();

        return ResponseResult.response(
                ResponseCode.BAD_REQUEST.getCode(),
                "Validation failed",
                errors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseResult<Object> handleException(Exception ex) {
        log.error("Unexpected error", ex);

        return ResponseResult.response(
                ResponseCode.INTERNAL_ERROR.getCode(),
                "System error occurred",
                null
        );
    }

    @ExceptionHandler(Throwable.class)
    public ResponseResult<Object> handleThrowable(Throwable ex) {
        log.error("Critical error", ex);

        return ResponseResult.response(
                ResponseCode.INTERNAL_ERROR.getCode(),
                "System failure",
                null
        );
    }
}
