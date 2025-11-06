# Task Tracker API

## Overview
Task Tracker API is a Spring Boot service that lets teams capture work items, update their state, and query upcoming work. The codebase is intentionally small so the focus stays on continuous integration: automated builds, static analysis, and coverage enforcement.

## Technologies Used
- Java 21
- Spring Boot 3 (Web, Validation)
- Maven build tooling
- JUnit 5, AssertJ, Spring MockMvc
- JaCoCo, Checkstyle, SpotBugs for quality gates
- Azure Pipelines for CI/CD orchestration

## Local Development Setup
1. Install Java 21 (matching the CI environment).
2. Ensure Apache Maven 3.9+ is available on your `$PATH`.
3. Clone the repository and switch to the `development` branch.
4. Build and test: `mvn -B verify`  
   The command compiles the app, runs all unit tests, executes Checkstyle and SpotBugs, and enforces ≥ 80 % line coverage via JaCoCo.
5. Run locally (optional): `mvn spring-boot:run` and hit `http://localhost:8080/api/tasks`.

### Quick Usage Example
Create a task:
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
        "title": "Draft documentation",
        "description": "Outline the CI pipeline steps",
        "dueDate": "2025-11-15"
      }'
```

Fetch only pending tasks:
```bash
curl "http://localhost:8080/api/tasks?status=PENDING"
```

Mark a task complete (replace `{id}` with the response UUID):
```bash
curl -X POST http://localhost:8080/api/tasks/{id}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
```

## Application Features
- **Create tasks** with title, description, and due date; tasks start in `PENDING`.
- **Query tasks** using optional filters for status or due date to highlight upcoming work.
- **Update task status** to reflect progress (`PENDING`, `IN_PROGRESS`, `COMPLETED`).
- **Delete tasks** once completed or no longer needed.

## CI Pipeline Implementation
- Azure Pipelines YAML (`azure-pipelines.yml`) triggers on pushes and PRs targeting `main` and `development`.
- Pipeline stages:
  - Checkout and cache Maven dependencies.
  - Provision JDK 21 via `UseJavaVersion@1`.
  - `mvn -B verify` runs compilation, tests, Checkstyle, SpotBugs, and JaCoCo coverage checks.
  - Publish JUnit test results and the JaCoCo coverage report.
  - Upload SpotBugs XML as a build artifact for inspection.
- Build fails if compilation, static analysis, or coverage checks trip—providing a hard gate before merging.

## Branch Policies and Protection
- Default branch is `main`; active development occurs on `development`.
- GitHub branch rules:
  - Require pull requests into both branches.
  - Enforce the Azure Pipeline status check and require branches to be up to date.
  - Block force pushes and direct merges to protected branches.
- Workflows: feature branches cut from `development`, PRs reviewed and merged into `development`, then promotion PRs from `development` to `main`.

## Testing Strategy
- Unit tests exercise service logic (task creation, filtering, state updates) using the in-memory repository.
- Web layer tests verify JSON contracts and request validation through Spring MockMvc.
- JaCoCo limit set to 0.80 line coverage; failing tests or insufficient coverage will fail both local and CI builds.
- Static analyzers (Checkstyle, SpotBugs) run on every build to catch style regressions and potential defects early.

## Troubleshooting Guide
- **Maven cannot download dependencies locally**: set a writable local repo (e.g., `mvn -Dmaven.repo.local=./.m2 verify`) if your environment restricts `~/.m2`.
- **CI build fails on Checkstyle/SpotBugs**: run `mvn -B verify` to reproduce; fix highlighted issues before pushing.
- **Coverage gate fails**: review `target/site/jacoco/index.html` locally to spot untested code paths and add unit tests.
- **Pipeline status check missing in PR**: verify the Azure Pipeline is set as a required status check under GitHub branch protection rules.
