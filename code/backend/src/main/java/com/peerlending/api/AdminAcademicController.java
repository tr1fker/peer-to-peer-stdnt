package com.peerlending.api;

import com.peerlending.api.dto.AcademicPendingRecordDto;
import com.peerlending.api.dto.AcademicRecordDto;
import com.peerlending.api.dto.RejectAcademicRecordRequest;
import com.peerlending.application.AcademicRecordService;
import com.peerlending.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/academic-records")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAcademicController {

    private final AcademicRecordService academicRecordService;

    public AdminAcademicController(AcademicRecordService academicRecordService) {
        this.academicRecordService = academicRecordService;
    }

    @GetMapping("/pending")
    public List<AcademicPendingRecordDto> pending() {
        return academicRecordService.listPendingForReview();
    }

    @PostMapping("/{id}/verify")
    public AcademicRecordDto verify(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        return academicRecordService.verify(id, principal.id());
    }

    @PostMapping("/{id}/reject")
    public AcademicRecordDto reject(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RejectAcademicRecordRequest body
    ) {
        String reason = body != null ? body.reason() : null;
        return academicRecordService.reject(id, principal.id(), reason);
    }
}
