package com.project.booktour.dtos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogDTO {
    private Integer blogId;
    private String title;
    private String content;
    private String author;
    private String image;  // Thêm trường image
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}