package com.ailearning.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drop_zones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DropZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long zoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private String zoneLabel;

    @OneToMany(mappedBy = "dropZone",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    private List<DragMapping> dragMappings = new ArrayList<>();
}
