package com.app.Harvest.Controller;

import com.app.Harvest.dto.request.LoginRequest;
import com.app.Harvest.dto.request.SignupRequest;
import com.app.Harvest.dto.response.ApiResponse;
import com.app.Harvest.dto.response.LoginResponse;
import com.app.Harvest.dto.response.SignupResponse;
import com.app.Harvest.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}