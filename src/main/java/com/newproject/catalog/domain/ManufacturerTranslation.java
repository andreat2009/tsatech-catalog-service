package com.newproject.catalog.domain;

import jakarta.persistence.*;

@Entity
@Table(
    name = "catalog_manufacturer_translation",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_catalog_manufacturer_translation", columnNames = {"manufacturer_id", "language_code"})
    }
)
public class ManufacturerTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id", nullable = false)
    private Manufacturer manufacturer;

    @Column(name = "language_code", length = 5, nullable = false)
    private String languageCode;

    @Column(length = 255, nullable = false)
    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
