// Updated DashboardController.java
package com.project.booktour.controllers;

import com.project.booktour.responses.dashboardreponse.DashboardResponse;
import com.project.booktour.responses.dashboardreponse.StatsByDateRangeResponse;
import com.project.booktour.services.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("${api.prefix}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getDashboardStats() {
        try {
            DashboardResponse stats = dashboardService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi lấy thống kê dashboard: " + e.getMessage());
        }
    }

    @GetMapping("/stats/range")
    public ResponseEntity<?> getStatsByDateRange(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            StatsByDateRangeResponse stats = dashboardService.getStatsByDateRange(start, end);
            return ResponseEntity.ok(stats);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Định dạng ngày không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thống kê theo khoảng ngày: " + e.getMessage());
        }
    }
}