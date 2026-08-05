package com.ailearning.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fill_blanks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FillBlank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long blankId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable =false)
    private Question question;

    @Column(nullable = false)
    private String correctAnswer;

    private Integer blankIndex;
}
