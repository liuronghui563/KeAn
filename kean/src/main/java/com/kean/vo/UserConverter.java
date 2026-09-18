package com.kean.vo;

import com.kean.entity.SysUser;

public final class UserConverter {

    private UserConverter() {
    }

    public static UserVO toVo(SysUser user) {
        return toVo(user, null, null);
    }

    public static UserVO toVo(SysUser user, String schoolName, String campusName) {
        return new UserVO(
                user.getId(),
                user.getRole(),
                user.getUsername(),
                user.getPhone(),
                user.getNickname(),
                user.getGender(),
                user.getAvatarUrl(),
                user.getSchoolId(),
                user.getCampusId(),
                schoolName,
                campusName,
                user.getCompletedCount(),
                user.getCancelledCount(),
                user.getReportedCount(),
                user.getStatus(),
                user.getForbidPublish(),
                user.getForbidApply(),
                user.getMuted(),
                user.getCreatedAt()
        );
    }
}
