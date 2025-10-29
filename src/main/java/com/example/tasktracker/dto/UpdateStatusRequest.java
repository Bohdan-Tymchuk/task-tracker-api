package com.example.tasktracker.dto;

import com.example.tasktracker.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
    @NotNull(message = "Status is required")
    TaskStatus status) {
}
