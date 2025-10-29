package com.app.Harvest.Service;

import com.app.Harvest.model.User;
import com.app.Harvest.Repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    public JwtUserDetailsService(UserRepository userRepo) { this.userRepo = userRepo; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        // map role to granted authority
        return new org.springframework.security.core.userdetails.User(
                u.getEmail(),
                u.getPassword(),
                u.getIsValidated() != null && u.getIsValidated() ?
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())) :
                        Collections.emptyList()
        );
    }
}
