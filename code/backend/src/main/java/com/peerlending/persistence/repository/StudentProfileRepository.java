package com.peerlending.persistence.repository;

import com.peerlending.persistence.entity.StudentProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, Long> {
}
