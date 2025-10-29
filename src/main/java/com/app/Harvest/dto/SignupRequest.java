package com.app.Harvest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SignupRequest {
    @NotBlank
    private String name;
    @Email @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String role; // expected values: COOPERATIVE, FARMER (cooperative or farmer signups)
    // other cooperative-specific fields later
    // getters & setters
}
