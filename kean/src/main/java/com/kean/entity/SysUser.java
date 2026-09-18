package com.kean.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String role;

    private String username;

    private String phone;

    private String passwordHash;

    private String nickname;

    private String gender;

    private String avatarUrl;

    private Long schoolId;

    private Long campusId;

    private Integer completedCount;

    private Integer cancelledCount;

    private Integer reportedCount;

    private String status;

    private Integer forbidPublish;

    private Integer forbidApply;

    private Integer muted;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
