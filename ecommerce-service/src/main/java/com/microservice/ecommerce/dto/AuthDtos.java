package com.microservice.ecommerce.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must be at most 100 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Email is not valid")
            @Size(max = 150, message = "Email must be at most 150 characters")
            String email,

            // BCrypt only uses the first 72 bytes, so longer passwords are rejected on purpose
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
            String password) {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email is not valid")
            String email,

            @NotBlank(message = "Password is required")
            String password) {}

    public record AuthResponse(String token, Long id, String name, String email, String role) {}
}
