package com.project.booktour.responses.toursreponse;

import com.project.booktour.responses.toursreponse.SimplifiedTourResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class TourListResponse {
    private List<SimplifiedTourResponse> tours;
    private int totalPages;
}
