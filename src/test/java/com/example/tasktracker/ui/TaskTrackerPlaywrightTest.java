package com.example.tasktracker.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.tasktracker.model.TaskStatus;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaskTrackerPlaywrightTest {

  @LocalServerPort
  private int port;

  private Playwright playwright;
  private Browser browser;
  private Page page;
  private final HttpClient client = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    clearAllTasks();
    playwright = Playwright.create();
    browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    page = browser.newPage();
  }

  @AfterEach
  void tearDown() {
    if (page != null) {
      page.close();
    }
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  @Test
  void userCanCreateTaskViaUi() {
    page.navigate(baseUrl());
    page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Task Tracker"))
        .waitFor();

    String title = "UI Task";
    String description = "Created via Playwright";
    String dueDate = LocalDate.now().plusDays(1).toString();

    page.fill("#title", title);
    page.fill("#description", description);
    page.fill("#dueDate", dueDate);
    page.waitForResponse(
        resp -> resp.url().endsWith("/api/tasks") && "POST".equals(resp.request().method()),
        () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create Task"))
            .click());
    page.waitForSelector("tbody tr");
    assertThat(page.locator("tbody tr td:first-child").allTextContents())
        .anySatisfy(text -> assertThat(text).contains(title));
  }

  @Test
  void userCanFilterByStatus() throws IOException, InterruptedException {
    createTaskApi("Pending Task", "Keep pending", LocalDate.now().plusDays(1));
    String doneId = createTaskApi("Completed Task", "Mark done", LocalDate.now().plusDays(2));
    updateStatusApi(doneId, "COMPLETED");

    page.navigate(baseUrl());
    waitForRowsAtLeast(2);

    // Open filters toggle before interacting with selects
    page.locator("details.filters-toggle summary").click();
    page.selectOption("#filterStatus", "COMPLETED");
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply")).click();
    page.waitForSelector("tbody tr");

    assertThat(page.locator("tbody tr").count()).isEqualTo(1);
    assertThat(page.locator("tbody tr td:first-child").textContent()).contains("Completed Task");
    assertThat(page.locator("tbody tr .status-tag").getAttribute("data-state"))
        .isEqualTo("COMPLETED");
  }

  @Test
  void userCanUpdateStatusViaUi() throws IOException, InterruptedException {
    createTaskApi("Status Task", "Move to done", LocalDate.now().plusDays(3));
    page.navigate(baseUrl());
    waitForRowsAtLeast(1);
    page.waitForSelector("tbody tr:has-text('Status Task')");

    page.locator("tbody tr:has-text('Status Task') select")
        .selectOption(TaskStatus.COMPLETED.name());
    page.waitForSelector("tbody tr:has-text('Status Task') .status-tag[data-state='COMPLETED']");
    assertThat(page.locator("tbody tr:has-text('Status Task') .status-tag").textContent())
        .containsIgnoringCase("completed");
  }

  @Test
  void userCanFilterByDueDate() throws IOException, InterruptedException {
    createTaskApi("Soon", "Due tomorrow", LocalDate.now().plusDays(1));
    createTaskApi("Later", "Due next week", LocalDate.now().plusDays(7));

    page.navigate(baseUrl());
    waitForRowsAtLeast(2);
    page.locator("details.filters-toggle summary").click();
    String cutoff = LocalDate.now().plusDays(2).toString();
    page.fill("#dueBefore", cutoff);
    page.waitForResponse(
        resp -> resp.url().contains("/api/tasks") && resp.url().contains("dueBefore"),
        () -> page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply"))
            .click());

    page.waitForFunction(
        "expected => document.querySelectorAll('tbody tr').length === expected", 1);
    assertThat(page.locator("tbody tr").count()).isEqualTo(1);
    assertThat(page.locator("tbody tr td:first-child").textContent()).contains("Soon");
  }

  private String baseUrl() {
    return System.getProperty("ui.baseUrl", "http://localhost:" + port);
  }

  private String createTaskApi(String title, String description, LocalDate dueDate)
      throws IOException, InterruptedException {
    Map<String, Object> payload = new HashMap<>();
    payload.put("title", title);
    payload.put("description", description);
    payload.put("dueDate", dueDate.toString());
    String body = mapper.writeValueAsString(payload);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl() + "/api/tasks"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 201 && response.statusCode() != 200) {
      throw new IllegalStateException("Failed to create task via API, status: "
          + response.statusCode() + ", body: " + response.body());
    }
    JsonNode root = mapper.readTree(response.body());
    return root.get("id").asText();
  }

  private void updateStatusApi(String id, String status) throws IOException, InterruptedException {
    Map<String, Object> payload = new HashMap<>();
    payload.put("status", status);
    String body = mapper.writeValueAsString(payload);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl() + "/api/tasks/" + id + "/status"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    client.send(request, HttpResponse.BodyHandlers.discarding());
  }

  private void waitForRowsAtLeast(int count) {
    page.waitForFunction(
        "expected => document.querySelectorAll('tbody tr').length >= expected",
        count);
  }

  private void clearAllTasks() {
    try {
      HttpRequest listReq = HttpRequest.newBuilder()
          .uri(URI.create(baseUrl() + "/api/tasks"))
          .GET()
          .build();
      HttpResponse<String> response = client.send(listReq, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return;
      }
      JsonNode arr = mapper.readTree(response.body());
      for (JsonNode node : arr) {
        String id = node.get("id").asText();
        HttpRequest del = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl() + "/api/tasks/" + id))
            .DELETE()
            .build();
        client.send(del, HttpResponse.BodyHandlers.discarding());
      }
    } catch (Exception ignored) {
      // best-effort cleanup
    }
  }
}
