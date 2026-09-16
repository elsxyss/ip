# Bola User Guide

![Bola's Kopitiam task-management chatbot](Ui.png)

Bola is a friendly task-management chatbot with a Singapore kopitiam personality. Use its
graphical interface to add, find, update, and save to-dos, deadlines, and events. Bola stores
changes automatically in `data/bola.txt` relative to the folder from which it is launched.

## Quick start

1. Install Java 25.
2. Download `bola.jar` from the latest GitHub release.
3. Put the JAR in the folder where you want Bola to store its data.
4. Run `java -jar bola.jar` to open the graphical interface.
5. Type a command and press Enter or click **Settle**.

Run `java -jar bola.jar --cli` instead to use the console interface. Enter `help` at any time
when Bola is not waiting for confirmation to see a command summary.

## Command summary

| Action | Command | Example |
| --- | --- | --- |
| Add a to-do | `todo DESCRIPTION` | `todo buy kopi` |
| Add a deadline | `deadline DESCRIPTION /by DATE` | `deadline submit report /by 2026-09-20` |
| Add an event | `event DESCRIPTION /from START /to END` | `event demo /from 20/9/2026 1400 /to 20/9/2026 1500` |
| Show all tasks | `list` | `list` |
| Find tasks | `find KEYWORD` | `find report` |
| Show upcoming tasks | `upcoming DAYS` | `upcoming 7` |
| Mark tasks complete | `mark SELECTION` | `mark 1, 3-5` |
| Mark tasks incomplete | `unmark SELECTION` | `unmark 2` |
| Delete tasks | `delete SELECTION` | `delete all` |
| Show command help | `help` | `help` |
| Exit | `bye` | `bye` |

Commands are case-sensitive, but confirmation answers (`Yes` and `No`) are not.

## Adding tasks

### To-dos

Use `todo` for a task without a date:

```text
todo buy kopi
```

### Deadlines

Use `deadline` and `/by` for work that must be completed by a date or time:

```text
deadline submit report /by 2026-09-20
deadline submit report /by 20/9/2026 1800
```

### Events

Use `event`, `/from`, and `/to` for an activity with a start and end. The end must be later
than the start:

```text
event project demo /from 20/9/2026 1400 /to 20/9/2026 1500
```

Dates accept `yyyy-MM-dd`. A date with a time accepts `yyyy-MM-dd HHmm` or `d/M/yyyy HHmm`.
Bola rejects impossible dates and duplicate tasks with an explanation.

## Viewing and finding tasks

Enter `list` to show every task and its number. Completed tasks contain `[X]`; incomplete tasks
contain `[ ]`. The GUI list also provides checkboxes that mark or unmark tasks and save the
change immediately.

Use `find KEYWORD` for a case-insensitive description search:

```text
find report
```

Use `upcoming DAYS` to show dated tasks within a positive number of days, sorted by date:

```text
upcoming 7
```

## Updating several tasks

`mark`, `unmark`, and `delete` accept one or more task numbers, inclusive ranges, or `all`.
Separate selections with spaces, commas, or both:

```text
mark 1, 3-5 8
unmark 2 4-6
delete 1, 3
delete all
```

Task numbers refer to the list before the operation begins. Bola processes repeated or
overlapping selections once. If any number or range is invalid, Bola rejects the entire command
without changing tasks.

Deleting two or more tasks requires confirmation. Every operation using `all` also requires
confirmation. Enter `Yes` to continue or `No` to cancel. While confirmation is pending, Bola
accepts only those two answers.

## Data and error recovery

On first use, a missing data file is normal: Bola starts with an empty list and creates the
`data` folder and `bola.txt` when the first task is saved.

If an existing file cannot be read or contains invalid records, Bola shows a storage warning,
starts with an empty in-memory list, and does not overwrite that file during the session. If a
save later fails, Bola warns that further changes in that session will not be saved. Fix the
file or folder permissions and restart Bola before relying on persistence again.

For command mistakes, read Bola's error message and correct the indicated description, date,
task number, range, or separator. An invalid command never partially changes the task list.

## Exiting

Enter `bye` to end the session. The GUI disables further input, displays a short closing
countdown, and closes automatically. Saved tasks are restored the next time Bola starts from
the same folder.

## Acknowledgements

- This project started from the [SE-EDU iP starter repository](https://github.com/NUS-CS2103-AY2627-S1/ip).
- The JavaFX application structure was adapted from the
  [SE-EDU JavaFX tutorial](https://se-education.org/guides/tutorials/javaFxPart1.html).
- Codex was used as an AI coding collaborator for the Week 6 `A-BetterGui`, `A-Personality`,
  `A-MoreErrorHandling`, and `A-MoreTesting` increments.
- The kopitiam background and the Bola/user avatar artwork were generated with OpenAI's image
  generation tool. The generation prompts are retained in the repository under `output/`.
