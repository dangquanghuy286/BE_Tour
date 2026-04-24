package com.project.booktour.dtos;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewDTO {

    @JsonProperty("tour_id")
    @NotNull(message = "Tour ID cannot be null")
    private Long tourId;

    @JsonProperty("user_id")
    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @JsonProperty("comment")
    @NotEmpty(message = "Comment cannot be empty")
    private String comment;

    @JsonProperty("rating")
    @NotNull(message = "Rating cannot be null")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    private Float rating;
}