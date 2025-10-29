package com.example.tasktracker.repository;

import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
  Task save(Task task);

  Optional<Task> findById(UUID id);

  List<Task> findAll();

  List<Task> findByStatus(TaskStatus status);

  List<Task> findDueOnOrBefore(LocalDate date);

  void deleteById(UUID id);
}
