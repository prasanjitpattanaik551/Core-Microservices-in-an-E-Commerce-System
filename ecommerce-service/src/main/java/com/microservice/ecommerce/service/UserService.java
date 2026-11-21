package com.microservice.ecommerce.service;

import com.microservice.common.security.JwtUtil;
import com.microservice.ecommerce.dto.AuthDtos.AuthResponse;
import com.microservice.ecommerce.dto.AuthDtos.LoginRequest;
import com.microservice.ecommerce.dto.AuthDtos.RegisterRequest;
import com.microservice.ecommerce.exception.DuplicateResourceException;
import com.microservice.ecommerce.exception.InvalidCredentialsException;
import com.microservice.ecommerce.model.User;
import com.microservice.ecommerce.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /** Input is already validated by @Valid on the controller. */
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.findByEmail(email) != null) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password())); // BCrypt hash
        user.setRole("USER");                                      // role can never come from the client
        return toResponse(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().trim().toLowerCase());
        // same message for unknown email and wrong password, so accounts can't be enumerated
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        return toResponse(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private AuthResponse toResponse(User user) {
        String role = user.getRole() == null ? "USER" : user.getRole();
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), role);
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), role);
    }
}
