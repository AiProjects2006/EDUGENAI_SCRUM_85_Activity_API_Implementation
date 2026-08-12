package com.ailearning.activity_service.repository;

import com.ailearning.activity_service.entity.StudentActivityAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentActivityAttemptRepository
        extends JpaRepository<StudentActivityAttempt, Long> {
}
