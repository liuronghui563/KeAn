package com.kean.vo;

import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String role,
        String username,
        String phone,
        String nickname,
        String gender,
        String avatarUrl,
        Long schoolId,
        Long campusId,
        String schoolName,
        String campusName,
        Integer completedCount,
        Integer cancelledCount,
        Integer reportedCount,
        String status,
        Integer forbidPublish,
        Integer forbidApply,
        Integer muted,
        LocalDateTime createdAt
) {
}
