package com.example.reminder.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.reminder.model.OAuthProvider;
import com.example.reminder.model.User;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderId(String providerId);
    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}
