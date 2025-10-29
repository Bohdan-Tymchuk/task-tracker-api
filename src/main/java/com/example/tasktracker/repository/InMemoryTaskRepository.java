package com.example.tasktracker.repository;

import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.TaskStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTaskRepository implements TaskRepository {
  private final Map<UUID, Task> store = new ConcurrentHashMap<>();

  @Override
  public Task save(Task task) {
    store.put(task.getId(), task);
    return task;
  }

  @Override
  public Optional<Task> findById(UUID id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public List<Task> findAll() {
    return Collections.unmodifiableList(new ArrayList<>(store.values()));
  }

  @Override
  public List<Task> findByStatus(TaskStatus status) {
    return store.values().stream()
        .filter(task -> task.getStatus() == status)
        .toList();
  }

  @Override
  public List<Task> findDueOnOrBefore(LocalDate date) {
    return store.values().stream()
        .filter(task -> !task.getDueDate().isAfter(date))
        .toList();
  }

  @Override
  public void deleteById(UUID id) {
    store.remove(id);
  }
}
