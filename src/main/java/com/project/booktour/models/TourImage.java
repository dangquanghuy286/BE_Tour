package com.project.booktour.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tours_images")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TourImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tour_image_id")
    private Long toursImageId;

    @ManyToOne
    @JoinColumn(name = "tour_id")
    private Tour tour;

    @Column(name = "image_url", length = 300)
    private String imageUrl;
}