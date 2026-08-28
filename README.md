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
1. After that, locate the `src/main/java/bola/Bola.java` file, right-click it, and choose `Run Bola.main()` (if the code editor is showing compile errors, try restarting the IDE).

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
