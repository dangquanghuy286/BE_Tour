package com.project.booktour.dtos;

import com.project.booktour.models.Gender; // Import enum Gender
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideDTO {

    private Integer guideId;
    private String fullName;
    private Integer age;
    private Gender gender;
    private String phoneNumber;
    private String databaseLink;
    private String gmailLink;
    private String photo;
    private Boolean isActive;
}