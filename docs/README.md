# Bola User Guide

// Update the title above to match the actual product name

// Product screenshot goes here

// Product intro goes here

## Adding deadlines

// Describe the action and its outcome.

// Give examples of usage

Example: `keyword (optional arguments)`

// A description of the expected outcome goes here

```
expected output
```

## Performing mass operations

Use `mark`, `unmark`, or `delete` with several task numbers to update multiple tasks
in one command. Task numbers are the one-based numbers shown by `list`.

Selections can contain individual numbers, inclusive ranges, or a mixture separated
by spaces and commas:

```text
mark 1, 3-5 8
unmark 2 4-6
delete 1, 3
```

The selected numbers refer to the task list before the command starts. Bola processes
repeated or overlapping selections once and displays affected tasks in their original
list order. If any part of a selection is invalid, Bola rejects the whole command and
does not change any task.

Use `all` by itself to select every task:

```text
mark all
unmark all
delete all
```

Deleting two or more tasks requires confirmation. Every command that uses `all` also
requires confirmation, even when the list contains one task. Answer `Yes` to perform
the pending operation or `No` to cancel it. Confirmation answers ignore letter case.
Until `Yes` or `No` is entered, Bola does not execute other commands.

Examples:

```text
delete 2, 4
Bola: U sure u want to delete these 2 tasks? (Yes/No)

Yes
Bola: Okay, removed these 2 tasks already:
    2. [T][ ] buy kopi
    4. [T][ ] read book
Now got 2 tasks in your list.
```

```text
mark all
Bola: U sure u want to mark all 3 tasks? (Yes/No)

No
Bola: Okay, cancelled. No tasks changed.
```

Enter `help` at any time when no confirmation is pending to view every supported
command and a selection example.

## Feature ABC

// Feature details


## Feature XYZ

// Feature details
