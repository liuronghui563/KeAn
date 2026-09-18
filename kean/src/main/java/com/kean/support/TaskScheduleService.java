package com.kean.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kean.entity.SubstituteTask;
import com.kean.enums.TaskStatus;
import com.kean.mapper.SubstituteTaskMapper;
import com.kean.service.TaskStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class TaskScheduleService {

    private static final Logger log = LoggerFactory.getLogger(TaskScheduleService.class);

    private final SubstituteTaskMapper taskMapper;
    private final TaskStatusService taskStatusService;

    public TaskScheduleService(SubstituteTaskMapper taskMapper, TaskStatusService taskStatusService) {
        this.taskMapper = taskMapper;
        this.taskStatusService = taskStatusService;
    }

    @Scheduled(fixedDelay = 30000)
    public void refreshStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<SubstituteTask> dueExpire = taskMapper.selectList(new LambdaQueryWrapper<SubstituteTask>()
                .in(SubstituteTask::getStatus, TaskStatus.WAITING.name(), TaskStatus.APPLYING.name())
                .le(SubstituteTask::getStartAt, now));
        for (SubstituteTask task : dueExpire) {
            try {
                taskStatusService.expire(task);
                taskMapper.updateById(task);
            } catch (Exception ex) {
                log.warn("Expire task {} failed: {}", task.getId(), ex.getMessage());
            }
        }
        List<SubstituteTask> dueStart = taskMapper.selectList(new LambdaQueryWrapper<SubstituteTask>()
                .eq(SubstituteTask::getStatus, TaskStatus.CONFIRMED.name())
                .le(SubstituteTask::getStartAt, now));
        for (SubstituteTask task : dueStart) {
            try {
                taskStatusService.toInProgressIfDue(task);
                taskMapper.updateById(task);
            } catch (Exception ex) {
                log.warn("Start task {} failed: {}", task.getId(), ex.getMessage());
            }
        }
    }
}
