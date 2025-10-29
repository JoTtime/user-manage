package com.app.Harvest.Controller;

import com.app.Harvest.dto.*;
import com.app.Harvest.Service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        JwtResponse res = authService.authenticateUser(req);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        // Only COOPERATIVE and FARMER allowed to self-sign up; GOVERNMENT and SUPER_ADMIN are manual
        authService.registerUser(req);
        return ResponseEntity.ok("User registered successfully. If cooperative, wait for super admin validation.");
    }
}
