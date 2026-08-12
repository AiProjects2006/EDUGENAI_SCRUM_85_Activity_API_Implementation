package com.ailearning.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attempt_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class AttemptQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attemptQuestionId;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private StudentActivityAttempt attempt;

    private String questionType;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    private String instructions;

    @Column(columnDefinition = "TEXT")
    private String studentAnswer;

    @Column(columnDefinition = "TEXT")
    private String correctAnswer;

    private Boolean isCorrect;

    @Column(columnDefinition = "TEXT")
    private String feedback;
}
