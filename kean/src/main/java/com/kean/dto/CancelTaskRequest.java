package com.kean.dto;

import jakarta.validation.constraints.Size;

public record CancelTaskRequest(
        @Size(max = 255) String reason
) {
}
