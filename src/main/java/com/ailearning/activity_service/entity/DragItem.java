package com.ailearning.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drag_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DragItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private String itemText;

    private String imageUrl;

    @OneToMany(mappedBy = "dragItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DragMapping> dragMappings = new ArrayList<>();
}
