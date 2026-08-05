package com.ailearning.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "matching_pairs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pairId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private String leftItem;

    @Column(nullable = false)
    private String rightItem;

    private Integer displayOrder;
}