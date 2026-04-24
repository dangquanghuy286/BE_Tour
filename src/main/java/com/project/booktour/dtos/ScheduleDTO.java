package com.project.booktour.dtos;



import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDTO {
    private Long day;
    private String title;
    private List<String> content;
}