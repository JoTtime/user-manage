package com.app.Harvest.Controller;

import com.app.Harvest.dto.request.ChangePasswordRequest;
import com.app.Harvest.dto.request.ForgotPasswordRequest;
import com.app.Harvest.dto.request.LoginRequest;
import com.app.Harvest.dto.request.ResetPasswordRequest;
import com.app.Harvest.dto.request.SignupRequest;
import com.app.Harvest.dto.response.ApiResponse;
import com.app.Harvest.dto.response.LoginResponse;
import com.app.Harvest.dto.response.SignupResponse;
import com.app.Harvest.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        SignupResponse response = authService.signup(signupRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Signup successful", response));
    }

    // NEW ENDPOINTS FOR PASSWORD MANAGEMENT

    /**
     * Request password reset - sends email with reset link
     * POST /auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "If the email exists, you will receive password reset instructions",
                        null
                )
        );
    }

    /**
     * Reset password using token from email
     * POST /auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("Password has been reset successfully", null)
        );
    }

    /**
     * Change password for logged-in user
     * POST /auth/change-password
     * Requires authentication
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        String userEmail = authentication.getName();
        authService.changePassword(request, userEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Password changed successfully", null)
        );
    }
}