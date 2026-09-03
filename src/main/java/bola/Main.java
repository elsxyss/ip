package bola;

import java.io.IOException;

import bola.ui.MainWindow;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Loads the FXML chat window and connects it to Bola.
 */
public class Main extends Application {
    private final Bola bola = new Bola("data/bola.txt");
    private MainWindow controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/view/MainWindow.fxml"));
        AnchorPane root = loader.load();
        controller = loader.getController();
        controller.setBola(bola);

        stage.setTitle("Bola");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
        controller.focusInput();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.stop();
        }
    }
}
