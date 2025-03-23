package org.example.ahorcadoborradofx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class AhorcadoApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AhorcadoApplication.class.getResource("ahorcado-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Ahorcado");
        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));

        AhorcadoController controller = fxmlLoader.getController();
        controller.setStage(stage);
        stage.show();


    }

    public static void main(String[] args) {
        launch();
    }
}