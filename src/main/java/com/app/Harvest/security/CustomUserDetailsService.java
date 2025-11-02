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

        // Check if user is validated before allowing login
        boolean enabled = user.getIsValidated() != null && user.getIsValidated();

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),  // Use username as principal
                user.getPassword(),
                enabled,  // Account enabled
                true,     // Account non-expired
                true,     // Credentials non-expired
                true,     // Account non-locked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}