package com.mawa3id.specialty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "specialties")
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(name = "name_ar", length = 120)
    private String nameAr;

    @Column(name = "name_fr", length = 120)
    private String nameFr;

    @Column(length = 500)
    private String description;

    protected Specialty() {
    }

    public Specialty(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Specialty(String name, String nameAr, String nameFr, String description) {
        this.name = name;
        this.nameAr = nameAr;
        this.nameFr = nameFr;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameAr() {
        return nameAr;
    }

    public String getNameFr() {
        return nameFr;
    }

    public String getDescription() {
        return description;
    }
}
