package com.app.Harvest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;
    private String cropName;
    private Double areaHa;
    private String status;
    private LocalDate plantingDate;
    private LocalDate expectedHarvestDate;
    private String notes;
    private Long farmerId;
    private String farmerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}