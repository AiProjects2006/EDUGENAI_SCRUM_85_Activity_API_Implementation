package com.ailearning.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sorting_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SortingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private String itemText;

    @Column(nullable = false)
    private Integer correctOrder;
}
