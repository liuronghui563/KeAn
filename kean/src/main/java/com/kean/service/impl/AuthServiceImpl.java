package com.kean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kean.common.ErrorCode;
import com.kean.dto.LoginRequest;
import com.kean.dto.RegisterRequest;
import com.kean.entity.Campus;
import com.kean.entity.School;
import com.kean.entity.SysUser;
import com.kean.enums.UserRole;
import com.kean.enums.UserStatus;
import com.kean.exception.BizException;
import com.kean.mapper.CampusMapper;
import com.kean.mapper.SchoolMapper;
import com.kean.mapper.SysUserMapper;
import com.kean.security.JwtService;
import com.kean.security.SecurityUtils;
import com.kean.security.TokenBlacklistService;
import com.kean.service.AuthService;
import com.kean.utils.IpUtils;
import com.kean.vo.LoginVO;
import com.kean.vo.UserConverter;
import com.kean.vo.UserVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final int ENABLED = 1;

    private final SysUserMapper sysUserMapper;
    private final SchoolMapper schoolMapper;
    private final CampusMapper campusMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthServiceImpl(
            SysUserMapper sysUserMapper,
            SchoolMapper schoolMapper,
            CampusMapper campusMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.schoolMapper = schoolMapper;
        this.campusMapper = campusMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        assertSchoolAndCampus(request.schoolId(), request.campusId());
        if (existsUsername(request.username())) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        String phone = normalizePhone(request.phone());
        if (phone != null && existsPhone(phone)) {
            throw new BizException(ErrorCode.PHONE_EXISTS);
        }

        SysUser user = new SysUser();
        user.setRole(UserRole.USER.name());
        user.setUsername(request.username());
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setGender(request.gender());
        user.setSchoolId(request.schoolId());
        user.setCampusId(request.campusId());
        user.setCompletedCount(0);
        user.setCancelledCount(0);
        user.setReportedCount(0);
        user.setStatus(UserStatus.NORMAL.name());
        user.setForbidPublish(0);
        user.setForbidApply(0);
        user.setMuted(0);
        sysUserMapper.insert(user);
        log.info("新注册用户：{}，手机号：{}，学校id:{}",user.getUsername(),user.getPhone(),user.getSchoolId());
        return toUserVo(user);
    }

    @Override
    @Transactional
    public LoginVO login(LoginRequest request, HttpServletRequest httpRequest) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username())
        );
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        if (UserStatus.BANNED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_BANNED);
        }
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(IpUtils.clientIp(httpRequest));
        sysUserMapper.updateById(user);

        String token = jwtService.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, toUserVo(user));
    }

    @Override
    public UserVO currentUser() {
        SysUser user = sysUserMapper.selectById(SecurityUtils.currentUserId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (UserStatus.BANNED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_BANNED);
        }
        return toUserVo(user);
    }

    @Override
    public void logout(HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Claims claims = jwtService.parse(header.substring(7));
        Instant expireAt = claims.getExpiration().toInstant();
        long ttlSeconds = Duration.between(Instant.now(), expireAt).getSeconds();
        tokenBlacklistService.blacklist(claims.getId(), ttlSeconds);
    }

    private UserVO toUserVo(SysUser user) {
        String schoolName = null;
        String campusName = null;
        if (user.getSchoolId() != null) {
            School school = schoolMapper.selectById(user.getSchoolId());
            if (school != null) {
                schoolName = school.getName();
            }
        }
        if (user.getCampusId() != null) {
            Campus campus = campusMapper.selectById(user.getCampusId());
            if (campus != null) {
                campusName = campus.getName();
            }
        }
        return UserConverter.toVo(user, schoolName, campusName);
    }

    private void assertSchoolAndCampus(Long schoolId, Long campusId) {
        School school = schoolMapper.selectById(schoolId);
        Campus campus = campusMapper.selectById(campusId);
        if (school == null || campus == null) {
            throw new BizException(ErrorCode.SCHOOL_INVALID);
        }
        if (school.getStatus() == null || school.getStatus() != ENABLED
                || campus.getStatus() == null || campus.getStatus() != ENABLED
                || !schoolId.equals(campus.getSchoolId())) {
            throw new BizException(ErrorCode.SCHOOL_INVALID);
        }
    }

    private boolean existsUsername(String username) {
        return sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        ) > 0;
    }

    private boolean existsPhone(String phone) {
        return sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone)
        ) > 0;
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return phone.trim();
    }
}
