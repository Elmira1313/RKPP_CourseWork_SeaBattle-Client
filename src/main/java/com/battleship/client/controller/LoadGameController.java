package com.battleship.client.controller;

import com.battleship.client.NetworkManager;
import com.battleship.common.Game;
import com.battleship.common.Message;
import com.battleship.common.MessageType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class LoadGameController {

    @FXML private TableView<Object[]> table;
    @FXML private TableColumn<Object[], String> nameColumn;
    @FXML private TableColumn<Object[], String> difficultColumn;
    @FXML private TableColumn<Object[], String> dateColumn;
    @FXML private TableColumn<Object[], String> timeColumn;
    @FXML private TableColumn<Object[], Long> sizeColumn;
    @FXML private Button loadButton, deleteButton, backButton;
    @FXML private Label statusLabel;

    private ObservableList<Object[]> data = FXCollections.observableArrayList();
    private String playerName;
    private Stage stage;

    public void initData(String playerName, Stage stage) {
        this.playerName = playerName;
        this.stage = stage;

        initTable();
        initNetworkHandlers();

        NetworkManager.getInstance().send(MessageType.GAME_LIST, null);
    }

    private void initNetworkHandlers() {
        NetworkManager nm = NetworkManager.getInstance();

        nm.clearHandlers();

        nm.registerHandler(MessageType.GAME_LIST, msg -> {
            if (msg.getPayload() instanceof Object[][] list) {
                Platform.runLater(() -> {
                    data.clear();
                    if (list.length > 0) {
                        data.addAll(list);
                        statusLabel.setText("");
                    } else {
                        statusLabel.setText("Нет сохранённых игр");
                    }
                });
            }
        });

        nm.registerHandler(MessageType.GAME_STATE, msg -> {
            if (msg.getPayload() instanceof Game game) {
                Platform.runLater(() -> openGameScreen(game));
            }
        });

        nm.setOnErrorReceived(text -> Platform.runLater(() ->
                statusLabel.setText("Ошибка: " + text)
        ));
    }

    private void initTable() {
        nameColumn.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty((String) p.getValue()[0]));
        difficultColumn.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty((String) p.getValue()[1]));
        dateColumn.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty((String) p.getValue()[2]));
        timeColumn.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty((String) p.getValue()[3]));
        sizeColumn.setCellValueFactory(p -> new javafx.beans.property.SimpleLongProperty((Long) p.getValue()[4]).asObject());

        table.setItems(data);
        loadButton.setDisable(true);
        deleteButton.setDisable(true);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            boolean selected = newVal != null;
            loadButton.setDisable(!selected);
            deleteButton.setDisable(!selected);
        });
    }
    private String translate(String dif) {
        switch (dif){
            case "easy" -> { return "Лёгкий";}
            case "normal" -> { return "Средний";}
            case "hard" -> { return "Сложный";}
        }
        return "Средний";
    }

    private void openGameScreen(Game game) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/game.fxml")
            );
            Parent root = loader.load();

            GameController controller = loader.getController();
            controller.initGame(game);

            stage.setScene(new Scene(root, 1500, 800));
            stage.centerOnScreen();
            stage.setTitle("Морской бой — Продолжение игры");

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка загрузки игры");
        }
    }

    @FXML
    private void onLoad() {
        Object[] selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String filename = (String) selected[0];
        NetworkManager.getInstance().send(MessageType.CONTINUE_GAME, filename);
    }

    @FXML
    private void onDelete() {
        Object[] selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String filename = (String) selected[0];
        NetworkManager.getInstance().send(MessageType.DELETE_SAVE, filename);
        NetworkManager.getInstance().send(MessageType.GAME_LIST, null);
    }

    @FXML
    private void onBack() {
        try {
            NetworkManager nm = NetworkManager.getInstance();
            nm.unregisterHandlers(MessageType.GAME_LIST, MessageType.GAME_STATE);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/lobby.fxml")
            );
            Parent root = loader.load();
            stage.setScene(new Scene(root, 600, 700));
            stage.centerOnScreen();

            LobbyController controller = loader.getController();
            controller.initData(playerName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}