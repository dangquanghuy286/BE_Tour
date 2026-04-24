package com.project.booktour.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TourDTO {

    @NotEmpty(message = "Tiêu đề không được để trống")
    @Size(min = 3, max = 255, message = "Tiêu đề phải có độ dài từ 3 đến 255 ký tự")
    private String title; // Tiêu đề tour

    @NotEmpty(message = "Mô tả không được để trống")
    private String description; // Mô tả tour

    private List<String> images; // Danh sách hình ảnh tour

    @Min(value = 1, message = "Số lượng chỗ phải ít nhất là 1")
    private int quantity; // Số lượng chỗ

    @Min(value = 0, message = "Giá vé người lớn phải ít nhất là 0")
    @JsonProperty("price_adult")
    private double priceAdult; // Giá vé người lớn

    @Min(value = 0, message = "Giá vé trẻ em phải ít nhất là 0")
    @JsonProperty("price_child")
    private double priceChild; // Giá vé trẻ em

    @NotEmpty(message = "Thời gian tour không được để trống")
    private String duration; // Thời gian tour

    @NotEmpty(message = "Điểm đến không được để trống")
    private String destination; // Điểm đến

    private boolean availability = true; // Tình trạng còn chỗ

    @NotNull(message = "Lịch trình không được để trống")
    private List<ScheduleDTO> itinerary; // Lịch trình tour

    @NotNull(message = "Khu vực không được để trống")
    private String region; // Khu vực

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate; // Ngày bắt đầu

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate; // Ngày kết thúc

    @NotEmpty(message = "Nơi xuất phát không được để trống")
    private String departurePoint; // Nơi xuất phát của tour

    @AssertTrue(message = "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu")
    public boolean isEndDateValid() {
        return endDate == null || startDate == null || !endDate.isBefore(startDate);
    }
}