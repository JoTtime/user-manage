package com.app.Harvest.Entity;

import com.app.Harvest.model.User;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "cooperatives")
public class Cooperative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String region;

    private String contactNumber;

    private String address;

    // Cooperative account info
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Farmers managed by this cooperative
    @OneToMany(mappedBy = "cooperative", cascade = CascadeType.ALL)
    private List<Farmer> farmers;

    // ======== GETTERS AND SETTERS ========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Farmer> getFarmers() {
        return farmers;
    }

    public void setFarmers(List<Farmer> farmers) {
        this.farmers = farmers;
    }

}
