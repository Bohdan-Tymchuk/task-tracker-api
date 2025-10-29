package com.example.tasktracker.service;

import com.example.tasktracker.dto.TaskRequest;
import com.example.tasktracker.dto.TaskResponse;
import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.TaskStatus;
import com.example.tasktracker.repository.TaskRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
  private final TaskRepository repository;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public TaskService(TaskRepository repository) {
    this.repository = repository;
  }

  public TaskResponse createTask(TaskRequest request) {
    Task task = Task.builder()
        .title(request.title())
        .description(request.description())
        .dueDate(request.dueDate())
        .status(TaskStatus.PENDING)
        .build();
    Task saved = repository.save(task);
    return toResponse(saved);
  }

  public List<TaskResponse> getTasks(TaskStatus status, LocalDate dueBefore) {
    List<Task> tasks;
    if (status != null) {
      tasks = repository.findByStatus(status);
    } else if (dueBefore != null) {
      tasks = repository.findDueOnOrBefore(dueBefore);
    } else {
      tasks = repository.findAll();
    }
    return tasks.stream().map(this::toResponse).toList();
  }

  public TaskResponse getTask(UUID id) {
    Task task = repository.findById(id)
        .orElseThrow(() -> new TaskServiceException("Task %s not found".formatted(id)));
    return toResponse(task);
  }

  public TaskResponse updateStatus(UUID id, TaskStatus status) {
    Task existing = repository.findById(id)
        .orElseThrow(() -> new TaskServiceException("Task %s not found".formatted(id)));
    Task updated = existing.toBuilder()
        .status(status)
        .build();
    repository.save(updated);
    return toResponse(updated);
  }

  public void deleteTask(UUID id) {
    repository.deleteById(id);
  }

  private TaskResponse toResponse(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getDueDate(),
        task.getStatus());
  }
}
