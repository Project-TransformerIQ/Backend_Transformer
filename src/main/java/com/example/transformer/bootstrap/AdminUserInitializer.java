// src/main/java/com/example/transformer/bootstrap/AdminUserInitializer.java
package com.example.transformer.bootstrap;

import com.example.transformer.model.AppUser;
import com.example.transformer.model.UserOccupation;
import com.example.transformer.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AdminUserInitializer(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.findByName("admin").isEmpty()) {
            AppUser admin = AppUser.builder()
                    .name("admin")
                    .occupation(UserOccupation.ADMIN)
                    .admin(true)
                    .passwordHash(encoder.encode("admin123"))
                    .createdAt(LocalDateTime.now())
                    .build();
            users.save(admin);
            System.out.println("=== Admin user created: name=admin, password=admin123 ===");
        }
    }
}
