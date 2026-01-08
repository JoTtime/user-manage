package com.app.Harvest.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "farmers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Farmer extends BaseEntity {

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "language")
    private String language; // Local language: English, French, Fulfulde, Ewondo, etc.

    @Column(name = "area_ha")
    private Double areaHa; // Total farm area

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "active"; // active or inactive

    @Column(name = "qr_code", unique = true)
    private String qrCode; // optional: unique ID for offline access

    // Embedded coordinates (optional)
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude", column = @Column(name = "latitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "longitude")),
            @AttributeOverride(name = "address", column = @Column(name = "coordinates_address"))
    })
    private Coordinates coordinates;

    // One farmer can have many projects (crops)
    @OneToMany(mappedBy = "farmer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    // Belongs to a cooperative (farmer is registered by cooperative)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperative_id", nullable = false)
    private Cooperative cooperative;

    // Optional: Link to user account (for future login functionality)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Helper method to add a project
    public void addProject(Project project) {
        projects.add(project);
        project.setFarmer(this);
    }

    // Helper method to remove a project
    public void removeProject(Project project) {
        projects.remove(project);
        project.setFarmer(null);
    }

    // Calculate total allocated area from projects
    public Double getTotalAllocatedArea() {
        return projects.stream()
                .mapToDouble(Project::getAreaHa)
                .sum();
    }

    // Calculate remaining available area
    public Double getRemainingArea() {
        if (areaHa == null) return 0.0;
        return areaHa - getTotalAllocatedArea();
    }
}