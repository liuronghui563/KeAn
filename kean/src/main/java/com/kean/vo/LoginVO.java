package com.kean.vo;

public record LoginVO(
        String token,
        UserVO user
) {
}
