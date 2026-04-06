package com.peerlending.api;

import com.peerlending.api.dto.AdminUserDetailDto;
import com.peerlending.api.dto.AdminUserRowDto;
import com.peerlending.api.dto.BlockUserRequest;
import com.peerlending.api.dto.RevokeVerifiedAcademicRequest;
import com.peerlending.api.dto.RevokeVerifiedAcademicResultDto;
import com.peerlending.application.UserAdminService;
import com.peerlending.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final UserAdminService userAdminService;

    public AdminUsersController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<AdminUserRowDto> list() {
        return userAdminService.listUsers();
    }

    @GetMapping("/{id}")
    public AdminUserDetailDto get(@PathVariable Long id) {
        return userAdminService.getUserDetail(id);
    }

    @PostMapping("/{id}/revoke-verified-academic")
    public RevokeVerifiedAcademicResultDto revokeVerifiedAcademic(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) RevokeVerifiedAcademicRequest body
    ) {
        String reason = Optional.ofNullable(body).map(RevokeVerifiedAcademicRequest::reason).orElse(null);
        return userAdminService.revokeVerifiedAcademic(id, principal.id(), reason);
    }

    @PostMapping("/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) BlockUserRequest body
    ) {
        String reason = body != null ? body.reason() : null;
        userAdminService.blockUser(id, principal.id(), reason);
    }

    @PostMapping("/{id}/unblock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id
    ) {
        userAdminService.unblockUser(id, principal.id());
    }
}
