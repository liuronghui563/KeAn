-- 与 kean/src/main/resources/db/migration/V1__init_base_tables.sql 保持一致。
CREATE DATABASE IF NOT EXISTS KeBang DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE KeBang;
-- 课安 V1 全量表结构（KeBang）
-- 课程名称快照字段：substitute_task.course_name_snapshot
-- 任务状态：WAITING / APPLYING / MATCHED / CONFIRMED / IN_PROGRESS / COMPLETED / CANCELLED / EXPIRED

CREATE TABLE school (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL COMMENT '学校名称',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学校';

CREATE TABLE campus (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    school_id   BIGINT       NOT NULL COMMENT '所属学校',
    name        VARCHAR(100) NOT NULL COMMENT '校区名称',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_campus_school_id (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校区';

CREATE TABLE course (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    school_id    BIGINT       NOT NULL COMMENT '所属学校',
    course_code  VARCHAR(64)  NOT NULL COMMENT '课程代码',
    course_name  VARCHAR(128) NOT NULL COMMENT '课程名称',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted      TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_school_code (school_id, course_code),
    INDEX idx_course_school_id (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程目录，V1 由管理员维护';

CREATE TABLE sys_user (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role             VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
    username         VARCHAR(32)  NOT NULL COMMENT '登录用户名',
    phone            VARCHAR(20)  NULL COMMENT '手机号，选填',
    password_hash    VARCHAR(100) NOT NULL,
    nickname         VARCHAR(32)  NOT NULL,
    avatar_url       VARCHAR(512) NULL,
    school_id        BIGINT       NULL COMMENT '学生必填，管理员可空',
    campus_id        BIGINT       NULL,
    completed_count  INT          NOT NULL DEFAULT 0,
    cancelled_count  INT          NOT NULL DEFAULT 0,
    reported_count   INT          NOT NULL DEFAULT 0,
    status           VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / BANNED',
    forbid_publish   TINYINT      NOT NULL DEFAULT 0 COMMENT '禁止发布',
    forbid_apply     TINYINT      NOT NULL DEFAULT 0 COMMENT '禁止申请',
    muted            TINYINT      NOT NULL DEFAULT 0 COMMENT '禁言',
    last_login_at    DATETIME     NULL,
    last_login_ip    VARCHAR(64)  NULL,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_phone (phone),
    INDEX idx_sys_user_school_id (school_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

CREATE TABLE substitute_task (
    id                        BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    publisher_id              BIGINT         NOT NULL COMMENT '发布者',
    course_id                 BIGINT         NOT NULL COMMENT '课程目录ID',
    course_name_snapshot      VARCHAR(128)   NOT NULL COMMENT '发布时课程名称快照，不随目录变更',
    task_date                 DATE           NOT NULL COMMENT '上课日期',
    start_time                TIME           NOT NULL,
    end_time                  TIME           NOT NULL,
    start_at                  DATETIME       NOT NULL COMMENT '开始时间，供过期/到点扫描',
    end_at                    DATETIME       NOT NULL,
    school_id                 BIGINT         NOT NULL,
    campus_id                 BIGINT         NOT NULL,
    building                  VARCHAR(64)    NOT NULL COMMENT '教学楼',
    classroom                 VARCHAR(64)    NOT NULL COMMENT '教室',
    reward                    DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '酬谢金额，允许0，纯展示',
    reason                    VARCHAR(500)   NULL COMMENT '代课原因',
    requirement               VARCHAR(500)   NULL COMMENT '代课要求',
    remark                    VARCHAR(500)   NULL,
    status                    VARCHAR(20)    NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/APPLYING/MATCHED/CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED/EXPIRED',
    apply_count               INT            NOT NULL DEFAULT 0,
    accepted_application_id   BIGINT         NULL COMMENT 'MATCHED 时选中的申请',
    publisher_confirmed       TINYINT        NOT NULL DEFAULT 0 COMMENT 'MATCHED 阶段发布者履约确认',
    applicant_confirmed       TINYINT        NOT NULL DEFAULT 0 COMMENT 'MATCHED 阶段代课者履约确认',
    publisher_completed       TINYINT        NOT NULL DEFAULT 0 COMMENT 'IN_PROGRESS 阶段发布者完成确认',
    applicant_completed       TINYINT        NOT NULL DEFAULT 0 COMMENT 'IN_PROGRESS 阶段代课者完成确认',
    cancel_reason             VARCHAR(255)   NULL,
    cancelled_by              VARCHAR(16)    NULL COMMENT 'USER / ADMIN',
    deleted                   TINYINT        NOT NULL DEFAULT 0,
    created_at                DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_school_status_start (school_id, status, start_at),
    INDEX idx_task_publisher (publisher_id),
    INDEX idx_task_campus (campus_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代课任务';

CREATE TABLE substitute_application (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id       BIGINT       NOT NULL,
    applicant_id  BIGINT       NOT NULL,
    message       VARCHAR(500) NULL COMMENT '申请留言',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/CANCELLED',
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_application_task_user (task_id, applicant_id),
    INDEX idx_application_applicant (applicant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代课申请';

CREATE TABLE chat_session (
    id               BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_a_id        BIGINT   NOT NULL COMMENT '较小用户ID',
    user_b_id        BIGINT   NOT NULL COMMENT '较大用户ID',
    task_id          BIGINT   NULL COMMENT '关联代课任务，可空',
    last_message_at  DATETIME NULL,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_chat_session_users (user_a_id, user_b_id),
    INDEX idx_chat_session_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私聊会话';

CREATE TABLE chat_message (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id  BIGINT        NOT NULL,
    sender_id   BIGINT        NOT NULL,
    msg_type    VARCHAR(16)   NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT / IMAGE',
    content     VARCHAR(2000) NOT NULL COMMENT '文本或图片 object key',
    deleted     TINYINT       NOT NULL DEFAULT 0,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_message_session_time (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私聊消息';

CREATE TABLE notification (
    id          BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    type        VARCHAR(32)   NOT NULL COMMENT 'SYSTEM/APPLICATION/TASK/CHAT',
    title       VARCHAR(100)  NOT NULL,
    content     VARCHAR(500)  NOT NULL,
    biz_type    VARCHAR(32)   NULL,
    biz_id      BIGINT        NULL,
    read_flag   TINYINT       NOT NULL DEFAULT 0,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_user_read (user_id, read_flag, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知';

CREATE TABLE review (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT       NOT NULL,
    from_user_id    BIGINT       NOT NULL,
    to_user_id      BIGINT       NOT NULL,
    completed_flag  TINYINT      NOT NULL DEFAULT 1 COMMENT '是否完成',
    rating          TINYINT      NOT NULL COMMENT '1-5 星',
    tags_json       JSON         NULL COMMENT '评价标签',
    content         VARCHAR(500) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_review_task_from (task_id, from_user_id),
    INDEX idx_review_to_user (to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='互评';

CREATE TABLE report (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    reporter_id    BIGINT        NOT NULL,
    target_type    VARCHAR(16)   NOT NULL COMMENT 'USER / TASK / MESSAGE',
    target_id      BIGINT        NOT NULL,
    type           VARCHAR(32)   NOT NULL COMMENT 'FAKE/HARASS/MALICIOUS_CANCEL/FRAUD/VIOLATION/OTHER',
    description    VARCHAR(500)  NULL,
    images_json    JSON          NULL COMMENT '证据图片 object key 列表',
    status         VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/RESOLVED/REJECTED',
    handler_id     BIGINT        NULL,
    handle_result  VARCHAR(32)   NULL COMMENT 'WARN/DELETE/RESTRICT/BAN/REJECT',
    handle_remark  VARCHAR(500)  NULL,
    handled_at     DATETIME      NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_report_status (status, created_at),
    INDEX idx_report_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报';

CREATE TABLE user_blacklist (
    id               BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT   NOT NULL COMMENT '拉黑发起人',
    blocked_user_id  BIGINT   NOT NULL COMMENT '被拉黑用户',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_blacklist_pair (user_id, blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='黑名单，单向';

CREATE TABLE user_favorite (
    id          BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    task_id     BIGINT   NOT NULL COMMENT '收藏的代课任务',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_favorite_user_task (user_id, task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏';

CREATE TABLE operation_log (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    admin_id        BIGINT        NOT NULL,
    admin_name      VARCHAR(32)   NOT NULL,
    operation_type  VARCHAR(64)   NOT NULL,
    target_type     VARCHAR(32)   NOT NULL,
    target_id       VARCHAR(64)   NULL,
    result          VARCHAR(16)   NOT NULL COMMENT 'SUCCESS / FAIL',
    ip              VARCHAR(64)   NULL,
    description     VARCHAR(500)  NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operation_log_admin_time (admin_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志';

CREATE TABLE sys_config (
    id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    config_key    VARCHAR(64)   NOT NULL,
    config_value  VARCHAR(1024) NULL,
    remark        VARCHAR(255)  NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

INSERT INTO school (id, name, status) VALUES (1, '演示大学', 1);
INSERT INTO campus (id, school_id, name, status) VALUES (1, 1, '主校区', 1);
INSERT INTO sys_config (config_key, config_value, remark) VALUES
    ('site.name', '课安', '站点名称'),
    ('register.enabled', '1', '是否开放注册');

