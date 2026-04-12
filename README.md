# Course Enrolment API — Automated Test Suite

## Tech Stack
| Tool | Purpose |
|------|---------|
| Java 21 | Programming language |
| Playwright 1.49.0 | API HTTP client |
| JUnit 5 | Test runner and lifecycle management |
| Allure Reports | Test reporting |
| Maven 3.9+ | Build tool and dependency management |

> For full details on architecture, test coverage, design decisions
> and known gaps — refer to the accompanying
> **Test Design & Automation Strategy Document**.
---

## Prerequisites
Make sure the following are installed before getting started:
| Tool | Version | Download |
|------|---------|----------|
| Java | 21 | [Microsoft OpenJDK](https://www.microsoft.com/openjdk) |
| Maven | 3.9+ | [Apache Maven](https://maven.apache.org/download.cgi) |
| Git | Latest | [Git](https://git-scm.com/downloads) |
| IntelliJ IDEA | Latest | [JetBrains](https://www.jetbrains.com/idea/download) |

Verify your installations:
```bash
java -version
mvn -version
git --version
```
---

## Getting Started
### 1. Clone the Repository
```bash
git clone https://https://github.com/TestPro-Playwright/Playwright_APITests.git
```

### 2. Install Dependencies
```bash
mvn dependency:resolve
```
This downloads all required dependencies defined in `pom.xml` into
your local Maven cache (`~/.m2/repository/`). This is the equivalent
of `yarn install` in a JavaScript/TypeScript project.

> If you need to force a clean re-download:
> ```bash
> mvn dependency:resolve -U
> ```

### 3. Open the Project in IntelliJ
```
File → Open → select the CourseEnrollmentAPI-Tests folder → OK
```
Wait for IntelliJ to finish indexing. If prompted to reload Maven,
click **Reload All Maven Projects** in the Maven panel on the right.

### 4. Configure Credentials
Open `src/test/resources/config.properties` and replace all placeholder values
with the actual instructor and student credentials provided in the submission email:

```properties
base.url=https://courseenrollmentapimanagementsystem.onrender.com
instructor.username=YOUR_INSTRUCTOR_USERNAME
instructor.password=YOUR_INSTRUCTOR_PASSWORD
student.username=YOUR_STUDENT_USERNAME
student.password=YOUR_STUDENT_PASSWORD
timeout=60000
```
> **Note:** Tests will fail with a clear error message if placeholder
> values are not replaced before running.
---

## Running the Tests
### Run All Tests and Open Report
```bash
mvn clean test && mvn allure:serve
```

### Run a Specific Test Class
```bash
# Instructor tests only
mvn test -Dtest=InstructorCourseTest

# Student tests only
mvn test -Dtest=StudentEnrolmentTest

# Both classes in order
mvn test -Dtest="InstructorCourseTest,StudentEnrolmentTest"
```
---

## Viewing the Allure Report
```bash
# Run tests and open report in one command
mvn clean test && mvn allure:serve
```
> **Important:** Do not open `index.html` directly by double-clicking.
> Always use `mvn allure:serve` — opening the file directly causes the
> report to get stuck on "Loading..." due to browser security restrictions.
---

## Troubleshooting
| Problem | Fix |
|-----------|-----|
| `mvn: command not found` | Revisit Maven installation and PATH setup |
| `401 Unauthorised` on first test | Check credentials in `config.properties` |
| Report shows "Loading..." | Use `mvn allure:serve` instead of opening the HTML file directly |
| Dependencies not resolving | Run `mvn dependency:resolve -U` to force a clean re-download |
