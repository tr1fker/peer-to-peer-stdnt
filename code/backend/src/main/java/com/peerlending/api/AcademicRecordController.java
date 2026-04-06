package com.peerlending.api;

import com.peerlending.api.dto.AcademicRecordDto;
import com.peerlending.api.dto.SubmitAcademicRecordRequest;
import com.peerlending.application.AcademicRecordService;
import com.peerlending.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-records")
@PreAuthorize("isAuthenticated()")
public class AcademicRecordController {

    private final AcademicRecordService academicRecordService;

    public AcademicRecordController(AcademicRecordService academicRecordService) {
        this.academicRecordService = academicRecordService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AcademicRecordDto submit(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody SubmitAcademicRecordRequest request
    ) {
        return academicRecordService.submit(principal.id(), request);
    }

    @GetMapping("/mine")
    public List<AcademicRecordDto> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return academicRecordService.listMine(principal.id());
    }
}
