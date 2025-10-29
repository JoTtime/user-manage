package com.app.Harvest.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    // getters & setters
}
