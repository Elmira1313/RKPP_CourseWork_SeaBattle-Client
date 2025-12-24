package com.battleship.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class MenuDialogController {

    @FXML private VBox root;

    private Stage stage;
    private Runnable onResume;
    private Runnable onRestart;
    private Runnable onExitToMenu;
    private GameController gameController;

    public void show(GameController gameController,
                     Runnable onResume,
                     Runnable onRestart,
                     Runnable onExitToMenu) {

        this.gameController = gameController;
        this.onResume = onResume;
        this.onRestart = onRestart;
        this.onExitToMenu = onExitToMenu;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/menu_dialog.fxml")
            );
            loader.setController(this);
            Parent root = loader.load();

            stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("Пауза");

            Scene scene = new Scene(root);
            scene.setFill(null);
            stage.setScene(scene);

            Stage mainStage = (Stage) gameController.getStatusLabel().getScene().getWindow();
            stage.setOnShown(e -> {
                double width = root.getBoundsInParent().getWidth();
                double height = root.getBoundsInParent().getHeight();
                stage.setX(mainStage.getX() + (mainStage.getWidth() - width) / 2);
                stage.setY(mainStage.getY() + (mainStage.getHeight() - height) / 2);
            });

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onResume() {
        if (onResume != null) onResume.run();
        close();
    }

    @FXML
    private void onRestart() {
        if (onRestart != null) onRestart.run();
        close();
    }

    @FXML
    private void onExitToMenu() {
        if (onExitToMenu != null) onExitToMenu.run();
        close();
    }

    private void close() {
        if (stage != null) {
            stage.close();
        }
    }
}