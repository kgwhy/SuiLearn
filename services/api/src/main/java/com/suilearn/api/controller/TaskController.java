package com.suilearn.api.controller;

import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.service.SuiLearnV2Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/tasks")
public class TaskController {
    private final SuiLearnV2Service service;

    public TaskController(SuiLearnV2Service service) {
        this.service = service;
    }

    @GetMapping("/{taskId}")
    TaskStatus getTaskStatus(@PathVariable String taskId) {
        return service.getTaskStatus(taskId);
    }
}
