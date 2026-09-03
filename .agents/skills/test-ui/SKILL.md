---
name: test-ui
description: Run fail-fast end-to-end tests for this project's console UI from command and expected-output lists, maintain test/ui-test-plan.md, and report the complete console session. Use when adding, updating, or executing Bola text UI test cases.
---

# Test UI

Use `test/ui-test-plan.md` as the source of truth for UI test cases and test configuration. Work from the repository root.

## Accept test cases

Accept either test cases already recorded in the plan or lists supplied by the user. A supplied case must have:

- a concise aim;
- one or more console commands in execution order; and
- one expected output block for every command.

Treat the command and expected-output lists as positional pairs and verify that their lengths match. If the aim is omitted but is clear from the commands, write a concise aim. Stop and ask for the missing information only when a safe expectation cannot be inferred.

Before testing, add or update the cases in `test/ui-test-plan.md`. Preserve unrelated existing cases and configuration. Follow the file's established Markdown structure; every case must visibly state its aim, inputs, and expected outputs.

## Prepare an isolated session

1. Read the plan, `README.md`, and the relevant build configuration to determine the launch command and setup.
2. Verify Java 25 with `java -version`. On macOS, if necessary, select it with `sdk use java 25.0.3.fx-zulu` in the same shell used to build and run the application.
3. Build the executable JAR once with `./gradlew shadowJar` unless the plan specifies another build step.
4. Create a temporary working directory for each test case, copy `build/libs/bola.jar` into it, and apply that case's data-file precondition there. This prevents UI tests from changing the developer's real `data/bola.txt`.
5. Start `java -jar bola.jar` interactively from that temporary directory. Capture the welcome text as part of the transcript, but do not compare it with a command's expected output unless the plan explicitly includes it.

Do not silently update expected output to match the program. A requested expectation describes the required behavior.

## Run and compare

Run cases in plan order. Within each case, keep one application process open so later commands can exercise state created by earlier commands.

For every input/output pair:

1. Send exactly one command.
2. Capture only the response caused by that command. Exclude terminal input echo and the standard inter-response divider unless the expected block explicitly includes it. The goodbye outer divider is part of the `bye` response.
3. Normalize CRLF to LF and remove ANSI color control sequences. Make no other changes: compare text, whitespace, punctuation, and line order exactly.
4. Compare the actual response with its paired expected block before sending the next command.
5. Append both the typed command and raw displayed response to the session transcript.

If the response matches, continue. If it differs, immediately terminate the application process, skip all remaining commands and cases, and report:

- the failed case and command;
- the expected output in a fenced block;
- the actual normalized output in a fenced block; and
- the console transcript up to termination.

Do not rerun or continue after a failure unless the user asks.

## Report the session

After a fully passing run, report the number of passed cases and commands, then show the complete console input/output transcript in a fenced `text` block. Prefix typed commands with `> ` so they are distinguishable from program output. Include the welcome and shutdown output in the transcript even when those lines were excluded from comparison.

Report the exact build or launch error and stop if the application cannot be started; do not describe an unexecuted case as passed or failed.
