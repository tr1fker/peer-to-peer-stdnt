package com.peerlending.domain;

public enum ProfileVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    /** Успеваемость снята, заёмщикские действия ограничены (не бан входа). */
    BLOCKED
}
