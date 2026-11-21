package com.microservice.ecommerce.exception;

/** Thrown when a unique value already exists, e.g. a registered email (HTTP 409). */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
