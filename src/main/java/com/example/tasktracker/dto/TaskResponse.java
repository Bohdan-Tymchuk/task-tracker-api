package com.example.tasktracker.dto;

import com.example.tasktracker.model.TaskStatus;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    LocalDate dueDate,
    TaskStatus status) {
}
