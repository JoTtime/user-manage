package com.app.Harvest.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "cooperatives")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cooperative extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "registration_number", unique = true)
    private String registrationNumber;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "address")
    private String address;

    @Column(name = "region")
    private String region;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "cooperative", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> members;
}