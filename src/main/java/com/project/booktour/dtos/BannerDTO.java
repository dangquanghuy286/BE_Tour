package com.project.booktour.dtos;

import com.project.booktour.models.Banner;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BannerDTO {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    // BỎ @NotBlank vì imageUrl sẽ được set trong service, không cần validate
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Size(max = 500, message = "Link must not exceed 500 characters")
    private String link;

    private Banner.Position position;

    private Integer priority;

    private Boolean isActive;

    private LocalDate startDate;

    private LocalDate endDate;
}