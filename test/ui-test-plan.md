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
   ================================================================
   ```

## TC-003: Find tasks and view the upcoming schedule

**Aim:** Verify that successful `upcoming` and `find` results use Bola's Singaporean voice.

**Data precondition:** Create `data/bola.txt` with these records:

```text
T | 0 | read book
D | 0 | return book | 2026-09-15
```

**Inputs:**

1. `upcoming 7`
2. `find book`
3. `bye`

**Expected outputs:**

1. For `upcoming 7`:

   ```text
        Bola: Next 7 days got these tasks:
            2. [D][ ] return book (By: Sep 15 2026)
   ```

2. For `find book`:

   ```text
        Bola: Can, found these matching tasks:
            1. [T][ ] read book
            2. [D][ ] return book (By: Sep 15 2026)
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

## TC-005: Perform explicit mass operations

**Aim:** Verify mixed selectors, deduplication, original numbering, confirmation,
and atomic cancellation for explicit mass operations.

**Inputs:**

1. `todo task 1`
2. `todo task 2`
3. `todo task 3`
4. `todo task 4`
5. `mark 1-3 2-4 2`
6. `unmark 4, 2`
7. `delete 2, 4`
8. `list`
9. `no`
10. `delete 2 4`
11. `yes`
12. `list`
13. `bye`

**Expected outputs:**

1. For `todo task 1`:

   ```text
        Bola: Can! I've added this task:
            [T][ ] task 1
        Now got 1 task in your list.
   ```

2. For `todo task 2`:

   ```text
        Bola: Can! I've added this task:
            [T][ ] task 2
        Now got 2 tasks in your list.
   ```

3. For `todo task 3`:

   ```text
        Bola: Can! I've added this task:
            [T][ ] task 3
        Now got 3 tasks in your list.
   ```

4. For `todo task 4`:

   ```text
        Bola: Can! I've added this task:
            [T][ ] task 4
        Now got 4 tasks in your list.
   ```

5. For `mark 1-3 2-4 2`:

   ```text
        Bola: Nice, 4 tasks settled liao! ✅
            1. [T][X] task 1
            2. [T][X] task 2
            3. [T][X] task 3
            4. [T][X] task 4
        Now got 4 tasks in your list.
   ```

6. For `unmark 4, 2`:

   ```text
        Bola: Okay, these 2 tasks not settled yet.
            2. [T][ ] task 2
            4. [T][ ] task 4
        Now got 4 tasks in your list.
   ```

7. For `delete 2, 4`:

   ```text
        Bola: U sure u want to delete these 2 tasks? (Yes/No)
   ```

8. For `list` while confirmation is pending:

   ```text
        Bola: Aiyo, please answer Yes or No, can?
   ```

9. For `no`:

   ```text
        Bola: Okay, cancelled. No tasks changed.
   ```

10. For `delete 2 4`:

    ```text
         Bola: U sure u want to delete these 2 tasks? (Yes/No)
    ```

11. For `yes`:

    ```text
         Bola: Okay, removed these 2 tasks already:
             2. [T][ ] task 2
             4. [T][ ] task 4
         Now got 2 tasks in your list.
    ```

12. For `list`:

    ```text
         Bola: Your tasks all here:
             1. [T][X] task 1
             2. [T][X] task 3
    ```

13. For `bye`:

    ```text
         Bola: All settled? Steady lah. See you again! 👋
    ================================================================
    ```

## TC-006: Validate all-selection and help behavior

**Aim:** Verify confirmation for `all`, empty-list validation, and the general help output.

**Inputs:**

1. `todo first`
2. `todo second`
3. `mark all`
4. `YES`
5. `delete all`
6. `yes`
7. `unmark all`
8. `help`
9. `bye`

**Expected outputs:**

1. For `todo first`:

   ```text
        Bola: Can! I've added this task:
            [T][ ] first
        Now got 1 task in your list.
   ```

2. For `todo second`:

   ```text
        Bola: Can! I've added this task:
            [T][ ] second
        Now got 2 tasks in your list.
   ```

3. For `mark all`:

   ```text
        Bola: U sure u want to mark all 2 tasks? (Yes/No)
   ```

4. For `YES`:

   ```text
        Bola: Nice, 2 tasks settled liao! ✅
            1. [T][X] first
            2. [T][X] second
        Now got 2 tasks in your list.
   ```

5. For `delete all`:

   ```text
        Bola: U sure u want to delete all 2 tasks? (Yes/No)
   ```

6. For `yes`:

   ```text
        Bola: Okay, removed these 2 tasks already:
            1. [T][X] first
            2. [T][X] second
        Bo lah! No more tasks in your list. 🎉
   ```

7. For `unmark all`:

   ```text
        Bola: Aiyo, there are no tasks to unmark leh.
   ```

8. For `help`:

   ```text
        Bola: Can! Here are the commands:
            todo <description>
            deadline <description> /by <date>
            event <description> /from <date> /to <date>
            list
            find <keyword>
            upcoming <days>
            mark <selection>
            unmark <selection>
            delete <selection>
            help
            bye
        Selection can use task numbers, inclusive ranges, or all.
        Example: delete 1, 3-5 8
        Dates use yyyy-MM-dd, or d/M/yyyy HHmm when including a time.
   ```

9. For `bye`:

   ```text
        Bola: All settled? Steady lah. See you again! 👋
   ================================================================
   ```

## TC-007: Reject an invalid mass selection atomically

**Aim:** Verify out-of-range, reversed, malformed, and mixed-`all` selections fail
without changing any task.

**Data precondition:** Create `data/bola.txt` with these records:

```text
T | 0 | first
T | 0 | second
T | 0 | third
```

**Inputs:**

1. `mark 1-4`
2. `delete 3-1`
3. `unmark 1,,2`
4. `delete all 2`
5. `list`
6. `bye`

**Expected outputs:**

1. For `mark 1-4`:

   ```text
        Bola: Aiyo, task number 4 doesn't exist leh.
   ```

2. For `delete 3-1`:

   ```text
        Bola: Aiyo, range 3-1 cannot leh; the start number must not be greater than the end number.
   ```

3. For `unmark 1,,2`:

   ```text
        Bola: Aiyo, please give me valid task numbers or ranges to unmark, can?
   ```

4. For `delete all 2`:

   ```text
        Bola: Aiyo, all must be used by itself for delete, can?
   ```

5. For `list`:

   ```text
        Bola: Your tasks all here:
            1. [T][ ] first
            2. [T][ ] second
            3. [T][ ] third
   ```

6. For `bye`:

   ```text
        Bola: All settled? Steady lah. See you again! 👋
   ================================================================
   ```
