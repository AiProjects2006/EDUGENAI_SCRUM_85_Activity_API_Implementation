package com.ailearning.activity_service.entity;

import com.ailearning.activity_service.enums.ActivitySource;
import com.ailearning.activity_service.enums.AttemptStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "student_activity_attempts")
public class StudentActivityAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attemptId;

    private Long studentId;

    @Enumerated(EnumType.STRING)
    private ActivitySource source;   // AI or MANUAL

    private Long activityId; // nullable, only for manual activities

    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    private AttemptStatus status;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL)
    private List<AttemptQuestion> questions;
}
