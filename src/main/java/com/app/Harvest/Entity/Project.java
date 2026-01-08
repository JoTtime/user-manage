package com.app.Harvest.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project extends BaseEntity {

    @Column(name = "crop_name", nullable = false)
    private String cropName;

    @Column(name = "area_ha", nullable = false)
    private Double areaHa;

    @Column(name = "status")
    @Builder.Default
    private String status = "active"; // active, completed, planned

    @Column(name = "planting_date")
    private java.time.LocalDate plantingDate;

    @Column(name = "expected_harvest_date")
    private java.time.LocalDate expectedHarvestDate;

    @Column(name = "notes", length = 500)
    private String notes;

    // Many projects belong to one farmer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;
}