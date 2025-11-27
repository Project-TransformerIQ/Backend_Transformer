// src/main/java/com/example/transformer/repository/AppUserRepository.java
package com.example.transformer.repository;

import com.example.transformer.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByName(String name);
}
