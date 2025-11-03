package com.app.Harvest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerResponse {

    private Long id;
    private String fullName;
    private String phoneNumber;
    private String location;
    private String crop;
    private Double areaHa;
    private String status;
    private String qrCode;
    private Long cooperativeId;
    private String cooperativeName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}