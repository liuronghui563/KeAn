package com.kean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.kean.common.ErrorCode;
import com.kean.dto.ApplyRequest;
import com.kean.entity.SubstituteApplication;
import com.kean.entity.SubstituteTask;
import com.kean.entity.SysUser;
import com.kean.enums.ApplicationStatus;
import com.kean.enums.UserStatus;
import com.kean.exception.BizException;
import com.kean.mapper.SubstituteApplicationMapper;
import com.kean.mapper.SubstituteTaskMapper;
import com.kean.mapper.SysUserMapper;
import com.kean.security.SecurityUtils;
import com.kean.service.ApplicationService;
import com.kean.service.TaskService;
import com.kean.service.TaskStatusService;
import com.kean.vo.ApplicationVO;
import com.kean.vo.TaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final SubstituteApplicationMapper applicationMapper;
    private final SubstituteTaskMapper taskMapper;
    private final SysUserMapper sysUserMapper;
    private final TaskStatusService taskStatusService;
    private final TaskService taskService;

    public ApplicationServiceImpl(
            SubstituteApplicationMapper applicationMapper,
            SubstituteTaskMapper taskMapper,
            SysUserMapper sysUserMapper,
            TaskStatusService taskStatusService,
            TaskService taskService
    ) {
        this.applicationMapper = applicationMapper;
        this.taskMapper = taskMapper;
        this.sysUserMapper = sysUserMapper;
        this.taskStatusService = taskStatusService;
        this.taskService = taskService;
    }

    @Override
    @Transactional
    public TaskVO apply(Long taskId, ApplyRequest request) {
        SysUser user = requireApplicant();
        SubstituteTask task = requireTask(taskId);
        if (Objects.equals(task.getPublisherId(), user.getId())) {
            throw new BizException(ErrorCode.CANNOT_APPLY_OWN);
        }
        if (!Objects.equals(task.getSchoolId(), user.getSchoolId())) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        taskStatusService.assertCanApply(task);
        assertGenderMatch(task, user);
        String message = trimToNull(request == null ? null : request.message());
        SubstituteApplication existing = applicationMapper.selectOne(new LambdaQueryWrapper<SubstituteApplication>()
                .eq(SubstituteApplication::getTaskId, taskId)
                .eq(SubstituteApplication::getApplicantId, user.getId()));
        if (existing != null) {
            if (!ApplicationStatus.CANCELLED.name().equals(existing.getStatus())) {
                throw new BizException(ErrorCode.ALREADY_APPLIED);
            }
            existing.setMessage(message);
            existing.setStatus(ApplicationStatus.PENDING.name());
            applicationMapper.updateById(existing);
        } else {
            SubstituteApplication application = new SubstituteApplication();
            application.setTaskId(taskId);
            application.setApplicantId(user.getId());
            application.setMessage(message);
            application.setStatus(ApplicationStatus.PENDING.name());
            applicationMapper.insert(application);
        }
        int count = task.getApplyCount() == null ? 0 : task.getApplyCount();
        task.setApplyCount(count + 1);
        taskStatusService.onApplicationCreated(task);
        taskMapper.updateById(task);
        return taskService.detail(taskId);
    }

    @Override
    public List<ApplicationVO> listByTask(Long taskId) {
        Long userId = SecurityUtils.currentUserId();
        SubstituteTask task = requireTask(taskId);
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        List<SubstituteApplication> applications = applicationMapper.selectList(
                new LambdaQueryWrapper<SubstituteApplication>()
                        .eq(SubstituteApplication::getTaskId, taskId)
                        .orderByAsc(SubstituteApplication::getCreatedAt)
        );
        return toVos(applications);
    }

    @Override
    @Transactional
    public TaskVO accept(Long applicationId) {
        Long userId = SecurityUtils.currentUserId();
        SubstituteApplication application = requireApplication(applicationId);
        if (!ApplicationStatus.PENDING.name().equals(application.getStatus())) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "只能接受待处理申请");
        }
        SubstituteTask task = requireTask(application.getTaskId());
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        taskStatusService.onAccepted(task, application.getId());
        application.setStatus(ApplicationStatus.ACCEPTED.name());
        applicationMapper.updateById(application);
        applicationMapper.update(null, new LambdaUpdateWrapper<SubstituteApplication>()
                .eq(SubstituteApplication::getTaskId, task.getId())
                .eq(SubstituteApplication::getStatus, ApplicationStatus.PENDING.name())
                .ne(SubstituteApplication::getId, application.getId())
                .set(SubstituteApplication::getStatus, ApplicationStatus.REJECTED.name()));
        taskMapper.updateById(task);
        return taskService.detail(task.getId());
    }

    @Override
    @Transactional
    public TaskVO reject(Long applicationId) {
        Long userId = SecurityUtils.currentUserId();
        SubstituteApplication application = requireApplication(applicationId);
        if (!ApplicationStatus.PENDING.name().equals(application.getStatus())) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "只能拒绝待处理申请");
        }
        SubstituteTask task = requireTask(application.getTaskId());
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        application.setStatus(ApplicationStatus.REJECTED.name());
        applicationMapper.updateById(application);
        if (countPending(task.getId()) == 0) {
            taskStatusService.onNoPendingApplications(task);
            taskMapper.updateById(task);
        }
        return taskService.detail(task.getId());
    }

    @Override
    @Transactional
    public TaskVO withdraw(Long applicationId) {
        Long userId = SecurityUtils.currentUserId();
        SubstituteApplication application = requireApplication(applicationId);
        if (!Objects.equals(application.getApplicantId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (!ApplicationStatus.PENDING.name().equals(application.getStatus())) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "只能撤回待处理申请");
        }
        SubstituteTask task = requireTask(application.getTaskId());
        application.setStatus(ApplicationStatus.CANCELLED.name());
        applicationMapper.updateById(application);
        int count = task.getApplyCount() == null ? 0 : task.getApplyCount();
        task.setApplyCount(Math.max(0, count - 1));
        if (countPending(task.getId()) == 0) {
            taskStatusService.onNoPendingApplications(task);
        }
        taskMapper.updateById(task);
        return taskService.detail(task.getId());
    }

    private long countPending(Long taskId) {
        return applicationMapper.selectCount(new LambdaQueryWrapper<SubstituteApplication>()
                .eq(SubstituteApplication::getTaskId, taskId)
                .eq(SubstituteApplication::getStatus, ApplicationStatus.PENDING.name()));
    }

    private List<ApplicationVO> toVos(List<SubstituteApplication> applications) {
        Set<Long> userIds = applications.stream().map(SubstituteApplication::getApplicantId).collect(Collectors.toSet());
        Map<Long, String> names = userIds.isEmpty() ? Map.of() : sysUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getNickname, (a, b) -> a));
        return applications.stream()
                .map(item -> new ApplicationVO(
                        item.getId(),
                        item.getTaskId(),
                        item.getApplicantId(),
                        names.get(item.getApplicantId()),
                        item.getMessage(),
                        item.getStatus(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    private void assertGenderMatch(SubstituteTask task, SysUser user) {
        String requirement = task.getGenderRequirement();
        if (!StringUtils.hasText(requirement) || "ANY".equals(requirement)) {
            return;
        }
        if (!requirement.equals(user.getGender())) {
            throw new BizException(ErrorCode.GENDER_NOT_MATCH);
        }
    }

    private SysUser requireApplicant() {
        SysUser user = sysUserMapper.selectById(SecurityUtils.currentUserId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (UserStatus.BANNED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_BANNED);
        }
        if (user.getForbidApply() != null && user.getForbidApply() == 1) {
            throw new BizException(ErrorCode.FORBID_APPLY);
        }
        return user;
    }

    private SubstituteTask requireTask(Long taskId) {
        SubstituteTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }

    private SubstituteApplication requireApplication(Long id) {
        SubstituteApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new BizException(ErrorCode.APPLICATION_NOT_FOUND);
        }
        return application;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
