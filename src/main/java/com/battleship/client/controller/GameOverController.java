package com.battleship.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class GameOverController {

    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private Button restartButton;
    @FXML private Button exitButton;

    private Stage stage;
    private GameController gameController;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Runnable onRestartCallback;
    private Runnable onExitCallback;

    public void show(boolean playerWon, GameController gameController,
                     ObjectOutputStream out, ObjectInputStream in,
                     Runnable onRestart, Runnable onExit) {

        this.gameController = gameController;
        this.out = out;
        this.in = in;
        this.onRestartCallback = onRestart;
        this.onExitCallback = onExit;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/game_over_dialog.fxml")
            );
            loader.setController(this);
            Parent root = loader.load();

            if (playerWon) {
                titleLabel.setText("ПОБЕДА!");
                titleLabel.setStyle("-fx-text-fill: #a6e3a1;");
                messageLabel.setText("Вы потопили все корабли противника!");
            } else {
                titleLabel.setText("ПОРАЖЕНИЕ");
                titleLabel.setStyle("-fx-text-fill: #f38ba8;");
                messageLabel.setText("Все ваши корабли потоплены...");
            }

            stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Игра окончена");

            Scene scene = new Scene(root);
            scene.setFill(null);
            stage.setScene(scene);

            centerOnMainStage(root);

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void centerOnMainStage(Parent root) {
        Stage mainStage = (Stage) gameController.getStatusLabel().getScene().getWindow();

        stage.setOnShown(event -> {
            double width = root.getBoundsInParent().getWidth();
            double height = root.getBoundsInParent().getHeight();

            stage.setX(mainStage.getX() + (mainStage.getWidth() - width) / 2);
            stage.setY(mainStage.getY() + (mainStage.getHeight() - height) / 2);
        });
    }

    @FXML
    private void onRestart() {
        if (onRestartCallback != null) {
            onRestartCallback.run();
        }
        close();
    }

    @FXML
    private void onExitToMenu() {
        if (onExitCallback != null) {
            onExitCallback.run();
        }
        close();
    }

    private void close() {
        if (stage != null) {
            stage.close();
        }
    }
}