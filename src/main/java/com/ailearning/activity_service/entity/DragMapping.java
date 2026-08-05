package com.ailearning.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "drag_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DragMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drag_item_id", nullable = false)
    private DragItem dragItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drop_zone_id", nullable = false)
    private DropZone dropZone;
}
