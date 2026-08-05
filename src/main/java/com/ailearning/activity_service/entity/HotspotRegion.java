package com.ailearning.activity_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotspot_regions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotspotRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long regionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private Integer xCoordinate;

    @Column(nullable = false)
    private Integer yCoordinate;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;
}
