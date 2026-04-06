package com.peerlending.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SubmitAcademicRecordRequest(
        @NotNull @DecimalMin("1.0") @DecimalMax("10.0") BigDecimal gradeAverage,
        String description
) {
}
