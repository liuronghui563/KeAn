package com.kean.controller;

import com.kean.common.Result;
import com.kean.dto.ApplyRequest;
import com.kean.service.ApplicationService;
import com.kean.vo.ApplicationVO;
import com.kean.vo.TaskVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/tasks/{taskId}/applications")
    public Result<TaskVO> apply(@PathVariable Long taskId, @Valid @RequestBody(required = false) ApplyRequest request) {
        return Result.ok(applicationService.apply(taskId, request == null ? new ApplyRequest(null) : request));
    }

    @GetMapping("/tasks/{taskId}/applications")
    public Result<List<ApplicationVO>> list(@PathVariable Long taskId) {
        return Result.ok(applicationService.listByTask(taskId));
    }

    @PostMapping("/applications/{id}/accept")
    public Result<TaskVO> accept(@PathVariable Long id) {
        return Result.ok(applicationService.accept(id));
    }

    @PostMapping("/applications/{id}/reject")
    public Result<TaskVO> reject(@PathVariable Long id) {
        return Result.ok(applicationService.reject(id));
    }

    @PostMapping("/applications/{id}/withdraw")
    public Result<TaskVO> withdraw(@PathVariable Long id) {
        return Result.ok(applicationService.withdraw(id));
    }
}
