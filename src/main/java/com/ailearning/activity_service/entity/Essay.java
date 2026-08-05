package com.ailearning.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "essays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Essay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long essayId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private Question question;

    private Integer maxWordCount;

    private Integer minWordCount;

    @Column(length = 1000)
    private String gradingRubric;
}