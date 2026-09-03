# UI Test Plan

## Test configuration

- Build command: `./gradlew shadowJar`
- Launch command: `java -jar bola.jar --cli` (the default launch now opens JavaFX).
- Java version: 25
- Isolation: Run each test case in a new temporary directory containing a copy of `build/libs/bola.jar`.
- Default data precondition: No `data/bola.txt` file.
- Comparison: Compare each command response exactly after converting CRLF to LF and removing ANSI colour codes. Ignore terminal input echo and the standard inter-response divider unless an expected block includes it. The outer divider printed by `bye` is part of that command's response.

## TC-001: Exit the application

**Aim:** Verify the simplified greeting and that `bye` still ends the console session immediately.

**Greeting check:** Before sending `bye`, verify these two consecutive greeting lines
(five leading spaces on each line):

```text
     Bola: Eh hello! I'm Bola.
     Got anything to settle today?
```

The console must not show the GUI-only closing countdown.

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

## TC-003: Find tasks and view the upcoming schedule

**Aim:** Verify that successful `upcoming` and `find` results use Bola's Singaporean voice.

**Data precondition:** Create `data/bola.txt` with these records:

```text
T | 0 | read book
D | 0 | return book | 2026-09-10
```

**Inputs:**

1. `upcoming 7`
2. `find book`
3. `bye`

**Expected outputs:**

1. For `upcoming 7`:

   ```text
        Bola: Next 7 days got these tasks:
            2. [D][ ] return book (By: Sep 10 2026)
   ```

2. For `find book`:

   ```text
        Bola: Can, found these matching tasks:
            1. [T][ ] read book
            2. [D][ ] return book (By: Sep 10 2026)
   ```

3. For `bye`:

   ```text
        Bola: All settled? Steady lah. See you again! 👋
   ================================================================
   ```
