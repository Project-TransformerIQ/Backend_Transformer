// src/main/java/com/example/transformer/controller/AuthController.java
package com.example.transformer.controller;

import com.example.transformer.dto.CreateUserRequest;
import com.example.transformer.dto.LoginRequest;
import com.example.transformer.dto.LoginResponse;
import com.example.transformer.exception.NotFoundException;
import com.example.transformer.model.AppUser;
import com.example.transformer.model.UserOccupation;
import com.example.transformer.repository.AppUserRepository;
import com.example.transformer.security.CurrentUserHolder;
import com.example.transformer.security.SessionService;
import com.example.transformer.security.SessionUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    public AuthController(AppUserRepository users,
                          PasswordEncoder passwordEncoder,
                          SessionService sessionService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest req) {
        AppUser user = users.findByName(req.name())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        boolean isAdmin = (user.getOccupation() == UserOccupation.ADMIN);
        SessionUser sessionUser = new SessionUser(
                user.getId(),
                user.getName(),
                user.getOccupation(),
                isAdmin
        );
        String token = sessionService.createSession(sessionUser);

        return ResponseEntity.ok(
                new LoginResponse(
                        token,
                        user.getId(),
                        user.getName(),
                        user.getOccupation(),
                        isAdmin
                )
        );
    }

    @PostMapping("/admin/users")
    public ResponseEntity<LoginResponse> createUser(
            @RequestBody @Valid CreateUserRequest req
    ) {
        SessionUser current = CurrentUserHolder.get();
        if (current == null || !current.admin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can create users");
        }

        if (users.findByName(req.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User name already exists");
        }

        AppUser user = AppUser.builder()
                .name(req.name().trim())
                .occupation(req.occupation())
                .passwordHash(passwordEncoder.encode(req.password()))
                .createdAt(LocalDateTime.now())
                .build();

        users.save(user);

        boolean isAdmin = (user.getOccupation() == UserOccupation.ADMIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new LoginResponse(
                        null, // no token here, just user info
                        user.getId(),
                        user.getName(),
                        user.getOccupation(),
                        isAdmin
                )
        );
    }
}
