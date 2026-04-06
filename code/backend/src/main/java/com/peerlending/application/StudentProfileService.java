package com.peerlending.application;

import com.peerlending.api.dto.StudentProfileDto;
import com.peerlending.api.dto.UpdateProfileRequest;
import com.peerlending.common.exception.NotFoundException;
import com.peerlending.domain.ProfileVerificationStatus;
import com.peerlending.persistence.entity.StudentProfileEntity;
import com.peerlending.persistence.entity.UserEntity;
import com.peerlending.persistence.repository.StudentProfileRepository;
import com.peerlending.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    public StudentProfileService(
            StudentProfileRepository studentProfileRepository,
            UserRepository userRepository
    ) {
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StudentProfileDto getForUser(Long userId) {
        return studentProfileRepository.findById(userId)
                .map(this::toDto)
                .orElseGet(() -> toDto(createMissingProfile(userId)));
    }

    @Transactional
    public StudentProfileDto update(Long userId, UpdateProfileRequest request) {
        StudentProfileEntity p = studentProfileRepository.findById(userId)
                .orElseGet(() -> createMissingProfile(userId));
        p.setFullName(request.fullName());
        p.setUniversity(request.university());
        p.setStudentGroup(request.studentGroup());
        studentProfileRepository.save(p);
        return toDto(p);
    }

    private StudentProfileEntity createMissingProfile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        StudentProfileEntity p = new StudentProfileEntity();
        p.setUser(user);
        p.setFullName(defaultDisplayName(user));
        p.setVerificationStatus(ProfileVerificationStatus.PENDING);
        return studentProfileRepository.save(p);
    }

    private static String defaultDisplayName(UserEntity user) {
        String email = user.getEmail();
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private StudentProfileDto toDto(StudentProfileEntity p) {
        return new StudentProfileDto(
                p.getFullName(),
                p.getUniversity(),
                p.getStudentGroup(),
                p.getVerificationStatus(),
                p.getReputationPoints()
        );
    }
}
