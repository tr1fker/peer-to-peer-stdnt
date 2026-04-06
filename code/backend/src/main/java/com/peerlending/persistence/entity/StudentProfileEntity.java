package com.peerlending.persistence.entity;

import com.peerlending.domain.ProfileVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
public class StudentProfileEntity {

    @Id
    private Long userId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String university;

    @Column(name = "student_group")
    private String studentGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private ProfileVerificationStatus verificationStatus = ProfileVerificationStatus.PENDING;

    @Column(name = "reputation_points", nullable = false)
    private int reputationPoints = 500;
}
