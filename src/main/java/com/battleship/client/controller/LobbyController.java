package com.battleship.client.controller;

import com.battleship.client.NetworkManager;
import com.battleship.client.SessionManager;
import com.battleship.common.Game;
import com.battleship.common.Message;
import com.battleship.common.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class LobbyController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    private String currentUser;

    public void initData(String username) {
        this.currentUser = username;
        welcomeLabel.setText("Привет, " + username + "!");
        statusLabel.setText("Готов к бою!");

        NetworkManager nm = NetworkManager.getInstance();

        nm.clearHandlers();

        nm.registerHandler(MessageType.GAME_STATE, msg -> {
            if (msg.getPayload() instanceof Game game) {
                Platform.runLater(() -> openPlacementScreen(game));
            }
        });

        nm.setOnErrorReceived(text -> Platform.runLater(() -> statusLabel.setText("Ошибка: " + text)));
    }

    public void reconnectAfterGame() {
        String token = SessionManager.getInstance().getAuthToken();
        String username = SessionManager.getInstance().getUsername();

        if (token != null && username != null) {
            welcomeLabel.setText("Привет, " + username + "!");
            statusLabel.setText("Готов к бою!");
            NetworkManager.getInstance().send(MessageType.RECONNECT_TOKEN, token);
        } else {
            loadLoginScreen();
        }
    }

    private void loadLoginScreen() {
        try {
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/login.fxml")
            );
            stage.setScene(new Scene(loader.load(), 600, 700));
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onNewGame() {
        NetworkManager nm = NetworkManager.getInstance();

        nm.clearHandlers();

        nm.registerHandler(MessageType.GAME_STATE, msg -> {
            if (msg.getPayload() instanceof Game game) {
                Platform.runLater(() -> {
                    System.out.println("Получена НОВАЯ игра, открываю placement");
                    openPlacementScreen(game);
                });
            }
        });

        nm.send(MessageType.START_NEW_GAME, null);
        statusLabel.setText("Создаём игру...");
    }

    @FXML
    private void onLogout() {
        loadLoginScreen();
    }

    @FXML
    private void onExit() {
        NetworkManager.getInstance().close();
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void onContinue() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/load_game.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 600));
            stage.centerOnScreen();
            stage.setTitle("Продолжить игру");

            LoadGameController controller = loader.getController();
            controller.initData(currentUser, stage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openPlacementScreen(Game game) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/placement.fxml")
            );
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Parent root = loader.load();

            PlacementController controller = loader.getController();
            controller.initGame(game);

            stage.setScene(new Scene(root, 1200, 800));
            stage.centerOnScreen();
            stage.setTitle("Морской бой — расстановка");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка: " + e.getMessage());
        }
    }
}