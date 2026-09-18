package com.kean.dto;

import jakarta.validation.constraints.Size;

public record ApplyRequest(
        @Size(max = 500) String message
) {
}
