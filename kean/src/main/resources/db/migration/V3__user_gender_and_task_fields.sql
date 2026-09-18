ALTER TABLE sys_user
    ADD COLUMN gender VARCHAR(16) NULL COMMENT 'MALE/FEMALE' AFTER nickname;

ALTER TABLE substitute_task
    MODIFY COLUMN course_id BIGINT NULL COMMENT '自填课程时为空';

ALTER TABLE substitute_task
    ADD COLUMN computer_lab TINYINT NOT NULL DEFAULT 0 COMMENT '是否上机 1是 0否' AFTER classroom;

ALTER TABLE substitute_task
    ADD COLUMN gender_requirement VARCHAR(16) NOT NULL DEFAULT 'ANY' COMMENT 'ANY/MALE/FEMALE' AFTER computer_lab;
