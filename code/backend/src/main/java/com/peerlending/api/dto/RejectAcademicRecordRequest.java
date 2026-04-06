package com.peerlending.api.dto;

import jakarta.validation.constraints.Size;

public record RejectAcademicRecordRequest(
        @Size(max = 2000) String reason
) {
}
