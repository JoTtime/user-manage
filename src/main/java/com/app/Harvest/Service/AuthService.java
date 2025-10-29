package com.app.Harvest.Service;

import com.app.Harvest.dto.*;
import com.app.Harvest.model.Role;
import com.app.Harvest.model.User;
import com.app.Harvest.Repository.UserRepository;
import com.app.Harvest.config.JwtUtils;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(AuthenticationManager authManager, UserRepository userRepo,
                       PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.authManager = authManager;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );
        // if auth succeeds, generate token
        User u = userRepo.findByEmail(loginRequest.getEmail()).get();
        String token = jwtUtils.generateJwtToken(auth);
        return new JwtResponse(token, "Bearer", u.getEmail(), u.getRole().name());
    }

    public void registerUser(SignupRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        Role role = Role.valueOf(req.getRole()); // validate values on controller
        user.setRole(role);

        // Only cooperative signups require validation; farmers maybe validated by cooperative later
        if (role == Role.COOPERATIVE) {
            user.setIsValidated(false);
        } else {
            // government and super_admin are not created here; government added manually
            user.setIsValidated(true);
        }
        userRepo.save(user);
    }
}
