package com.bloodbank.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "donations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_donation_code", columnNames = "donation_code")
    },
    indexes = {
        @Index(name = "idx_donation_code", columnList = "donation_code"),
        @Index(name = "idx_donation_donor_id", columnList = "donor_id"),
        @Index(name = "idx_donation_date", columnList = "donation_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "donation_code", nullable = false, length = 50)
    private String donationCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_donation_donor"))
    private Donor donor;

    @Column(name = "donation_date", nullable = false)
    private LocalDate donationDate;

    @Column(name = "number_of_units", nullable = false)
    private Integer numberOfUnits;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "donation", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<BloodUnit> bloodUnits = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
