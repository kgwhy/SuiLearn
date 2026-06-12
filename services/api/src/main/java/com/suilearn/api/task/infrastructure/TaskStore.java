package com.suilearn.api.task.infrastructure;

import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.persistence.SuiLearnV2Store;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class TaskStore {
    private final SuiLearnV2Store store;

    public TaskStore(SuiLearnV2Store store) {
        this.store = store;
    }

    public Optional<TaskStatus> find(String taskId) {
        return store.findTask(taskId);
    }

    public List<TaskStatus> list() {
        return store.listTasks();
    }

    public TaskStatus save(TaskStatus task) {
        return store.saveTask(task);
    }
}
