package com.project.booktour.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourImageDTO {

    @JsonProperty("tour_id")
    @Min(value = 1, message = "Tour ID must be greater than or equal to 1")
    private Long tourId;

    @Size(min = 5, max = 300, message = "Image URL must be between 5 and 300 characters")
    @JsonProperty("image_url")
    private String imageUrl;
}
