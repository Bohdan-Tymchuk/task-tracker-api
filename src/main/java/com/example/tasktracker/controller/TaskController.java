package com.example.tasktracker.controller;

import com.example.tasktracker.dto.TaskRequest;
import com.example.tasktracker.dto.TaskResponse;
import com.example.tasktracker.dto.UpdateStatusRequest;
import com.example.tasktracker.model.TaskStatus;
import com.example.tasktracker.service.TaskService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
  private final TaskService taskService;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TaskResponse createTask(@Valid @RequestBody TaskRequest request) {
    return taskService.createTask(request);
  }

  @GetMapping
  public List<TaskResponse> getTasks(
      @RequestParam(value = "status", required = false) TaskStatus status,
      @RequestParam(value = "dueBefore", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore) {
    return taskService.getTasks(status, dueBefore);
  }

  @GetMapping("/{id}")
  public TaskResponse getTask(@PathVariable("id") UUID id) {
    return taskService.getTask(id);
  }

  @PostMapping("/{id}/status")
  public TaskResponse updateStatus(
      @PathVariable("id") UUID id, @Valid @RequestBody UpdateStatusRequest request) {
    return taskService.updateStatus(id, request.status());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTask(@PathVariable("id") UUID id) {
    taskService.deleteTask(id);
    return ResponseEntity.noContent().build();
  }
}
