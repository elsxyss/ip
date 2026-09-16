# FXML JavaFX GUI test plan (Parts 4 and 5)

Use Java 25 with JavaFX. Build with `./gradlew shadowJar`, copy `build/libs/bola.jar`
to a fresh temporary directory, and run `java -jar bola.jar` there to protect real task data.

1. Check the 400-by-600 chat area, Bola title, greeting with a left avatar, input, and Send button.
   Bola's avatar must be the cheerful kopi cup; user messages must use the smiling kaya toast.
   The greeting must show `Bola: Eh hello! I'm Bola.` on the first line and
   `Got anything to settle today?` on the next line, without a flag. The input and Send button
   must sit in a solid cream toolbar, use rounded corners, and remain legible over the background.
2. Submit `todo read book` with Enter. Check the right user dialog and left task-added response.
   User messages must have muted sage bubbles and Bola replies must have warm cream bubbles,
   each with a subtle matching border and dark, readable text. Round all corners except the
   bottom-right corner for the user and the
   bottom-left corner for Bola. The bubble's top edge must align with the visible artwork's top edge,
   excluding transparent margins and nearly invisible export artifacts in the avatar PNG.
   Check this for both the cup and toast, including a short user message such as `okay`.
   Both avatars must use the same 80-by-80 fit area. The user and Bola rows must have a clear
   vertical gap while remaining visually grouped as one exchange.
   Short bubbles must fit their text instead of stretching to the avatar's height.
   The input should clear and retain focus.
3. Submit `list` with Send. Check that the saved task appears once in Bola's response.
   Each task must appear as its own row with a descriptive `To-do`, `Deadline`, or `Event`
   badge rather than a one-letter marker. All badges must have the same width and centered text,
   so every checkbox begins at the same horizontal position. Each row must have a real checkbox reflecting its
   completion status. Click the first task's checkbox and verify that it becomes selected,
   the normal marked confirmation appears, and the change is saved. Click it again and verify
   that it becomes unselected, the unmarked confirmation appears, and that change is saved too.
   After submitting another typed command, controls in older list responses must be disabled so
   stale task numbers cannot modify a different task.
4. Submit `mark 1`, `unmark 1`, `find book`, and an invalid command.
   Check that normal confirmations appear as cream Bola replies. Validation errors must use a
   pale red bubble, dark red text, and a stronger red left border. Storage warnings must use the
   corresponding amber warning treatment rather than the error treatment.
5. Submit a long task description and enough `list` commands to fill the window.
   Check wrapping, visible avatars, no horizontal scrollbar, and automatic scrolling to the newest reply.
   User bubbles must stop growing at 270 pixels and Bola bubbles at 360 pixels rather than
   stretching indefinitely as the window becomes wider. The wider Bola cap must give task
   descriptions noticeably more room beside their badges and checkboxes.
   Long and multiline messages must stay inside their bubble borders with padding on every side;
   their top edges must still align with the visible avatars and extend below them.
   The supplied kopitiam PNG must be visible behind the messages and stay fixed while scrolling.
   Its proportions must stay unchanged, with centered cropping to fill the chat area at every size.
   Check that the translucent white viewport wash softens the image without hiding it and that
   every bubble color remains readable.
   Enlarge and shrink the window horizontally and vertically. The input must stretch within its
   bottom toolbar, Send must stay at the bottom right, and the chat area must fill the remaining space.
   Messages must rewrap to the viewport width without a horizontal scrollbar or overlapping controls.
   The window must not shrink below 400 pixels wide or 220 pixels high.
6. Submit whitespace only. No dialog should be added.
7. Submit `bye`. Check the farewell appears immediately and both input and Send are disabled.
   After three seconds, a separate Bola dialog must read `[Closing in 5 seconds...]`.
   The farewell must remain in the chat throughout the eight-second closing sequence.
   After five more seconds (eight seconds after `bye`), the window must close automatically.
   Repeat in a fresh session using the other input method (Enter/Send).
8. Reopen the JAR from the same temporary directory. `list` should retain the task.

The packaged JAR must include `view/MainWindow.fxml`, `view/DialogBox.fxml`,
`styles/dialog.css`, `styles/main-window.css`, `images/bola-kopitiam-background.png`,
and both avatar PNGs. Launch it outside the repository to verify that
FXML, CSS, and image paths resolve from the JAR. No FXML loading or injection errors should appear.
Both Enter and Send must invoke the controller handler declared in `MainWindow.fxml`.

Automated checks: `./gradlew check jacocoTestReport` loads the FXML views on the JavaFX thread,
exercises both input handlers and the closing schedule, and checks bubble styling and layout.
