package com.battleship.client.controller;

import com.battleship.client.NetworkManager;
import com.battleship.client.SessionManager;
import com.battleship.common.Message;
import com.battleship.common.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void initialize() {
        NetworkManager nm = NetworkManager.getInstance();

        nm.registerSystemHandler(MessageType.LOGIN_SUCCESS, msg -> {
            if (msg.getPayload() instanceof String username) {
                Platform.runLater(() -> handleLoginSuccess(username));
            }
        });

        nm.registerSystemHandler(MessageType.AUTH_TOKEN, msg -> {
            if (msg.getPayload() instanceof String token) {
                SessionManager.getInstance().setAuthToken(token);
            }
        });

        nm.registerSystemHandler(MessageType.LOGIN_FAIL, msg -> {
            Platform.runLater(() -> {
                statusLabel.setText("Неверное имя пользователя или пароль");
            });
        });

        nm.registerSystemHandler(MessageType.REGISTER_FAIL, msg -> {
            Platform.runLater(() -> {
                String reason = "Ошибка регистрации";
                if (msg.getPayload() instanceof String s) {
                    reason = s;
                } else if (msg.getPayload() instanceof String[] arr && arr.length > 0) {
                    reason = arr[0];
                }
                statusLabel.setText(reason);
            });
        });

        nm.setOnErrorReceived(text -> Platform.runLater(() -> statusLabel.setText(text)));

        connectToServer();
    }

    private void connectToServer() {
        NetworkManager.getInstance().connect("localhost", 8888);
    }

    @FXML
    private void onLogin() {
        String login = loginField.getText().trim();
        String pass = passwordField.getText();
        if (login.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Заполните поля");
            return;
        }

        NetworkManager.getInstance().send(MessageType.LOGIN, new String[]{login, pass});
        statusLabel.setText("Вход...");
    }

    @FXML
    private void onRegister() {
        String login = loginField.getText().trim();
        String pass = passwordField.getText();
        if (login.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Заполните поля");
            return;
        }

        NetworkManager.getInstance().send(MessageType.REGISTER, new String[]{login, pass});
        statusLabel.setText("Регистрация...");
    }

    private void handleLoginSuccess(String username) {
        SessionManager.getInstance().setUsername(username);
        openLobby(username);
    }

    private void openLobby(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/lobby.fxml")
            );
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 600, 700));
            stage.centerOnScreen();
            stage.setTitle("Морской бой — Лобби");

            LobbyController lobbyController = loader.getController();
            lobbyController.initData(username);

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка открытия лобби");
        }
    }
}