package com.app.Harvest.security;

import com.app.Harvest.Entity.User;
import com.app.Harvest.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Try to find by username or email
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));

        // For development: always enable users
        // TODO: Re-enable validation check for production
        boolean enabled = true;  // Changed from: user.getIsValidated() != null && user.getIsValidated();

        // For production, use this instead:
        // boolean enabled = user.getIsValidated() != null && user.getIsValidated();

        // IMPORTANT: Use email as username in UserDetails so JWT token contains email
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),  // Use EMAIL as principal (not username) for JWT consistency
                user.getPassword(),
                enabled,  // Account enabled
                true,     // Account non-expired
                true,     // Credentials non-expired
                true,     // Account non-locked
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}