package com.project.booktour.responses.dashboardreponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingStatsResponse {
    private String bookingId;        // ID đặt tour
    private String customerName;     // Tên khách hàng
    private String tourName;         // Tên tour
    private Double price;            // Giá (VND)
    private String status;           // Trạng thái
    private String paymentMethod;    // Phương thức thanh toán
    private String region;           // Miền (lấy từ tour)
    private String bookingDate;      // Ngày đặt
}