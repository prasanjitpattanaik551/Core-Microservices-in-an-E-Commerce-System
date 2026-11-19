package com.microservice.ecommerce.dto;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(String name, String email, String password) {}

    public record LoginRequest(String email, String password) {}

    public record AuthResponse(String token, Long id, String name, String email, String role) {}
}