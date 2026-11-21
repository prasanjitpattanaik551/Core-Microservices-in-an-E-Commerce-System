package com.microservice.common.security;

/** The logged-in user, built from a validated JWT. Used as the Spring Security principal. */
public class AuthenticatedUser {

    private final Long userId;
    private final String email;
    private final String role;

    public AuthenticatedUser(Long userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = (role == null || role.isBlank()) ? "USER" : role;
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isAdmin() { return "ADMIN".equals(role); }
}
