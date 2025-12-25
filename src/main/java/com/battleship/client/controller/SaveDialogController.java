package com.battleship.client.controller;

import com.battleship.client.NetworkManager;
import com.battleship.common.MessageType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

public class SaveDialogController {

    @FXML private VBox root;
    private Stage dialogStage;
    private Runnable onYes;
    private Runnable onNo;

    public void show(GameController gameController,
                     Runnable onYesCallback,
                     Runnable onNoCallback) {
        this.onYes = onYesCallback;
        this.onNo = onNoCallback;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/save_dialog.fxml")
            );
            loader.setController(this);
            Parent root = loader.load();

            Stage parentStage = (Stage) gameController.getStatusLabel().getScene().getWindow();

            dialogStage = new Stage();
            dialogStage.initOwner(parentStage);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.setTitle("Сохранение");

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
    private void onYes() {
        NetworkManager.getInstance().send(MessageType.SAVE_GAME, null);
        if (onYes != null) onYes.run();
        close();
    }

    @FXML
    private void onNo() {
        if (onNo != null) onNo.run();
        close();
    }

    private void close() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
}