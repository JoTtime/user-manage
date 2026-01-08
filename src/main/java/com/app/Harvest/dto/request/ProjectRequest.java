package com.app.Harvest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ProjectRequest {

    private Long id; // This field is crucial for updates

    @NotBlank(message = "Crop name is required")
    private String cropName;

    @NotNull(message = "Area is required")
    @Positive(message = "Area must be positive")
    private Double areaHa;

    private String status;

    private LocalDate plantingDate;
    private LocalDate expectedHarvestDate;

    private String notes;
}