package com.bloodbank.entity;

import com.bloodbank.enums.BloodGroup;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "issue_records",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_issue_code", columnNames = "issue_code"),
        @UniqueConstraint(name = "uk_issue_blood_unit", columnNames = "blood_unit_id")
    },
    indexes = {
        @Index(name = "idx_issue_code", columnList = "issue_code"),
        @Index(name = "idx_issue_unit_id", columnList = "blood_unit_id"),
        @Index(name = "idx_issue_date", columnList = "issue_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_code", nullable = false, length = 50)
    private String issueCode;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "blood_unit_id",
        nullable = false,
        unique = true,
        foreignKey = @ForeignKey(name = "fk_issue_blood_unit")
    )
    private BloodUnit bloodUnit;

    @Column(name = "patient_name", nullable = false, length = 100)
    private String patientName;

    @Column(name = "hospital_name", nullable = false, length = 150)
    private String hospitalName;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_blood_group", nullable = false, length = 20)
    private BloodGroup requestedBloodGroup;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.issueDate == null) {
            this.issueDate = LocalDateTime.now();
        }
        this.createdAt = LocalDateTime.now();
    }
}
