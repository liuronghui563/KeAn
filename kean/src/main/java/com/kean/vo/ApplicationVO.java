package com.kean.vo;

import java.time.LocalDateTime;

public record ApplicationVO(
        Long id,
        Long taskId,
        Long applicantId,
        String nickname,
        String message,
        String status,
        LocalDateTime createdAt
) {
}
