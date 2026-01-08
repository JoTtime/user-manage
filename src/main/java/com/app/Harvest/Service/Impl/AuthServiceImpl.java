package com.app.Harvest.Service.Impl;

import com.app.Harvest.dto.request.ChangePasswordRequest;
import com.app.Harvest.dto.request.ForgotPasswordRequest;
import com.app.Harvest.dto.request.ResetPasswordRequest;
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
import com.app.Harvest.Service.EmailService;
import com.app.Harvest.Service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final CooperativeRepository cooperativeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+?237|237)?[26]\\d{8}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    private static final String[] VALID_REGIONS = {
            "Adamawa", "Centre", "East", "Far North", "Littoral",
            "North", "Northwest", "South", "Southwest", "West"
    };

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getRole() == Role.COOPERATIVE && !user.getIsApproved()) {
            throw new UnauthorizedException("Account is pending approval by admin");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("fullName", user.getFullName());
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        if (user.getCooperative() != null) {
            extraClaims.put("cooperativeId", user.getCooperative().getId());
            extraClaims.put("cooperativeName", user.getCooperative().getName());
        }
        extraClaims.put("isApproved", user.getIsApproved());

        String token = jwtService.generateToken(userDetails, extraClaims);

        return LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isApproved(user.getIsApproved())
                .cooperativeName(user.getCooperative() != null ? user.getCooperative().getName() : null)
                .cooperativeId(user.getCooperative() != null ? user.getCooperative().getId() : null)
                .build();
    }

    @Override
    @Transactional
    public SignupResponse signup(SignupRequest signupRequest) {
        validateSignupRequest(signupRequest);

        if (signupRequest.getRole() != Role.COOPERATIVE) {
            throw new BadRequestException("Only cooperative members can signup. Other users are added by admin.");
        }

        String cleanPhone = cleanPhoneNumber(signupRequest.getPhone());

        if (userRepository.existsByEmail(signupRequest.getCoopEmail())) {
            throw new BadRequestException("Email address is already registered");
        }

        if (userRepository.existsByPhoneNumber(cleanPhone)) {
            throw new BadRequestException("Phone number is already registered");
        }

        if (cooperativeRepository.existsByName(signupRequest.getCoopName())) {
            throw new BadRequestException("A cooperative with this name already exists");
        }

        if (cooperativeRepository.findByEmail(signupRequest.getCoopEmail()).isPresent()) {
            throw new BadRequestException("This email is already associated with another cooperative");
        }

        String username = generateUsernameFromEmail(signupRequest.getCoopEmail());
        if (userRepository.existsByUsername(username)) {
            username = username + UUID.randomUUID().toString().substring(0, 4);
        }

        Cooperative cooperative = Cooperative.builder()
                .name(signupRequest.getCoopName())
                .email(signupRequest.getCoopEmail())
                .phoneNumber(cleanPhone)
                .region(signupRequest.getLocation())
                .address(signupRequest.getLocation())
                .description("Registered via signup form")
                .build();
        cooperative = cooperativeRepository.save(cooperative);

        User user = User.builder()
                .username(username)
                .email(signupRequest.getCoopEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .fullName(signupRequest.getCoopName() + " Admin")
                .phoneNumber(cleanPhone)
                .role(signupRequest.getRole())
                .isApproved(false)
                .isValidated(false)
                .cooperative(cooperative)
                .registrationNumber(generateRegistrationNumber())
                .address(signupRequest.getLocation())
                .region(signupRequest.getLocation())
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

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Processing forgot password request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            return; // Don't throw exception, keep user details private
        }

        String resetToken = generateSecureToken();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.saveAndFlush(user);

        log.info("Reset token generated and saved for user: {}", user.getEmail());

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken, user.getFullName());
            log.info("Password reset email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email", e);
            throw new BadRequestException("Failed to send password reset email. Please try again later.");
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with token");

        User user = userRepository.findByResetTokenAndResetTokenExpiryAfter(
                request.getToken(), LocalDateTime.now()
        ).orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (request.getNewPassword().length() < 8 || !PASSWORD_PATTERN.matcher(request.getNewPassword()).matches()) {
            throw new BadRequestException("New password does not meet criteria");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.saveAndFlush(user);

        log.info("Password updated and flushed to database for user: {}", user.getEmail());

        try {
            emailService.sendPasswordChangedConfirmationEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            log.error("Failed to send password changed confirmation email", e);
        }

        log.info("Password reset successfully completed for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String userEmail) {
        log.info("Processing password change for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (request.getNewPassword().length() < 8 || !PASSWORD_PATTERN.matcher(request.getNewPassword()).matches()) {
            throw new BadRequestException("New password does not meet criteria");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.saveAndFlush(user);

        log.info("Password changed successfully for user: {}", userEmail);

        try {
            emailService.sendPasswordChangedConfirmationEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            log.error("Failed to send password changed confirmation email", e);
        }
    }

    private String generateSecureToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private void validateSignupRequest(SignupRequest request) {
        // ... existing validation logic
    }

    private String cleanPhoneNumber(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[\\s\\-()]", "");
    }

    private String generateUsernameFromEmail(String email) {
        String username = email.substring(0, email.indexOf('@')).toLowerCase();
        return username.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private String generateRegistrationNumber() {
        return "REG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}