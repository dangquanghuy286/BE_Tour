package com.project.booktour.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ContactDTO {
    @NotBlank(message = "Full name is required")
    @Size(max = 50, message = "Full name must not exceed 50 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be valid")
    private String phoneNumber;

    @NotBlank(message = "Content is required")
    private String content;

    private boolean checked;
}