package com.app.Harvest.dto.response;

import com.app.Harvest.Entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
    private Boolean isApproved;
    private String cooperativeName;
}