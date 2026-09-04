# Bola

This is a task-management chatbot built with Java 25.

## Setting up in Intellij

Prerequisites: JDK 25, update Intellij to the most recent version.

1. Open Intellij (if you are not in the welcome screen, click `File` > `Close Project` to close the existing project first)
1. Open the project into Intellij as follows:
   1. Click `Open`.
   1. Select the project directory, and click `OK`.
   1. If there are any further prompts, accept the defaults.
1. Configure the project to use **JDK 25** (not other versions) as explained in [here](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk).<br>
   In the same dialog, set the **Project language level** field to the `SDK default` option.
1. Run `src/main/java/bola/Launcher.java` to open the GUI (or `Bola.main()` for the original console).

## FXML JavaFX GUI

Run `./gradlew run` with Java 25. On macOS, use the JavaFX-bundled JDK:
`sdk use java 25.0.3.fx-zulu`.

The GUI follows [SE-EDU's tutorial Part 4](https://se-education.org/guides/tutorials/javaFxPart4.html):
FXML views define the chat window and dialogs, while Java controllers handle input and automatic scrolling.
Type existing Bola commands and press Enter or click Send. Blank input is ignored.
The window can resize in both directions, following
[tutorial Part 5](https://se-education.org/guides/tutorials/javaFxPart5.html).
The chat area and input follow the window dimensions, while Send stays at the bottom right.
The minimum window size is 400 by 220 pixels.
After `bye`, input is disabled and the farewell appears immediately. After three seconds,
`[Closing in 5 seconds...]` appears as a separate message. After five more seconds
(eight seconds after `bye`), the GUI closes automatically.

The layouts are in `src/main/resources/view/MainWindow.fxml` and `DialogBox.fxml`.
Open these files in Scene Builder to edit the layout. Their controllers are
`bola.ui.MainWindow` and `bola.ui.DialogBox`; `bola.Main` loads the window and connects the chatbot.
`src/main/resources/styles/dialog.css` defines the white, black-bordered bubbles and their speaker-specific corners.

The user-provided kopitiam-buddy avatars are stored in `src/main/resources/images`:
`DaBola.png` is the cheerful kopi cup for Bola, and `DaUser.png` is the smiling kaya toast for the user.

Use `./gradlew run --args='--cli'` or `java -jar bola.jar --cli` for console mode.
Both interfaces use the same commands and storage file.

Run `./gradlew check jacocoTestReport` for JUnit tests, Checkstyle, and the 50% line-coverage gate.

## Building and running the JAR file

Run the following command from the project root to create an executable fat JAR containing the application and its runtime dependencies:

```shell
./gradlew shadowJar
```

On Windows, use `gradlew.bat shadowJar` instead. The generated file is located at `build/libs/bola.jar`.

To run the packaged application, copy `bola.jar` into the folder where you want Bola to store its data, open a command window in that folder, and run:

```shell
java -jar "bola.jar"
```

The application creates its `data/bola.txt` storage file relative to the current folder when a task is first saved. The generated JAR is a build artifact and should not be committed to Git; distribute it through a GitHub release instead.

**Warning:** Keep the `src\main\java` folder as the root folder for Java files (i.e., don't rename those folders or move Java files to another folder outside of this folder path), as this is the default location some tools (e.g., Gradle) expect to find Java files.
