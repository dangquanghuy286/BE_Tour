package com.project.booktour.responses;

import com.project.booktour.dtos.GuideDTO;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideListResponse {
    private List<GuideDTO> guides;
    private int totalPages;
    private long totalElements;
}