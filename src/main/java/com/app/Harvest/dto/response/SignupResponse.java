package com.app.Harvest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {

    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private Boolean isApproved;
    private String message;
}