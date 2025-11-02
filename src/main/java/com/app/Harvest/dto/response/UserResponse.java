package com.app.Harvest.dto.response;

import com.app.Harvest.Entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Role role;
    private Boolean isApproved;
    private String registrationNumber;
    private String cooperativeName;
    private Long cooperativeId;
    private String region;
    private String address;
    private LocalDateTime createdAt;
}