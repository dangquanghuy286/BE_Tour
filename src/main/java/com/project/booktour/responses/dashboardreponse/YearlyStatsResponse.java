package com.project.booktour.responses.dashboardreponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YearlyStatsResponse {
    private Integer year;           // Năm
    private Double totalRevenue;    // Tổng doanh thu trong năm
    private Integer totalBookings;  // Tổng số booking trong năm
    private Integer totalUsers;     // Số người dùng đăng ký trong năm
}