package com.battleship.client.controller;

import com.battleship.common.Game;
import com.battleship.common.Message;
import com.battleship.common.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class LobbyController {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String currentUser;
    private Thread listenerThread;
    private volatile boolean listening = true;

    public void initData(String username, ObjectOutputStream out, ObjectInputStream in) {
        this.currentUser = username;
        this.out = out;
        this.in = in;
        welcomeLabel.setText("Привет, " + username);
        statusLabel.setText("Готов к бою");
    }

    @FXML private void onNewGame() {
        send(new Message(MessageType.START_NEW_GAME));
        statusLabel.setText("Создаём игру...");
    }

    @FXML private void onExit() {
        stopListening();
        Platform.exit();
        System.exit(0);
    }

    private void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            statusLabel.setText("Ошибка связи");
        }
    }

    public void startListening() {
        listenerThread = new Thread(() -> {
            try {
                while (listening) {
                    Object obj = in.readObject();

                    if (obj instanceof Message msg) {
                        Platform.runLater(() -> {
                            if (msg.getType() == MessageType.GAME_STATE) {
                                Game game = (Game) msg.getPayload();
                                stopListening();
                                openPlacementScreen(game);
                            } else if (msg.getType() == MessageType.ERROR) {
                                statusLabel.setText("Ошибка: " + msg.getPayload());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                if (listening) {
                    e.printStackTrace();
                    Platform.runLater(() -> statusLabel.setText("Соединение потеряно"));
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void stopListening() {
        listening = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    private void openPlacementScreen(Game game) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/placement.fxml")
            );
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1200, 800));
            stage.centerOnScreen();
            stage.setTitle("Морской бой — расстановка");

            PlacementController controller = loader.getController();
            controller.initGame(game, out, in);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onContinue() {
        statusLabel.setText("Сохранённые игры — скоро будет!");
    }
}