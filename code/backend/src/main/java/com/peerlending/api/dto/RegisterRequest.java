package com.peerlending.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank
        @Size(min = 8, max = 128)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,128}$",
                message = "Пароль: 8–128 символов, латинские буквы в разном регистре, цифра и хотя бы один спецсимвол"
        )
        String password,
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
    public RegisterRequest {
        email = email == null ? "" : email.trim().toLowerCase();
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
