package com.battleship.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainClient extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/battleship/client/view/login.fxml")
        );
        Scene scene = new Scene(loader.load(), 500, 600);
        stage.setScene(scene);
        stage.setTitle("Морской бой — вход");
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}