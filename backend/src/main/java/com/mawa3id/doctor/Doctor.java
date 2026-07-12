package com.mawa3id.doctor;

import com.mawa3id.specialty.Specialty;
import com.mawa3id.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;

    @Column(name = "cabinet_address", length = 300)
    private String cabinetAddress;

    @Column(name = "working_hours", length = 500)
    private String workingHours;

    @Column(length = 40)
    private String phone;

    @Column(length = 1000)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "acceptance_mode", nullable = false, length = 20)
    private AcceptanceMode acceptanceMode = AcceptanceMode.MANUAL;

    protected Doctor() {
    }

    public Doctor(User user, String name, Specialty specialty, String cabinetAddress,
                  String workingHours, String phone) {
        this.user = user;
        this.name = name;
        this.specialty = specialty;
        this.cabinetAddress = cabinetAddress;
        this.workingHours = workingHours;
        this.phone = phone;
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Specialty getSpecialty() {
        return specialty;
    }

    public void setSpecialty(Specialty specialty) {
        this.specialty = specialty;
    }

    public String getCabinetAddress() {
        return cabinetAddress;
    }

    public void setCabinetAddress(String cabinetAddress) {
        this.cabinetAddress = cabinetAddress;
    }

    public String getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(String workingHours) {
        this.workingHours = workingHours;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public AcceptanceMode getAcceptanceMode() {
        return acceptanceMode;
    }

    public void setAcceptanceMode(AcceptanceMode acceptanceMode) {
        this.acceptanceMode = acceptanceMode;
    }
}
