package com.project.booktour.dtos;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PromotionDTO {
    private String description;
    private Double discount;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer quantity;
    private String code;
}