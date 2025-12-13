package com.example.tasktracker.ui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bonigarcia.wdm.WebDriverManager;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.example.tasktracker.model.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TaskTrackerSeleniumTest {
  @LocalServerPort
  private int port;

  private WebDriver driver;
  private WebDriverWait wait;

  @BeforeEach
  void setUp() {
    WebDriver selected = tryCreateChrome();
    if (selected == null) {
      selected = tryCreateFirefox();
    }
    Assumptions.assumeTrue(selected != null, "No local Chrome or Firefox WebDriver available");
    this.driver = selected;
    this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
  }

  @AfterEach
  void tearDown() {
    if (driver != null) {
      driver.quit();
    }
  }

  private String baseUrl() {
    return System.getProperty("selenium.baseUrl", "http://localhost:" + port);
  }

  private void waitForPageReady() {
    wait.until(d -> ((org.openqa.selenium.JavascriptExecutor) d)
        .executeScript("return document.readyState").equals("complete"));
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("h1")));
  }

  private WebDriver tryCreateChrome() {
    try {
      WebDriverManager.chromedriver().setup();
      ChromeOptions options = new ChromeOptions();
      options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
      return new ChromeDriver(options);
    } catch (WebDriverException ex) {
      return null;
    }
  }

  private WebDriver tryCreateFirefox() {
    try {
      WebDriverManager.firefoxdriver().setup();
      FirefoxOptions options = new FirefoxOptions();
      resolveFirefoxBinary().ifPresent(options::setBinary);
      options.addArguments("-headless");
      return new FirefoxDriver(options);
    } catch (WebDriverException | IllegalStateException ex) {
      return null;
    }
  }

  private Optional<String> resolveFirefoxBinary() {
    String[] candidates = {
        "/snap/firefox/current/usr/lib/firefox/firefox",
        "/usr/lib/firefox/firefox",
        "/usr/bin/firefox"
    };
    for (String path : candidates) {
      if (new java.io.File(path).exists()) {
        return Optional.of(path);
      }
    }
    return Optional.empty();
  }

  @Test
  void userCanCreateTaskViaUi() {
    driver.get(baseUrl());
    waitForPageReady();

    driver.findElement(By.id("title")).sendKeys("UI Task");
    driver.findElement(By.id("description")).sendKeys("Created through Selenium");
    driver.findElement(By.id("dueDate")).sendKeys(LocalDate.now().plusDays(1).toString());

    driver.findElement(By.cssSelector("form#createForm button[type='submit']")).click();

    wait.until(ExpectedConditions.textToBePresentInElementLocated(
        By.cssSelector("tbody tr:first-child td:first-child"), "UI Task"));

    List<WebElement> titles = driver.findElements(By.cssSelector("tbody tr td:first-child"));
    assertThat(titles)
        .extracting(WebElement::getText)
        .anySatisfy(text -> assertThat(text).contains("UI Task"));
  }

  @Test
  void userCanUpdateStatusViaUi() {
    driver.get(baseUrl());
    waitForPageReady();

    createTask("Update Status Task", "Move to completed", LocalDate.now().plusDays(1));

    updateRowStatus("Update Status Task", TaskStatus.COMPLETED.name());
    WebElement refreshed = findRowByTitle("Update Status Task");
    assertThat(refreshed.findElement(By.className("status-tag")).getAttribute("data-state"))
        .isEqualTo("COMPLETED");
    assertThat(refreshed.findElement(By.className("status-tag")).getText())
        .containsIgnoringCase("completed");
  }

  @Test
  void userCanFilterByStatus() {
    driver.get(baseUrl());
    waitForPageReady();

    createTask("Pending Task", "Keep pending", LocalDate.now().plusDays(1));
    createTask("Completed Task", "Mark done", LocalDate.now().plusDays(2));

    updateRowStatus("Completed Task", TaskStatus.COMPLETED.name());

    openFilters();
    Select statusFilter = new Select(driver.findElement(By.id("filterStatus")));
    statusFilter.selectByValue("COMPLETED");
    driver.findElement(By.cssSelector("form#filterForm button[type='submit']")).click();

    wait.until(d -> d.findElements(By.cssSelector("tbody tr")).size() == 1);
    WebElement onlyRow = driver.findElement(By.cssSelector("tbody tr"));
    assertThat(onlyRow.findElement(By.cssSelector("td:first-child")).getText())
        .contains("Completed Task");
    assertThat(onlyRow.findElement(By.className("status-tag")).getAttribute("data-state"))
        .isEqualTo("COMPLETED");
  }

  @Test
  void userCanFilterByDueDate() {
    driver.get(baseUrl());
    waitForPageReady();

    createTask("Soon", "Due tomorrow", LocalDate.now().plusDays(1));
    createTask("Later", "Due next week", LocalDate.now().plusDays(7));

    openFilters();
    driver.findElement(By.id("dueBefore")).sendKeys(LocalDate.now().plusDays(2).toString());
    driver.findElement(By.cssSelector("form#filterForm button[type='submit']")).click();

    wait.until(d -> d.findElements(By.cssSelector("tbody tr")).size() == 1);
    WebElement onlyRow = driver.findElement(By.cssSelector("tbody tr"));
    assertThat(onlyRow.findElement(By.cssSelector("td:first-child")).getText()).contains("Soon");
  }

  private void createTask(String title, String description, LocalDate dueDate) {
    driver.findElement(By.id("title")).clear();
    driver.findElement(By.id("title")).sendKeys(title);
    driver.findElement(By.id("description")).clear();
    driver.findElement(By.id("description")).sendKeys(description);
    driver.findElement(By.id("dueDate")).clear();
    driver.findElement(By.id("dueDate")).sendKeys(dueDate.toString());
    driver.findElement(By.cssSelector("form#createForm button[type='submit']")).click();
    wait.until(d -> {
      try {
        return d.findElements(By.cssSelector("tbody tr td:first-child")).stream()
            .anyMatch(cell -> title.equals(cell.getText()));
      } catch (StaleElementReferenceException ex) {
        return false;
      }
    });
  }

  private WebElement findRowByTitle(String title) {
    return wait.until(d -> {
      try {
        return d.findElements(By.cssSelector("tbody tr")).stream()
            .filter(row -> title.equals(row.findElement(By.cssSelector("td:first-child")).getText()))
            .findFirst()
            .orElse(null);
      } catch (StaleElementReferenceException ex) {
        return null;
      }
    });
  }

  private void updateRowStatus(String title, String statusValue) {
    wait.until(d -> !d.findElements(By.cssSelector("tbody tr")).isEmpty());
    WebElement row = findRowByTitle(title);
    new Select(row.findElement(By.tagName("select"))).selectByValue(statusValue);
    wait.until(d -> {
      try {
        WebElement refreshed = findRowByTitle(title);
        return refreshed != null && statusValue.equals(
            refreshed.findElement(By.className("status-tag")).getAttribute("data-state"));
      } catch (StaleElementReferenceException ex) {
        return false;
      }
    });
  }

  private void openFilters() {
    List<WebElement> toggles = driver.findElements(By.cssSelector("details.filters-toggle summary"));
    if (!toggles.isEmpty()) {
      toggles.get(0).click();
    }
  }
}
