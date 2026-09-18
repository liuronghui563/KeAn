package com.kean.service;

import com.kean.common.ErrorCode;
import com.kean.entity.SubstituteTask;
import com.kean.enums.TaskStatus;
import com.kean.enums.UserRole;
import com.kean.exception.BizException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class TaskStatusService {

    public void initWaiting(SubstituteTask task) {
        task.setStatus(TaskStatus.WAITING.name());
    }

    public void assertEditable(SubstituteTask task) {
        if (!TaskStatus.WAITING.name().equals(task.getStatus())
                || (task.getApplyCount() != null && task.getApplyCount() > 0)) {
            throw new BizException(ErrorCode.TASK_NOT_EDITABLE);
        }
    }

    public void assertCanApply(SubstituteTask task) {
        String status = task.getStatus();
        if (!TaskStatus.WAITING.name().equals(status) && !TaskStatus.APPLYING.name().equals(status)) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "当前状态不可申请");
        }
        if (task.getStartAt() != null && !task.getStartAt().isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "任务已到上课时间，不能申请");
        }
    }

    public void onApplicationCreated(SubstituteTask task) {
        assertCanApply(task);
        if (TaskStatus.WAITING.name().equals(task.getStatus())) {
            task.setStatus(TaskStatus.APPLYING.name());
        }
    }

    public void onNoPendingApplications(SubstituteTask task) {
        if (TaskStatus.APPLYING.name().equals(task.getStatus())) {
            task.setStatus(TaskStatus.WAITING.name());
        }
    }

    public void onAccepted(SubstituteTask task, Long applicationId) {
        if (!TaskStatus.APPLYING.name().equals(task.getStatus())) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "当前状态不能选人");
        }
        task.setAcceptedApplicationId(applicationId);
        task.setStatus(TaskStatus.MATCHED.name());
    }

    public void onPublisherConfirm(SubstituteTask task) {
        if (!TaskStatus.MATCHED.name().equals(task.getStatus())) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "当前状态不能确认履约");
        }
        task.setPublisherConfirmed(1);
        maybeConfirm(task);
    }

    public void onApplicantConfirm(SubstituteTask task) {
        if (!TaskStatus.MATCHED.name().equals(task.getStatus())) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "当前状态不能确认履约");
        }
        task.setApplicantConfirmed(1);
        maybeConfirm(task);
    }

    public void toInProgressIfDue(SubstituteTask task) {
        if (TaskStatus.CONFIRMED.name().equals(task.getStatus())
                && task.getStartAt() != null
                && !task.getStartAt().isAfter(LocalDateTime.now())) {
            task.setStatus(TaskStatus.IN_PROGRESS.name());
        }
    }

    public void onPublisherComplete(SubstituteTask task) {
        if (!TaskStatus.IN_PROGRESS.name().equals(task.getStatus())) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "当前状态不能确认完成");
        }
        task.setPublisherCompleted(1);
        maybeComplete(task);
    }

    public void onApplicantComplete(SubstituteTask task) {
        if (!TaskStatus.IN_PROGRESS.name().equals(task.getStatus())) {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "当前状态不能确认完成");
        }
        task.setApplicantCompleted(1);
        maybeComplete(task);
    }

    public void cancelByUser(SubstituteTask task, String reason, boolean publisher, boolean acceptedApplicant) {
        String status = task.getStatus();
        if (TaskStatus.WAITING.name().equals(status) || TaskStatus.APPLYING.name().equals(status)) {
            if (!publisher) {
                throw new BizException(ErrorCode.FORBIDDEN, "仅发布者可取消");
            }
        } else if (TaskStatus.MATCHED.name().equals(status)) {
            if (!publisher && !acceptedApplicant) {
                throw new BizException(ErrorCode.FORBIDDEN, "仅发布者或已选代课者可取消");
            }
        } else if (TaskStatus.CONFIRMED.name().equals(status)) {
            if (!publisher && !acceptedApplicant) {
                throw new BizException(ErrorCode.FORBIDDEN, "仅双方可取消");
            }
        } else if (TaskStatus.IN_PROGRESS.name().equals(status)) {
            throw new BizException(ErrorCode.FORBIDDEN, "进行中仅管理员可强制取消");
        } else {
            throw new BizException(ErrorCode.TASK_STATUS_INVALID, "当前状态不能取消");
        }
        task.setStatus(TaskStatus.CANCELLED.name());
        task.setCancelReason(reason);
        task.setCancelledBy("USER");
    }

    public void expire(SubstituteTask task) {
        String status = task.getStatus();
        if (TaskStatus.WAITING.name().equals(status) || TaskStatus.APPLYING.name().equals(status)) {
            task.setStatus(TaskStatus.EXPIRED.name());
            return;
        }
        throw new BizException(ErrorCode.TASK_STATUS_INVALID);
    }

    public boolean isAdmin(String role) {
        return Objects.equals(UserRole.ADMIN.name(), role);
    }

    private void maybeConfirm(SubstituteTask task) {
        if (isSet(task.getPublisherConfirmed()) && isSet(task.getApplicantConfirmed())) {
            task.setStatus(TaskStatus.CONFIRMED.name());
            toInProgressIfDue(task);
        }
    }

    private void maybeComplete(SubstituteTask task) {
        if (isSet(task.getPublisherCompleted()) && isSet(task.getApplicantCompleted())) {
            task.setStatus(TaskStatus.COMPLETED.name());
        }
    }

    private boolean isSet(Integer flag) {
        return flag != null && flag == 1;
    }
}
