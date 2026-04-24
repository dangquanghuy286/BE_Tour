package com.project.booktour.responses.dashboardreponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyRevenueResponse{
    private String monthYear; // Định dạng "MM-yyyy", ví dụ "05-2025"
    private Double revenue;   // Doanh thu của tháng
}