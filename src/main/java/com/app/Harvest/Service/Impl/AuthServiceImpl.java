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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CooperativeRepository cooperativeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // Cameroon phone number pattern
    // Supports: +237XXXXXXXXX, 237XXXXXXXXX, or 6/2XXXXXXXX
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+?237|237)?[26]\\d{8}$"
    );

    // Strong password pattern
    // At least 8 characters, 1 uppercase, 1 lowercase, 1 number, 1 special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    // Valid Cameroon regions
    private static final String[] VALID_REGIONS = {
            "Adamawa", "Centre", "East", "Far North", "Littoral",
            "North", "Northwest", "South", "Southwest", "West"
    };

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

        // Check if user is validated (COMMENTED FOR TESTING)
        // if (user.getIsValidated() == null || !user.getIsValidated()) {
        //     throw new UnauthorizedException("Account is not validated");
        // }

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
        // Validate all inputs
        validateSignupRequest(signupRequest);

        // Only COOPERATIVE role can sign up
        if (signupRequest.getRole() != Role.COOPERATIVE) {
            throw new BadRequestException("Only cooperative members can signup. Other users are added by admin.");
        }

        // Clean and normalize phone number
        String cleanPhone = cleanPhoneNumber(signupRequest.getPhone());

        // Check for duplicate email
        if (userRepository.existsByEmail(signupRequest.getCoopEmail())) {
            throw new BadRequestException("Email address is already registered");
        }

        // Check for duplicate phone number
        if (userRepository.existsByPhoneNumber(cleanPhone)) {
            throw new BadRequestException("Phone number is already registered");
        }

        // Check for duplicate cooperative name
        if (cooperativeRepository.existsByName(signupRequest.getCoopName())) {
            throw new BadRequestException("A cooperative with this name already exists");
        }

        // Check for duplicate cooperative email
        if (cooperativeRepository.findByEmail(signupRequest.getCoopEmail()).isPresent()) {
            throw new BadRequestException("This email is already associated with another cooperative");
        }

        // Generate username from email
        String username = generateUsernameFromEmail(signupRequest.getCoopEmail());

        // Validate username uniqueness
        if (userRepository.existsByUsername(username)) {
            // If username exists, append random numbers
            username = username + UUID.randomUUID().toString().substring(0, 4);
        }

        // Create new cooperative
        Cooperative cooperative = Cooperative.builder()
                .name(signupRequest.getCoopName())
                .email(signupRequest.getCoopEmail())
                .phoneNumber(cleanPhone)
                .region(signupRequest.getLocation())
                .address(signupRequest.getLocation())
                .description("Registered via signup form")
                .build();
        cooperative = cooperativeRepository.save(cooperative);

        // Create user account for the cooperative
        User user = User.builder()
                .username(username)
                .email(signupRequest.getCoopEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .fullName(signupRequest.getCoopName() + " Admin")
                .phoneNumber(cleanPhone)
                .role(signupRequest.getRole())
                .isApproved(false) // Requires admin approval
                .isValidated(false) // Requires email validation
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

    /**
     * Validate signup request data
     */
    private void validateSignupRequest(SignupRequest request) {
        // Validate cooperative name
        if (request.getCoopName() == null || request.getCoopName().trim().isEmpty()) {
            throw new BadRequestException("Cooperative name is required");
        }
        if (request.getCoopName().trim().length() < 3) {
            throw new BadRequestException("Cooperative name must be at least 3 characters long");
        }

        // Validate email
        if (request.getCoopEmail() == null || request.getCoopEmail().trim().isEmpty()) {
            throw new BadRequestException("Email address is required");
        }
        if (!EMAIL_PATTERN.matcher(request.getCoopEmail().trim()).matches()) {
            throw new BadRequestException("Invalid email address format");
        }

        // Validate phone number
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new BadRequestException("Phone number is required");
        }
        String cleanPhone = cleanPhoneNumber(request.getPhone());
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            throw new BadRequestException("Invalid phone number format. Please use Cameroon format: +237XXXXXXXXX or 6XXXXXXXX");
        }

        // Validate location
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new BadRequestException("Location is required");
        }

        // Validate location format and region
        validateCameroonLocation(request.getLocation());

        // Validate password
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Password is required");
        }
        if (request.getPassword().length() < 8) {
            throw new BadRequestException("Password must be at least 8 characters long");
        }
        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new BadRequestException("Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character");
        }
    }

    /**
     * Validate Cameroon location format and region
     * Expected format: "City, Region"
     */
    private void validateCameroonLocation(String location) {
        String trimmedLocation = location.trim();

        // Check if format contains comma
        if (!trimmedLocation.contains(",")) {
            throw new BadRequestException("Invalid location format. Use: City, Region (e.g., Yaoundé, Centre)");
        }

        // Split by comma
        String[] parts = trimmedLocation.split(",");

        // Must have exactly 2 parts
        if (parts.length != 2) {
            throw new BadRequestException("Invalid location format. Use: City, Region (e.g., Yaoundé, Centre)");
        }

        String city = parts[0].trim();
        String region = parts[1].trim();

        // Both city and region must not be empty
        if (city.isEmpty() || region.isEmpty()) {
            throw new BadRequestException("Both city and region are required in format: City, Region");
        }

        // City must be at least 2 characters
        if (city.length() < 2) {
            throw new BadRequestException("City name must be at least 2 characters");
        }

        // Validate region is a valid Cameroon region
        boolean validRegion = false;
        for (String validRegionName : VALID_REGIONS) {
            if (validRegionName.equalsIgnoreCase(region)) {
                validRegion = true;
                break;
            }
        }

        if (!validRegion) {
            throw new BadRequestException(
                    "Invalid Cameroon region. Valid regions are: " + String.join(", ", VALID_REGIONS)
            );
        }
    }

    /**
     * Clean phone number by removing spaces, dashes, and parentheses
     */
    private String cleanPhoneNumber(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[\\s\\-()]", "");
    }

    /**
     * Generate username from email
     */
    private String generateUsernameFromEmail(String email) {
        // Extract username from email (part before @)
        String username = email.substring(0, email.indexOf('@')).toLowerCase();
        // Remove any special characters and replace with underscore
        return username.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Generate unique registration number
     */
    private String generateRegistrationNumber() {
        return "REG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}