package com.kean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kean.common.ErrorCode;
import com.kean.common.PageResult;
import com.kean.dto.CancelTaskRequest;
import com.kean.dto.CreateTaskRequest;
import com.kean.dto.TaskQuery;
import com.kean.entity.Campus;
import com.kean.entity.SubstituteApplication;
import com.kean.entity.SubstituteTask;
import com.kean.entity.SysUser;
import com.kean.enums.ApplicationStatus;
import com.kean.enums.TaskStatus;
import com.kean.enums.UserStatus;
import com.kean.exception.BizException;
import com.kean.mapper.CampusMapper;
import com.kean.mapper.SubstituteApplicationMapper;
import com.kean.mapper.SubstituteTaskMapper;
import com.kean.mapper.SysUserMapper;
import com.kean.security.LoginUser;
import com.kean.security.SecurityUtils;
import com.kean.service.TaskService;
import com.kean.service.TaskStatusService;
import com.kean.vo.PublisherBriefVO;
import com.kean.vo.TaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private static final long DEFAULT_SCHOOL_ID = 1L;
    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_SIZE = 50L;

    private final SubstituteTaskMapper taskMapper;
    private final CampusMapper campusMapper;
    private final SysUserMapper sysUserMapper;
    private final SubstituteApplicationMapper applicationMapper;
    private final TaskStatusService taskStatusService;

    public TaskServiceImpl(
            SubstituteTaskMapper taskMapper,
            CampusMapper campusMapper,
            SysUserMapper sysUserMapper,
            SubstituteApplicationMapper applicationMapper,
            TaskStatusService taskStatusService
    ) {
        this.taskMapper = taskMapper;
        this.campusMapper = campusMapper;
        this.sysUserMapper = sysUserMapper;
        this.applicationMapper = applicationMapper;
        this.taskStatusService = taskStatusService;
    }

    @Override
    public PageResult<TaskVO> list(TaskQuery query) {
        long pageNo = query.page() == null || query.page() < 1 ? DEFAULT_PAGE : query.page();
        long size = query.size() == null || query.size() < 1 ? DEFAULT_SIZE : Math.min(query.size(), MAX_SIZE);
        Long schoolId = resolveViewerSchoolId(query.schoolId());

        LambdaQueryWrapper<SubstituteTask> wrapper = new LambdaQueryWrapper<SubstituteTask>()
                .eq(SubstituteTask::getSchoolId, schoolId)
                .ge(SubstituteTask::getStartAt, LocalDateTime.now());
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(SubstituteTask::getStatus, query.status().trim().toUpperCase());
        } else {
            wrapper.in(SubstituteTask::getStatus, TaskStatus.WAITING.name(), TaskStatus.APPLYING.name());
        }
        if (query.courseId() != null) {
            wrapper.eq(SubstituteTask::getCourseId, query.courseId());
        }
        if (query.campusId() != null) {
            wrapper.eq(SubstituteTask::getCampusId, query.campusId());
        }
        if (StringUtils.hasText(query.taskDate())) {
            wrapper.eq(SubstituteTask::getTaskDate, parseDate(query.taskDate()));
        }
        if (StringUtils.hasText(query.keyword())) {
            String keyword = query.keyword().trim();
            wrapper.and(w -> w.like(SubstituteTask::getCourseNameSnapshot, keyword)
                    .or().like(SubstituteTask::getBuilding, keyword)
                    .or().like(SubstituteTask::getClassroom, keyword));
        }
        wrapper.orderByAsc(SubstituteTask::getStartAt).orderByDesc(SubstituteTask::getId);

        Page<SubstituteTask> page = taskMapper.selectPage(new Page<>(pageNo, size), wrapper);
        Map<Long, String> campusNames = loadCampusNames(page.getRecords());
        List<TaskVO> list = page.getRecords().stream()
                .map(task -> toVo(task, campusNames.get(task.getCampusId()), null, false, null, null, false))
                .toList();
        return new PageResult<>(list, page.getTotal(), pageNo, size);
    }

    @Override
    public TaskVO detail(Long id) {
        SubstituteTask task = requireVisibleTask(id);
        Campus campus = campusMapper.selectById(task.getCampusId());
        SysUser publisher = sysUserMapper.selectById(task.getPublisherId());
        PublisherBriefVO brief = publisher == null ? null : new PublisherBriefVO(
                publisher.getId(),
                publisher.getNickname(),
                publisher.getAvatarUrl(),
                publisher.getCompletedCount()
        );
        LoginUser loginUser = SecurityUtils.currentUserOrNull();
        boolean mine = loginUser != null && Objects.equals(loginUser.userId(), task.getPublisherId());
        String myApplicationStatus = null;
        Long myApplicationId = null;
        boolean matchedApplicant = false;
        if (loginUser != null) {
            SubstituteApplication mineApp = applicationMapper.selectOne(new LambdaQueryWrapper<SubstituteApplication>()
                    .eq(SubstituteApplication::getTaskId, task.getId())
                    .eq(SubstituteApplication::getApplicantId, loginUser.userId()));
            if (mineApp != null) {
                myApplicationStatus = mineApp.getStatus();
                myApplicationId = mineApp.getId();
            }
            matchedApplicant = isAcceptedApplicant(task, loginUser.userId());
        }
        return toVo(task, campus == null ? null : campus.getName(), brief, mine, myApplicationStatus, myApplicationId, matchedApplicant);
    }

    @Override
    @Transactional
    public TaskVO create(CreateTaskRequest request) {
        SysUser user = requirePublisher();
        SubstituteTask task = new SubstituteTask();
        task.setPublisherId(user.getId());
        task.setSchoolId(user.getSchoolId());
        task.setApplyCount(0);
        task.setPublisherConfirmed(0);
        task.setApplicantConfirmed(0);
        task.setPublisherCompleted(0);
        task.setApplicantCompleted(0);
        fillContent(task, request, user.getSchoolId());
        taskStatusService.initWaiting(task);
        taskMapper.insert(task);
        return detail(task.getId());
    }

    @Override
    @Transactional
    public TaskVO update(Long id, CreateTaskRequest request) {
        SysUser user = requirePublisher();
        SubstituteTask task = requireOwnedTask(id, user.getId());
        taskStatusService.assertEditable(task);
        fillContent(task, request, user.getSchoolId());
        taskMapper.updateById(task);
        return detail(task.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysUser user = requirePublisher();
        SubstituteTask task = requireOwnedTask(id, user.getId());
        taskStatusService.assertEditable(task);
        taskMapper.deleteById(task.getId());
    }

    @Override
    @Transactional
    public TaskVO confirm(Long id) {
        Long userId = SecurityUtils.currentUserId();
        SubstituteTask task = requireVisibleTask(id);
        if (Objects.equals(task.getPublisherId(), userId)) {
            taskStatusService.onPublisherConfirm(task);
        } else if (isAcceptedApplicant(task, userId)) {
            taskStatusService.onApplicantConfirm(task);
        } else {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        taskMapper.updateById(task);
        return detail(id);
    }

    @Override
    @Transactional
    public TaskVO complete(Long id) {
        Long userId = SecurityUtils.currentUserId();
        SubstituteTask task = requireVisibleTask(id);
        if (Objects.equals(task.getPublisherId(), userId)) {
            taskStatusService.onPublisherComplete(task);
        } else if (isAcceptedApplicant(task, userId)) {
            taskStatusService.onApplicantComplete(task);
        } else {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        taskMapper.updateById(task);
        if (TaskStatus.COMPLETED.name().equals(task.getStatus())) {
            bumpCompleted(task.getPublisherId());
            Long applicantId = acceptedApplicantId(task);
            if (applicantId != null) {
                bumpCompleted(applicantId);
            }
        }
        return detail(id);
    }

    @Override
    @Transactional
    public TaskVO cancel(Long id, CancelTaskRequest request) {
        Long userId = SecurityUtils.currentUserId();
        SubstituteTask task = requireVisibleTask(id);
        boolean publisher = Objects.equals(task.getPublisherId(), userId);
        boolean acceptedApplicant = isAcceptedApplicant(task, userId);
        String reason = request == null ? null : request.reason();
        taskStatusService.cancelByUser(task, trimToNull(reason), publisher, acceptedApplicant);
        taskMapper.updateById(task);
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            int cancelled = user.getCancelledCount() == null ? 0 : user.getCancelledCount();
            user.setCancelledCount(cancelled + 1);
            sysUserMapper.updateById(user);
        }
        return detail(id);
    }

    @Override
    public PageResult<TaskVO> listMyPublished(Long page, Long size) {
        Long userId = SecurityUtils.currentUserId();
        return pageTasks(new LambdaQueryWrapper<SubstituteTask>()
                .eq(SubstituteTask::getPublisherId, userId)
                .orderByDesc(SubstituteTask::getCreatedAt), page, size);
    }

    @Override
    public PageResult<TaskVO> listMyApplied(Long page, Long size) {
        Long userId = SecurityUtils.currentUserId();
        List<SubstituteApplication> applications = applicationMapper.selectList(
                new LambdaQueryWrapper<SubstituteApplication>()
                        .eq(SubstituteApplication::getApplicantId, userId)
                        .orderByDesc(SubstituteApplication::getCreatedAt)
        );
        List<Long> taskIds = applications.stream().map(SubstituteApplication::getTaskId).distinct().toList();
        if (taskIds.isEmpty()) {
            long pageNo = page == null || page < 1 ? DEFAULT_PAGE : page;
            long pageSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
            return new PageResult<>(List.of(), 0, pageNo, pageSize);
        }
        return pageTasks(new LambdaQueryWrapper<SubstituteTask>()
                .in(SubstituteTask::getId, taskIds)
                .orderByDesc(SubstituteTask::getCreatedAt), page, size);
    }

    private void fillContent(SubstituteTask task, CreateTaskRequest request, Long schoolId) {
        Campus campus = campusMapper.selectById(request.campusId());
        if (campus == null || !Objects.equals(campus.getSchoolId(), schoolId) || campus.getStatus() == null || campus.getStatus() != 1) {
            throw new BizException(ErrorCode.SCHOOL_INVALID);
        }
        LocalDateTime startAt = LocalDateTime.of(request.taskDate(), request.startTime());
        LocalDateTime endAt = LocalDateTime.of(request.taskDate(), request.endTime());
        if (!endAt.isAfter(startAt)) {
            throw new BizException(ErrorCode.TIME_INVALID, "结束时间必须晚于开始时间");
        }
        if (!startAt.isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.TIME_INVALID, "上课时间不能早于当前时间");
        }
        task.setCourseId(null);
        task.setCourseNameSnapshot(request.courseName().trim());
        task.setTaskDate(request.taskDate());
        task.setStartTime(request.startTime());
        task.setEndTime(request.endTime());
        task.setStartAt(startAt);
        task.setEndAt(endAt);
        task.setCampusId(campus.getId());
        task.setBuilding(request.building().trim());
        task.setClassroom(request.classroom().trim());
        task.setComputerLab(Boolean.TRUE.equals(request.computerLab()) ? 1 : 0);
        task.setGenderRequirement(request.genderRequirement());
        task.setReward(request.reward());
        task.setReason(trimToNull(request.reason()));
        task.setRequirement(trimToNull(request.requirement()));
        task.setRemark(trimToNull(request.remark()));
    }

    private SysUser requirePublisher() {
        SysUser user = sysUserMapper.selectById(SecurityUtils.currentUserId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (UserStatus.BANNED.name().equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_BANNED);
        }
        if (user.getForbidPublish() != null && user.getForbidPublish() == 1) {
            throw new BizException(ErrorCode.FORBID_PUBLISH);
        }
        return user;
    }

    private SubstituteTask requireOwnedTask(Long id, Long userId) {
        SubstituteTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        if (!Objects.equals(task.getPublisherId(), userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return task;
    }

    private SubstituteTask requireVisibleTask(Long id) {
        SubstituteTask task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        Long schoolId = resolveViewerSchoolId(null);
        if (!Objects.equals(task.getSchoolId(), schoolId)) {
            throw new BizException(ErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }

    private Long resolveViewerSchoolId(Long querySchoolId) {
        LoginUser loginUser = SecurityUtils.currentUserOrNull();
        if (loginUser != null) {
            SysUser user = sysUserMapper.selectById(loginUser.userId());
            if (user != null && user.getSchoolId() != null) {
                return user.getSchoolId();
            }
        }
        return querySchoolId == null ? DEFAULT_SCHOOL_ID : querySchoolId;
    }

    private Map<Long, String> loadCampusNames(List<SubstituteTask> tasks) {
        Set<Long> ids = tasks.stream().map(SubstituteTask::getCampusId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        campusMapper.selectByIds(ids).forEach(campus -> names.put(campus.getId(), campus.getName()));
        return names;
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, "日期格式应为 yyyy-MM-dd");
        }
    }

    private PageResult<TaskVO> pageTasks(LambdaQueryWrapper<SubstituteTask> wrapper, Long page, Long size) {
        long pageNo = page == null || page < 1 ? DEFAULT_PAGE : page;
        long pageSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        Page<SubstituteTask> result = taskMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        Map<Long, String> campusNames = loadCampusNames(result.getRecords());
        LoginUser loginUser = SecurityUtils.currentUserOrNull();
        List<TaskVO> list = result.getRecords().stream().map(task -> {
            boolean mine = loginUser != null && Objects.equals(loginUser.userId(), task.getPublisherId());
            boolean matched = loginUser != null && isAcceptedApplicant(task, loginUser.userId());
            return toVo(task, campusNames.get(task.getCampusId()), null, mine, null, null, matched);
        }).toList();
        return new PageResult<>(list, result.getTotal(), pageNo, pageSize);
    }

    private boolean isAcceptedApplicant(SubstituteTask task, Long userId) {
        Long applicantId = acceptedApplicantId(task);
        return applicantId != null && Objects.equals(applicantId, userId);
    }

    private Long acceptedApplicantId(SubstituteTask task) {
        if (task.getAcceptedApplicationId() == null) {
            return null;
        }
        SubstituteApplication application = applicationMapper.selectById(task.getAcceptedApplicationId());
        if (application == null || !ApplicationStatus.ACCEPTED.name().equals(application.getStatus())) {
            return null;
        }
        return application.getApplicantId();
    }

    private void bumpCompleted(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return;
        }
        int completed = user.getCompletedCount() == null ? 0 : user.getCompletedCount();
        user.setCompletedCount(completed + 1);
        sysUserMapper.updateById(user);
    }

    private TaskVO toVo(
            SubstituteTask task,
            String campusName,
            PublisherBriefVO publisher,
            boolean mine,
            String myApplicationStatus,
            Long myApplicationId,
            boolean matchedApplicant
    ) {
        return new TaskVO(
                task.getId(),
                task.getPublisherId(),
                task.getCourseId(),
                task.getCourseNameSnapshot(),
                task.getTaskDate(),
                task.getStartTime(),
                task.getEndTime(),
                task.getStartAt(),
                task.getEndAt(),
                task.getSchoolId(),
                task.getCampusId(),
                campusName,
                task.getBuilding(),
                task.getClassroom(),
                task.getComputerLab(),
                task.getGenderRequirement(),
                task.getReward(),
                task.getReason(),
                task.getRequirement(),
                task.getRemark(),
                task.getStatus(),
                task.getApplyCount(),
                task.getCreatedAt(),
                publisher,
                mine,
                task.getPublisherConfirmed(),
                task.getApplicantConfirmed(),
                task.getPublisherCompleted(),
                task.getApplicantCompleted(),
                task.getAcceptedApplicationId(),
                myApplicationStatus,
                myApplicationId,
                matchedApplicant
        );
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
