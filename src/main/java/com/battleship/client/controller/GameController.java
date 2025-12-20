package com.battleship.client.controller;

import com.battleship.common.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.ResourceBundle;

public class GameController implements Initializable {

    @FXML private GridPane playerGrid;
    @FXML private GridPane computerGrid;
    @FXML private Label statusLabel;

    private Game game;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listenerThread;
    private volatile boolean listening = true;

    private static final Color WATER = Color.web("#1e1e2e");
    private static final Color SHIP = Color.web("#89b4fa");
    private static final Color HIT = Color.web("#f38ba8");
    private static final Color MISS = Color.web("#585b70");
    private static final Color LINE = Color.web("#585b70");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusLabel.setText("Ваш ход!");
    }

    public void initGame(Game game, ObjectOutputStream out, ObjectInputStream in) {
        this.game = game;
        this.out = out;
        this.in = in;
        renderFields();
        startListening();
    }

    private void renderFields() {
        renderGrid(playerGrid, game.playerField, false);
        renderGrid(computerGrid, null, true);
    }

    private void renderGrid(GridPane grid, boolean[][] field, boolean isEnemy) {
        grid.getChildren().clear();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                StackPane cell = new StackPane();
                Rectangle rect = new Rectangle(40, 40);
                rect.setStroke(LINE);

                if (isEnemy) {
                    rect.setFill(WATER);
                    final int r = row, c = col;
                    rect.setOnMouseClicked(e -> shoot(r, c));
                } else {
                    rect.setFill(field != null && field[row][col] ? SHIP : WATER);
                }

                cell.getChildren().add(rect);
                grid.add(cell, col, row);
            }
        }
    }

    private void shoot(int row, int col) {
        try {
            out.writeObject(new Message(MessageType.SHOT, new int[]{row, col}));
            statusLabel.setText("Выстрел отправлен...");
        } catch (IOException e) {
            statusLabel.setText("Ошибка!");
        }
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            try {
                while (listening) {
                    Message msg = (Message) in.readObject();
                    Platform.runLater(() -> handleMessage(msg));
                }
            } catch (Exception e) {
                if (listening) {
                    Platform.runLater(() -> statusLabel.setText("Соединение разорвано"));
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

    private void handleMessage(Message msg) {
        switch (msg.getType()) {
            case SHOT_RESULT -> {
                int[] res = (int[]) msg.getPayload();
                int row = res[0], col = res[1];
                boolean hit = res[2] == 1;
                updateCell(computerGrid, row, col, hit);
                statusLabel.setText(hit ? "Попадание!" : "Промах. Ход противника.");
            }
            case OPPONENT_SHOT -> {
                int[] res = (int[]) msg.getPayload();
                int row = res[0], col = res[1];
                boolean hit = res[2] == 1;
                updateCell(playerGrid, row, col, hit);
                statusLabel.setText(hit ? "Противник попал!" : "Противник промахнулся!");

                if (hit) {
                    for (Ship ship : game.playerShips) {
                        for (int[] cell : ship.cells) {
                            if (cell[0] == row && cell[1] == col) {
                                ship.hits++;
                                break;
                            }
                        }
                    }
                }
            }
            case GAME_OVER -> {
                boolean win = (boolean) msg.getPayload();
                statusLabel.setText(win ? "ПОБЕДА!" : "ПОРАЖЕНИЕ!");
                stopListening();
            }
        }
    }

    private void updateCell(GridPane grid, int row, int col, boolean hit) {
        Rectangle rect = getCellRect(grid, row, col);
        if (rect != null) {
            rect.setFill(hit ? HIT : MISS);
        }
    }

    private Rectangle getCellRect(GridPane grid, int row, int col) {
        Node cell = getCellNode(grid, row, col);
        return cell != null ? (Rectangle) ((StackPane) cell).getChildren().get(0) : null;
    }

    private Node getCellNode(GridPane grid, int row, int col) {
        for (Node node : grid.getChildren()) {
            Integer r = GridPane.getRowIndex(node);
            Integer c = GridPane.getColumnIndex(node);
            if ((r == null ? 0 : r) == row && (c == null ? 0 : c) == col) {
                return node;
            }
        }
        return null;
    }
}