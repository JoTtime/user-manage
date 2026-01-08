package com.app.Harvest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Location is required")
    private String location;

    private String language;

    @NotNull(message = "Area in hectares is required")
    @Positive(message = "Area must be positive")
    private Double areaHa;

    private String status; // active, inactive

    // Optional: Projects to create when creating farmer
    @Valid
    private List<ProjectRequest> projects;
    private CoordinatesRequest coordinates;
}