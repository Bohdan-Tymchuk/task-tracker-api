package com.example.tasktracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tasktracker.model.Task;
import com.example.tasktracker.model.TaskStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryTaskRepositoryTest {
    private InMemoryTaskRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTaskRepository();
    }

    @Test
    void saveAndRetrieveTask() {
        Task task = Task.builder()
                .title("Document API")
                .description("Write README updates")
                .dueDate(LocalDate.now())
                .status(TaskStatus.PENDING)
                .build();

        repository.save(task);

        assertThat(repository.findById(task.getId())).contains(task);
    }

    @Test
    void findByStatusFiltersTasks() {
        Task pending = Task.builder()
                .title("Pending")
                .description("Not started")
                .dueDate(LocalDate.now())
                .status(TaskStatus.PENDING)
                .build();
        Task completed = pending.toBuilder()
                .id(null)
                .status(TaskStatus.COMPLETED)
                .build();
        repository.save(pending);
        repository.save(completed);

        assertThat(repository.findByStatus(TaskStatus.COMPLETED))
                .extracting(Task::getStatus)
                .containsExactly(TaskStatus.COMPLETED);
    }

    @Test
    void findDueOnOrBeforeFiltersByDate() {
        Task dueTomorrow = Task.builder()
                .title("Soon")
                .description("Due tomorrow")
                .dueDate(LocalDate.now().plusDays(1))
                .build();
        Task dueLater = Task.builder()
                .title("Later")
                .description("Due later")
                .dueDate(LocalDate.now().plusDays(5))
                .build();
        repository.save(dueTomorrow);
        repository.save(dueLater);

        assertThat(repository.findDueOnOrBefore(LocalDate.now().plusDays(2)))
                .hasSize(1)
                .first()
                .extracting(Task::getTitle)
                .isEqualTo("Soon");
    }

    @Test
    void deleteByIdRemovesTask() {
        Task task = Task.builder()
                .title("Delete me")
                .description("Temporary")
                .dueDate(LocalDate.now())
                .build();
        repository.save(task);

        repository.deleteById(task.getId());

        assertThat(repository.findById(task.getId())).isEmpty();
    }
}
