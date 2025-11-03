package com.app.Harvest.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "crop")
    private String crop;

    @Column(name = "area_ha")
    private Double areaHa;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "active"; // active or inactive

    @Column(name = "qr_code", unique = true)
    private String qrCode; // optional: unique ID for offline access

    // Belongs to a cooperative (farmer is registered by cooperative)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperative_id", nullable = false)
    private Cooperative cooperative;

    // Optional: Link to user account (for future login functionality)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}