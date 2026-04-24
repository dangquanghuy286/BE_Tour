package com.project.booktour.responses.dashboardreponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsByDateRangeResponse {
    private String startDate;       // Ngày bắt đầu (dd/MM/yyyy)
    private String endDate;         // Ngày kết thúc (dd/MM/yyyy)
    private Double totalRevenue;    // Tổng doanh thu trong khoảng thời gian
    private Integer totalBookings;  // Tổng số booking trong khoảng thời gian
    private Integer totalUsers;     // Số người dùng mới trong khoảng thời gian
}