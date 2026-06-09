package com.local.lms.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    public ResourceNotFoundException(String message, Long Id) {
        super(message + " not found with id " + Id);
    }
}
