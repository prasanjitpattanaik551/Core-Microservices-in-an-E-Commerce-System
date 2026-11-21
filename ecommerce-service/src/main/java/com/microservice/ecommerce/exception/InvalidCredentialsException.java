package com.microservice.ecommerce.exception;

/** Thrown when login fails (HTTP 401). */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
