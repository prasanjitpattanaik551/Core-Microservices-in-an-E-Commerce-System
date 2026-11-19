package com.microservice.ecommerce.service;

import com.microservice.common.security.JwtUtil;
import com.microservice.ecommerce.dto.AuthDtos.AuthResponse;
import com.microservice.ecommerce.dto.AuthDtos.LoginRequest;
import com.microservice.ecommerce.dto.AuthDtos.RegisterRequest;
import com.microservice.ecommerce.model.User;
import com.microservice.ecommerce.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest req) {
        if (req == null || isBlank(req.name()) || isBlank(req.email()) || isBlank(req.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name, email and password are required");
        }
        String email = req.email().trim().toLowerCase();
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email");
        }
        if (req.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        if (userRepository.findByEmail(email) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(req.password())); // BCrypt hash
        user.setRole("USER");                                      // role can never come from the client
        return toResponse(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest req) {
        if (req == null || isBlank(req.email()) || isBlank(req.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email and password are required");
        }
        User user = userRepository.findByEmail(req.email().trim().toLowerCase());
        // same message for unknown email and wrong password, so accounts can't be enumerated
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
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

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}