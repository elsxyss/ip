# UI Test Plan

## Test configuration

- Build command: `./gradlew shadowJar`
- Launch command: `java -jar bola.jar`
- Java version: 25
- Isolation: Run each test case in a new temporary directory containing a copy of `build/libs/bola.jar`.
- Default data precondition: No `data/bola.txt` file.
- Comparison: Compare each command response exactly after converting CRLF to LF and removing ANSI colour codes. Ignore terminal input echo and the standard inter-response divider unless an expected block includes it. The outer divider printed by `bye` is part of that command's response.

## TC-001: Exit the application

**Aim:** Verify that `bye` ends the session with Bola's farewell.

**Inputs:**

1. `bye`

**Expected outputs:**

1. For `bye`:

   ```text
        Bola: All settled? Steady lah. See you again! 👋
   ================================================================
   ```

## TC-002: Verify an empty task list

**Aim:** Verify an empty task list.

**Inputs:**

1. `list`
2. `bye`

**Expected outputs:**

1. For `list`:

   ```text
   Bola: Bo lah! Your task list is empty. 😌
   ```

2. For `bye`:

   ```text
   Bola: All settled? Steady lah. See you again! 👋
   ================================================
   ```
