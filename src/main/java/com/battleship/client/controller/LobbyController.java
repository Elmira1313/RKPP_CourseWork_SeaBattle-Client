package com.battleship.client.controller;

import com.battleship.common.Message;
import com.battleship.common.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class LobbyController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String currentUser;

    public void initData(String username, ObjectOutputStream out, ObjectInputStream in) {
        this.currentUser = username;
        this.out = out;
        this.in = in;
        welcomeLabel.setText("Привет, " + username + "!");
        statusLabel.setText("Готов к бою!");
    }

    @FXML
    private void onNewGame() {
        send(new Message(MessageType.START_NEW_GAME));
        statusLabel.setText("Запрос на новую игру отправлен...");
    }

    @FXML
    private void onContinue() {
        send(new Message(MessageType.CONTINUE_GAME));
        statusLabel.setText("Поиск сохранённой игры...");
    }

    @FXML
    private void onExit() {
        Platform.exit();
        System.exit(0);
    }

    private void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            statusLabel.setText("Ошибка связи с сервером");
        }
    }

    public void startListening() {
        new Thread(() -> {
            try {
                while (true) {
                    Message msg = (Message) in.readObject();
                    Platform.runLater(() -> {
                        if (msg.getType() == MessageType.GAME_STATE) {
                            statusLabel.setText("Игра готова! (скоро будет расстановка)");
                        }
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }
}