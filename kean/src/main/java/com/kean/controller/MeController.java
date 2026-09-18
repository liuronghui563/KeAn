package com.kean.controller;

import com.kean.common.PageResult;
import com.kean.common.Result;
import com.kean.service.TaskService;
import com.kean.vo.TaskVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final TaskService taskService;

    public MeController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/published")
    public Result<PageResult<TaskVO>> published(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size
    ) {
        return Result.ok(taskService.listMyPublished(page, size));
    }

    @GetMapping("/applied")
    public Result<PageResult<TaskVO>> applied(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long size
    ) {
        return Result.ok(taskService.listMyApplied(page, size));
    }
}
