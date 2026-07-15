package com.suilearn.api.controller;

import com.suilearn.api.generation.application.KnowledgePointQuestionGenerationService;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.task.application.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v2/tasks")
public class TaskController {
    private final TaskService taskService;
    private final KnowledgePointQuestionGenerationService questionGenerationService;

    public TaskController(TaskService taskService) {
        this(taskService, null);
    }

    @Autowired
    public TaskController(TaskService taskService, KnowledgePointQuestionGenerationService questionGenerationService) {
        this.taskService = taskService;
        this.questionGenerationService = questionGenerationService;
    }

    @GetMapping("/{taskId}")
    TaskStatus getTaskStatus(@PathVariable String taskId) {
        return taskService.getTaskStatus(taskId);
    }

    @GetMapping("/{taskId}/question-drafts")
    List<GeneratedQuestionDraft> questionDrafts(@PathVariable String taskId) {
        if (questionGenerationService == null) {
            throw new IllegalStateException("Knowledge point interview question generation is unavailable");
        }
        return questionGenerationService.listDrafts(taskId);
    }
}
