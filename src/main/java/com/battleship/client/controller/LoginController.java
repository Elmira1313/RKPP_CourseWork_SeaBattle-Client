package com.battleship.client.controller;

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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;

public class LoginController {

    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;

    @FXML private void initialize() {
        connectToServer();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 8888);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Нет связи с сервером"));
            }
        }).start();
    }

    @FXML private void onLogin() {
        String login = loginField.getText().trim();
        String pass = passwordField.getText();
        if (login.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Заполните поля");
            return;
        }
        send(new Message(MessageType.LOGIN, new String[]{login, pass}));
        statusLabel.setText("Вход...");
    }

    @FXML private void onRegister() {
        String login = loginField.getText().trim();
        String pass = passwordField.getText();
        if (login.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("Заполните поля");
            return;
        }
        send(new Message(MessageType.REGISTER, new String[]{login, pass}));
        statusLabel.setText("Регистрация...");
    }

    private void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
            new Thread(this::waitForLoginResponse).start();
        } catch (Exception e) {
            Platform.runLater(() -> statusLabel.setText("Ошибка отправки"));
        }
    }

    private void waitForLoginResponse() {
        try {
            Message msg = (Message) in.readObject();
            Platform.runLater(() -> {
                if (msg.getType() == MessageType.LOGIN_SUCCESS || msg.getType() == MessageType.REGISTER_SUCCESS) {
                    String username = msg.getType() == MessageType.LOGIN_SUCCESS
                            ? (String) msg.getPayload()
                            : loginField.getText();

                    openLobby(username);
                } else {
                    statusLabel.setText("Ошибка: " + msg.getPayload());
                }
            });
        } catch (Exception e) {
            Platform.runLater(() -> statusLabel.setText("Соединение разорвано"));
        }
    }

    private void openLobby(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleship/client/view/lobby.fxml"));
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 600, 700));

            LobbyController lobby = loader.getController();
            lobby.initData(username, out, in);
            lobby.startListening();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}