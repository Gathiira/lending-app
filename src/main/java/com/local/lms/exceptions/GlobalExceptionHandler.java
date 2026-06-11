package com.local.lms.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.local.lms.dto.response.FieldErrorResponse;
import com.local.lms.dto.response.ResponseResult;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseResult<Object> handleBusinessException(BusinessException ex) {
        log.error("BusinessException ", ex);
        return ResponseResult.response(
                ex.getCode(),
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseResult<Object> resourceNotFoundException(ResourceNotFoundException ex) {
        log.error("ResourceNotFoundException ", ex);
        return ResponseResult.response(
                "400",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseResult<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error("HttpMessageNotReadableException ", ex);
        return ResponseResult.response(
                ResponseCode.BAD_REQUEST.getCode(),
                "Invalid request payload",
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<Object> handleValidation(MethodArgumentNotValidException ex) {
        log.error("MethodArgumentNotValidException ", ex);
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

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseResult<Object> invalidFormatHandler(InvalidFormatException ex) {
        log.error("InvalidFormatException ", ex);
        return ResponseResult.response(
                ResponseCode.BAD_REQUEST.getCode(),
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(UnexpectedTypeException.class)
    public ResponseResult<Object> invalidFormatHandler(UnexpectedTypeException ex) {
        log.error("UnexpectedTypeException ", ex);
        return ResponseResult.response(
                ResponseCode.BAD_REQUEST.getCode(),
                "Invalid request payload",
                null
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseResult<Object> methodNotSupportedHandler(HttpRequestMethodNotSupportedException ex) {
        log.error("HttpRequestMethodNotSupportedException ", ex);
        return ResponseResult.response(
                ResponseCode.BAD_REQUEST.getCode(),
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseResult<Object> methodNotSupportedHandler(MissingServletRequestParameterException ex) {
        log.error("MissingServletRequestParameterException ", ex);
        return ResponseResult.response(
                ResponseCode.BAD_REQUEST.getCode(),
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseResult<Object> handleException(Exception ex) {
        log.error("Unexpected error", ex);

        return ResponseResult.response(
                ResponseCode.INTERNAL_ERROR.getCode(),
                "Failed to process request. Contact support.",
                null
        );
    }

    @ExceptionHandler(Throwable.class)
    public ResponseResult<Object> handleThrowable(Throwable ex) {
        log.error("Critical error", ex);

        return ResponseResult.response(
                ResponseCode.INTERNAL_ERROR.getCode(),
                "System failure. Contact support.",
                null
        );
    }
}
