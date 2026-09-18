package com.kean.vo;

public record PublisherBriefVO(
        Long id,
        String nickname,
        String avatarUrl,
        Integer completedCount
) {
}
