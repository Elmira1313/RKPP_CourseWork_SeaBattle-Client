package com.battleship.client.controller;

import com.battleship.client.NetworkManager;
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

public class GameOverController {

    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private Button restartButton;
    @FXML private Button exitButton;

    private Stage dialogStage;
    private Runnable onRestartCallback;
    private Runnable onExitCallback;

    public void show(boolean playerWon,
                     GameController gameController,
                     NetworkManager networkManager,
                     Runnable onRestart,
                     Runnable onExit) {

        this.onRestartCallback = onRestart;
        this.onExitCallback = onExit;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/game_over_dialog.fxml")
            );
            loader.setController(this);
            Parent root = loader.load();

            Stage parentStage = (Stage) gameController.getStatusLabel().getScene().getWindow();

            if (playerWon) {
                titleLabel.setText("ПОБЕДА!");
                titleLabel.setStyle("-fx-text-fill: #a6e3a1;");
                messageLabel.setText("Вы потопили все корабли противника!");
            } else {
                titleLabel.setText("ПОРАЖЕНИЕ");
                titleLabel.setStyle("-fx-text-fill: #f38ba8;");
                messageLabel.setText("Все ваши корабли потоплены...");
            }

            dialogStage = new Stage();
            dialogStage.initOwner(parentStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.setTitle("Игра окончена");

            Scene scene = new Scene(root);
            scene.setFill(null);
            dialogStage.setScene(scene);

            dialogStage.setOnShown(e -> centerOnParent(parentStage, root));
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void centerOnParent(Stage parentStage, Parent root) {
        double width = root.getBoundsInParent().getWidth();
        double height = root.getBoundsInParent().getHeight();
        dialogStage.setX(parentStage.getX() + (parentStage.getWidth() - width) / 2);
        dialogStage.setY(parentStage.getY() + (parentStage.getHeight() - height) / 2);
    }

    @FXML
    private void onRestart() {
        if (onRestartCallback != null) onRestartCallback.run();
        close();
    }

    @FXML
    private void onExitToMenu() {
        if (onExitCallback != null) onExitCallback.run();
        close();
    }

    private void close() {
        if (dialogStage != null) dialogStage.close();
    }

    public Label getStatusLabel() {
        return null;
    }
}