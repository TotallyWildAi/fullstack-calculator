package com.bench.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory user store that pre-populates test users with BCrypt-encoded passwords.
 * Used by CustomUserDetailsService to load user credentials.
 */
@Component
public class InMemoryUserStore {

    private final Map<String, User> users = new HashMap<>();

    public InMemoryUserStore() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodedPassword1 = encoder.encode("SecurePass123!");
        users.put("testuser", new User("testuser", encodedPassword1, "USER"));
        
        String encodedPassword2 = encoder.encode("AnotherPass123!");
        users.put("anotheruser", new User("anotheruser", encodedPassword2, "USER"));
    }

    /**
     * Find a user by username.
     *
     * @param username the username to search for
     * @return Optional containing the User if found, empty otherwise
     */
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
