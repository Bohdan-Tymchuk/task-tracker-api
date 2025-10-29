package com.example.tasktracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tasktracker.dto.TaskRequest;
import com.example.tasktracker.model.TaskStatus;
import com.example.tasktracker.repository.InMemoryTaskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskServiceTest {
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(new InMemoryTaskRepository());
    }

    @Test
    void createTaskStoresNewTask() {
        TaskRequest request = new TaskRequest("Write report", "Prepare sprint report",
                LocalDate.now().plusDays(1));

        var response = taskService.createTask(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.title()).isEqualTo("Write report");
        assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
        assertThat(taskService.getTask(response.id()).id()).isEqualTo(response.id());
    }

    @Test
    void getTasksFiltersByStatus() {
        TaskRequest first = new TaskRequest("Task 1", "Pending task", LocalDate.now());
        TaskRequest second = new TaskRequest("Task 2", "In progress task", LocalDate.now());
        var firstResponse = taskService.createTask(first);
        var secondResponse = taskService.createTask(second);
        taskService.updateStatus(secondResponse.id(), TaskStatus.IN_PROGRESS);

        List<?> pendingTasks = taskService.getTasks(TaskStatus.PENDING, null);
        List<?> activeTasks = taskService.getTasks(TaskStatus.IN_PROGRESS, null);

        assertThat(pendingTasks).extracting("id").contains(firstResponse.id());
        assertThat(activeTasks).extracting("id").contains(secondResponse.id());
    }

    @Test
    void getTasksFiltersByDueDate() {
        TaskRequest first = new TaskRequest("Task 1", "Soon", LocalDate.now().plusDays(1));
        TaskRequest second = new TaskRequest("Task 2", "Later", LocalDate.now().plusDays(10));
        taskService.createTask(first);
        taskService.createTask(second);

        List<?> dueSoon = taskService.getTasks(null, LocalDate.now().plusDays(2));

        assertThat(dueSoon).hasSize(1);
    }

    @Test
    void updateStatusThrowsWhenTaskMissing() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> taskService.updateStatus(randomId, TaskStatus.COMPLETED))
                .isInstanceOf(TaskServiceException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteTaskRemovesExistingTask() {
        TaskRequest request = new TaskRequest("Clean up", "Archive finished tasks",
                LocalDate.now().plusDays(2));
        var response = taskService.createTask(request);

        taskService.deleteTask(response.id());

        assertThatThrownBy(() -> taskService.getTask(response.id()))
                .isInstanceOf(TaskServiceException.class)
                .hasMessageContaining("not found");
    }
}
