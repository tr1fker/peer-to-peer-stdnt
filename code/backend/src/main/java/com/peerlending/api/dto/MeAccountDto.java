package com.peerlending.api.dto;

import java.util.Set;

public record MeAccountDto(
        Long userId,
        String email,
        Set<String> roles,
        boolean githubLinked,
        boolean googleLinked,
        boolean blocked
) {
}
