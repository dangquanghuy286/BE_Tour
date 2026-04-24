package com.project.booktour.responses.dashboardreponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyStatsResponse {
    private String fullDate;        // Định dạng "dd/MM/yyyy", ví dụ "25/05/2025"
    private String shortDate;       // Định dạng "dd/MM", ví dụ "25/05" cho hiển thị trên chart
    private Double totalRevenue;    // Doanh thu trong ngày
    private Integer totalBookings;  // Số lượng booking trong ngày
    private Integer totalUsers;     // Số người dùng mới đăng ký trong ngày
}