package com.microservice.payment.exception;

/** Thrown when the request is invalid for a business reason (HTTP 400). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
