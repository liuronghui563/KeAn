package com.kean.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kean.config.AdminProperties;
import com.kean.entity.SysUser;
import com.kean.enums.UserRole;
import com.kean.enums.UserStatus;
import com.kean.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final SysUserMapper sysUserMapper;
    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(
            SysUserMapper sysUserMapper,
            AdminProperties adminProperties,
            PasswordEncoder passwordEncoder
    ) {
        this.sysUserMapper = sysUserMapper;
        this.adminProperties = adminProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long adminCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, UserRole.ADMIN.name())
        );
        if (adminCount != null && adminCount > 0) {
            return;
        }
        if (!StringUtils.hasText(adminProperties.getPassword())) {
            log.warn("未配置 ADMIN_PASSWORD，跳过种子管理员");
            return;
        }

        SysUser existed = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, adminProperties.getUsername())
        );
        if (existed != null) {
            log.warn("用户名 {} 已存在且不是管理员，跳过种子管理员", adminProperties.getUsername());
            return;
        }

        SysUser admin = new SysUser();
        admin.setRole(UserRole.ADMIN.name());
        admin.setUsername(adminProperties.getUsername());
        admin.setPasswordHash(passwordEncoder.encode(adminProperties.getPassword()));
        admin.setNickname(adminProperties.getNickname());
        admin.setCompletedCount(0);
        admin.setCancelledCount(0);
        admin.setReportedCount(0);
        admin.setStatus(UserStatus.NORMAL.name());
        admin.setForbidPublish(0);
        admin.setForbidApply(0);
        admin.setMuted(0);
        sysUserMapper.insert(admin);
        log.info("已创建种子管理员账号 {}", adminProperties.getUsername());
    }
}
