package com.app.Harvest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerResponse {

    private Long id;
    private String fullName;
    private String phoneNumber;
    private String location;
    private String language;
    private Double areaHa; // Total farm area
    private Double allocatedArea; // Total area allocated to projects
    private Double remainingArea; // Remaining available area
    private String status;
    private String qrCode;
    private Long cooperativeId;
    private String cooperativeName;
    private CoordinatesResponse coordinates; // Add this
    private List<ProjectResponse> projects; // List of all projects/crops
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}