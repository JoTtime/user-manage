package com.app.Harvest.Service.Impl;

import com.app.Harvest.dto.request.LoginRequest;
import com.app.Harvest.dto.request.SignupRequest;
import com.app.Harvest.dto.response.LoginResponse;
import com.app.Harvest.dto.response.SignupResponse;
import com.app.Harvest.Entity.Cooperative;
import com.app.Harvest.Entity.Role;
import com.app.Harvest.Entity.User;
import com.app.Harvest.exception.BadRequestException;
import com.app.Harvest.exception.UnauthorizedException;
import com.app.Harvest.Repository.CooperativeRepository;
import com.app.Harvest.Repository.UserRepository;
import com.app.Harvest.Service.AuthService;
import com.app.Harvest.Service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CooperativeRepository cooperativeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // Load user details
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Check if user is validated
        if (user.getIsValidated() == null || !user.getIsValidated()) {
            throw new UnauthorizedException("Account is not validated");
        }

        // Check if cooperative user is approved
        if (user.getRole() == Role.COOPERATIVE && !user.getIsApproved()) {
            throw new UnauthorizedException("Account is pending approval by admin");
        }

        // Generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
        String token = jwtService.generateToken(userDetails);

        // Build response
        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isApproved(user.getIsApproved())
                .cooperativeName(user.getCooperative() != null ? user.getCooperative().getName() : null)
                .build();
    }

    @Override
    @Transactional
    public SignupResponse signup(SignupRequest signupRequest) {
        // Validate email uniqueness
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        // Validate username uniqueness
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        // Only COOPERATIVE role can sign up
        if (signupRequest.getRole() != Role.COOPERATIVE) {
            throw new BadRequestException("Only cooperative members can signup. Other users are added by admin.");
        }

        // Create or get cooperative
        Cooperative cooperative = null;
        if (signupRequest.getCooperativeName() != null && !signupRequest.getCooperativeName().isEmpty()) {
            // Check if cooperative already exists
            cooperative = cooperativeRepository.findByName(signupRequest.getCooperativeName())
                    .orElseGet(() -> {
                        // Create new cooperative
                        Cooperative newCoop = Cooperative.builder()
                                .name(signupRequest.getCooperativeName())
                                .registrationNumber(signupRequest.getCooperativeRegistrationNumber())
                                .address(signupRequest.getCooperativeAddress())
                                .region(signupRequest.getCooperativeRegion())
                                .description(signupRequest.getCooperativeDescription())
                                .build();
                        return cooperativeRepository.save(newCoop);
                    });
        }

        // Create user
        User user = User.builder()
                .username(signupRequest.getUsername())
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .fullName(signupRequest.getFullName())
                .phoneNumber(signupRequest.getPhoneNumber())
                .role(signupRequest.getRole())
                .isApproved(false) // Requires admin approval
                .isValidated(false) // Requires email validation
                .cooperative(cooperative)
                .registrationNumber(generateRegistrationNumber())
                .address(signupRequest.getAddress())
                .region(signupRequest.getRegion())
                .build();

        user = userRepository.save(user);

        return SignupResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .isApproved(user.getIsApproved())
                .message("Registration successful. Your account is pending approval by admin.")
                .build();
    }

    private String generateRegistrationNumber() {
        return "REG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}