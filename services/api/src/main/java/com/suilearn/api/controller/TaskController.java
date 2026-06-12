package com.suilearn.api.controller;

import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.task.application.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{taskId}")
    TaskStatus getTaskStatus(@PathVariable String taskId) {
        return taskService.getTaskStatus(taskId);
    }
}
