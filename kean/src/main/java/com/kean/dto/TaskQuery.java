package com.kean.dto;

public record TaskQuery(
        String keyword,
        String taskDate,
        Long courseId,
        Long campusId,
        String status,
        Long schoolId,
        Long page,
        Long size
) {
}
