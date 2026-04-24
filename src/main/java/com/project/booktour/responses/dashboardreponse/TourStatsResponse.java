package com.project.booktour.responses.dashboardreponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TourStatsResponse{
    private String tourId;        // ID tour
    private String tourName;      // Tên tour
    private Integer bookedSlots;  // Chỗ đã đặt
    private Integer availableSlots; // Chỗ trống
    private Double price;         // Giá (VND)
    private Double rating;        // Đánh giá
    private String duration;      // Thời gian
    private int bookingCount;
}