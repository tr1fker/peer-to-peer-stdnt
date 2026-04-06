package com.peerlending.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GithubRegisterCompleteRequest(
        @NotBlank String pendingToken,
        @NotBlank
        @Size(max = 255)
        @Pattern(
                regexp = "^\\S+(?:\\s+\\S+){2}$",
                message = "Укажите ФИО тремя словами через пробел (фамилия, имя, отчество)"
        )
        String fullName,
        @Size(max = 255) String university,
        @Size(max = 128) String studentGroup
) {
    public GithubRegisterCompleteRequest {
        if (fullName != null) {
            fullName = fullName.trim();
        }
        if (university != null) {
            university = university.trim();
        }
        if (studentGroup != null) {
            studentGroup = studentGroup.trim();
        }
    }
}
