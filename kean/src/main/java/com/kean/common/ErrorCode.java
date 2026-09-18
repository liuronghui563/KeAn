package com.kean.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS(0, "ok", HttpStatus.OK),
    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    SCHOOL_INVALID(40001, "学校或校区无效", HttpStatus.BAD_REQUEST),
    COURSE_INVALID(40002, "课程无效", HttpStatus.BAD_REQUEST),
    TIME_INVALID(40003, "上课时间不合法", HttpStatus.BAD_REQUEST),
    TASK_NOT_EDITABLE(40004, "当前状态不允许修改或删除", HttpStatus.BAD_REQUEST),
    TASK_STATUS_INVALID(40005, "当前状态不允许该操作", HttpStatus.BAD_REQUEST),
    CANNOT_APPLY_OWN(40006, "不能申请自己发布的代课", HttpStatus.BAD_REQUEST),
    GENDER_INVALID(40007, "请选择性别", HttpStatus.BAD_REQUEST),
    GENDER_NOT_MATCH(40008, "不符合该任务的性别要求", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "未登录或登录已失效", HttpStatus.UNAUTHORIZED),
    LOGIN_FAILED(40101, "用户名或密码错误", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "没有权限", HttpStatus.FORBIDDEN),
    ACCOUNT_BANNED(40301, "账号已被封禁", HttpStatus.FORBIDDEN),
    FORBID_PUBLISH(40302, "账号已被禁止发布", HttpStatus.FORBIDDEN),
    FORBID_APPLY(40303, "账号已被禁止申请", HttpStatus.FORBIDDEN),
    TASK_NOT_FOUND(40401, "代课任务不存在", HttpStatus.NOT_FOUND),
    APPLICATION_NOT_FOUND(40402, "申请不存在", HttpStatus.NOT_FOUND),
    USERNAME_EXISTS(40901, "用户名已存在", HttpStatus.CONFLICT),
    PHONE_EXISTS(40902, "手机号已被注册", HttpStatus.CONFLICT),
    ALREADY_APPLIED(40903, "已经申请过该任务", HttpStatus.CONFLICT),
    INTERNAL_ERROR(50000, "服务器内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
