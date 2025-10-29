package com.example.tasktracker.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.tasktracker.dto.TaskRequest;
import com.example.tasktracker.dto.TaskResponse;
import com.example.tasktracker.dto.UpdateStatusRequest;
import com.example.tasktracker.model.TaskStatus;
import com.example.tasktracker.service.TaskService;
import com.example.tasktracker.service.TaskServiceException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Test
    void createTaskReturnsCreatedTask() throws Exception {
        UUID id = UUID.randomUUID();
        TaskResponse response = new TaskResponse(id, "Write tests", "Add coverage", LocalDate.now(),
                TaskStatus.PENDING);
        when(taskService.createTask(any(TaskRequest.class))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Write tests",
                                  "description": "Add coverage",
                                  "dueDate": "%s"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id")
                        .value(id.toString()));
    }

    @Test
    void updateStatusDelegatesToService() throws Exception {
        UUID id = UUID.randomUUID();
        TaskResponse response = new TaskResponse(id, "Task", "Desc", LocalDate.now(), TaskStatus.COMPLETED);
        when(taskService.updateStatus(eq(id), eq(TaskStatus.COMPLETED))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tasks/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status")
                        .value("COMPLETED"));

        verify(taskService).updateStatus(id, TaskStatus.COMPLETED);
    }

    @Test
    void getTasksSupportsQueryParameters() throws Exception {
        UUID id = UUID.randomUUID();
        TaskResponse response = new TaskResponse(id, "Task", "Desc", LocalDate.now(), TaskStatus.PENDING);
        when(taskService.getTasks(TaskStatus.PENDING, null)).thenReturn(List.of(response));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tasks")
                        .param("status", "PENDING"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].id")
                        .value(id.toString()));
    }

    @Test
    void getTaskNotFoundReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTask(id)).thenThrow(new TaskServiceException("Task %s not found".formatted(id)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tasks/{id}", id))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value(containsString("not found")));
    }

    @Test
    void createTaskWithPastDueDateReturnsBadRequest() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid",
                                  "description": "Past due",
                                  "dueDate": "%s"
                                }
                                """.formatted(yesterday)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value(containsString("Due date")));
}
}
