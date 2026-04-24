package com.project.booktour.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tours")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tour extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tour_id")
    private Long tourId; // ID tour

    @Column(name = "title", length = 255, nullable = false)
    private String title; // Tiêu đề tour

    @Column(name = "description", columnDefinition = "LONGTEXT", nullable = false)
    private String description; // Mô tả tour

    @OneToMany(
            mappedBy = "tour",
            fetch = FetchType.LAZY,  // Nên dùng LAZY thay vì EAGER để tối ưu hiệu suất
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<TourImage> tourImages = new ArrayList<>(); // Danh sách hình ảnh tour

    @OneToMany(
            mappedBy = "tour",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Review> reviews; // Danh sách đánh giá tour

    @Column(name = "quantity", nullable = false)
    private Integer quantity; // Số lượng chỗ của tour

    @Column(name = "price_adult", nullable = false)
    private Double priceAdult; // Giá vé người lớn

    @Column(name = "price_child", nullable = false)
    private Double priceChild; // Giá vé trẻ em

    @Column(name = "duration", length = 255, nullable = false)
    private String duration; // Thời gian tour

    @Column(name = "destination", length = 255, nullable = false)
    private String destination; // Điểm đến

    @Column(name = "availability", nullable = false)
    private Boolean availability = true; // Tình trạng còn chỗ

    @Column(name = "itinerary", length = 255, nullable = false)
    private String itinerary; // Lịch trình tour

    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false)
    private Region region; // Khu vực của tour

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // Ngày bắt đầu

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // Ngày kết thúc

    @Column(name = "departure_point", length = 255, nullable = false)
    private String departurePoint; // Nơi xuất phát của tour
}