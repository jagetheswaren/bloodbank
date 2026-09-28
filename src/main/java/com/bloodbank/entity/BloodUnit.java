package com.bloodbank.entity;

import com.bloodbank.enums.BloodGroup;
import com.bloodbank.enums.BloodUnitStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "blood_units",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_blood_unit_code", columnNames = "unit_code")
    },
    indexes = {
        @Index(name = "idx_bu_unit_code", columnList = "unit_code"),
        @Index(name = "idx_bu_blood_group", columnList = "blood_group"),
        @Index(name = "idx_bu_status", columnList = "status"),
        @Index(name = "idx_bu_expiry_date", columnList = "expiry_date"),
        @Index(name = "idx_bu_status_expiry", columnList = "status, expiry_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloodUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "unit_code", nullable = false, length = 50)
    private String unitCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_blood_unit_donation"))
    private Donation donation;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", nullable = false, length = 20)
    private BloodGroup bloodGroup;

    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BloodUnitStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "bloodUnit", fetch = FetchType.LAZY)
    private IssueRecord issueRecord;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
