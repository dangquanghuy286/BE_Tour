package com.project.booktour.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDTO {

    @NotBlank(message = "Username or is required")
    @JsonProperty("user_name")
    private String userName;
    @NotBlank(message = "Password cannot be blank")
    private String password;
}
