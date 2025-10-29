package com.app.Harvest.Entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "farmers")
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String phoneNumber;

    private String location;

    private String qrCode; // optional: unique ID for offline access

    // Farmer account (login credentials)
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Belongs to a cooperative
    @ManyToOne
    @JoinColumn(name = "cooperative_id")
    private Cooperative cooperative;

    // Farmer's production history
    @OneToMany(mappedBy = "farmer", cascade = CascadeType.ALL)
    private List<Production> productions;

    // ======== GETTERS AND SETTERS ========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Cooperative getCooperative() {
        return cooperative;
    }

    public void setCooperative(Cooperative cooperative) {
        this.cooperative = cooperative;
    }

    public List<Production> getProductions() {
        return productions;
    }

    public void setProductions(List<Production> productions) {
        this.productions = productions;
    }
}
