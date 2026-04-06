package com.peerlending.application;

import com.peerlending.persistence.entity.StudentProfileEntity;
import com.peerlending.persistence.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReputationService {

    public static final int DEFAULT_POINTS = 500;
    public static final int MIN_POINTS = 0;
    public static final int MAX_POINTS = 1000;

    private final StudentProfileRepository studentProfileRepository;

    public ReputationService(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional
    public void adjustBorrowerReputation(Long borrowerUserId, int delta) {
        if (delta == 0) {
            return;
        }
        StudentProfileEntity profile = studentProfileRepository.findById(borrowerUserId).orElse(null);
        if (profile == null) {
            return;
        }
        int next = profile.getReputationPoints() + delta;
        next = Math.max(MIN_POINTS, Math.min(MAX_POINTS, next));
        profile.setReputationPoints(next);
        studentProfileRepository.save(profile);
    }
}
