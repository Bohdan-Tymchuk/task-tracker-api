package com.example.tasktracker.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable representation of a task tracked by the application.
 */
public final class Task {
  private final UUID id;
  private final String title;
  private final String description;
  private final LocalDate dueDate;
  private final TaskStatus status;

  private Task(Builder builder) {
    this.id = builder.id;
    this.title = builder.title;
    this.description = builder.description;
    this.dueDate = builder.dueDate;
    this.status = builder.status;
  }

  public UUID getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Task task)) {
      return false;
    }
    return Objects.equals(id, task.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private UUID id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private TaskStatus status = TaskStatus.PENDING;

    public Builder() {
      // Default constructor
    }

    public Builder(Task task) {
      this.id = task.id;
      this.title = task.title;
      this.description = task.description;
      this.dueDate = task.dueDate;
      this.status = task.status;
    }

    public Builder id(UUID idValue) {
      this.id = idValue;
      return this;
    }

    public Builder title(String titleValue) {
      this.title = titleValue;
      return this;
    }

    public Builder description(String descriptionValue) {
      this.description = descriptionValue;
      return this;
    }

    public Builder dueDate(LocalDate dueDateValue) {
      this.dueDate = dueDateValue;
      return this;
    }

    public Builder status(TaskStatus statusValue) {
      this.status = statusValue;
      return this;
    }

    public Task build() {
      if (id == null) {
        id = UUID.randomUUID();
      }
      return new Task(this);
    }
  }
}
