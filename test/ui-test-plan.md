# UI Test Plan

## Test configuration

- Build command: `./gradlew shadowJar`
- Launch command: `java -jar bola.jar --cli` (the default launch now opens JavaFX).
- Java version: 25
- Avatar alignment is covered by `test/gui-test-plan.md` and `DialogBoxTest` using the actual PNGs.
  Window resizing and the responsive chat background are covered by `test/gui-test-plan.md`
  and `MainWindowTest`.
  For changes limited to GUI layout, run TC-001 as the console launch/exit smoke test.
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

## TC-004: Preserve grouped task confirmation lines

**Aim:** Verify that varargs-based output preserves line order, spacing, task status,
and task counts for adding, marking, unmarking, and deleting a task.

**Inputs:**

1. `todo buy kopi`
2. `mark 1`
3. `unmark 1`
4. `delete 1`
5. `bye`

**Expected outputs:**

1. For `todo buy kopi`:

   ```text
        Bola: Can! I've added this task:
            [T][ ] buy kopi
        Now got 1 task in your list.
   ```

2. For `mark 1`:

   ```text
        Bola: Nice, one task settled liao! ✅
            [T][X] buy kopi
   ```

3. For `unmark 1`:

   ```text
        Bola: Okay, this one not settled yet.
            [T][ ] buy kopi
   ```

4. For `delete 1`:

   ```text
        Bola: Okay, removed already:
            [T][ ] buy kopi
        Bo lah! No more tasks in your list. 🎉
   ```

5. For `bye`:

   ```text
        Bola: All settled? Steady lah. See you again! 👋
   ================================================================
   ```
