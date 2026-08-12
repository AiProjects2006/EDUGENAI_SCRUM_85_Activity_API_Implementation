package com.ailearning.activity_service.repository;

import com.ailearning.activity_service.entity.StudentActivityAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentActivityAttemptRepository
        extends JpaRepository<StudentActivityAttempt, Long> {

    List<StudentActivityAttempt> findByStudentIdOrderBySubmittedAtDesc(
            Long studentId);

    Optional<StudentActivityAttempt> findByAttemptIdAndStudentId(
            Long attemptId,
            Long studentId);

}

