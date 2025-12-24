package com.battleship.client.controller;

import com.battleship.common.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class GameController implements Initializable {

    @FXML private GridPane playerGrid;
    @FXML private GridPane computerGrid;
    @FXML private Label statusLabel;
    @FXML private Rectangle playerTurnIndicator;
    @FXML private Rectangle computerTurnIndicator;


    private Game game;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Socket socket;
    private Thread listenerThread;
    private volatile boolean listening = true;
    private GameOverController gameOverController;

    private final boolean[][] playerShots = new boolean[10][10];
    private final Set<String> playerSunkCells = new HashSet<>();
    private final Set<String> computerSunkCells = new HashSet<>();
    private boolean canShoot = true;

    private static final Color WATER = Color.web("#1e1e2e");
    private static final Color SHIP = Color.web("#89b4fa");
    private static final Color HIT = Color.web("#f9e2af");
    private static final Color SUNK = Color.web("#f38ba8");
    private static final Color MISS = Color.web("#585b70");
    private static final Color LINE = Color.web("#585b70");

    public Label getStatusLabel() {
        return statusLabel;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusLabel.setText("Ваш ход!");
    }

    public void initGame(Game game, ObjectOutputStream out, ObjectInputStream in, Socket socket) {
        this.game = game;
        this.out = out;
        this.in = in;
        this.socket = socket;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                playerShots[i][j] = false;
            }
        }
        playerSunkCells.clear();
        computerSunkCells.clear();
        canShoot = true;
        renderFields();
        startListening();

        setTurnIndicator(true);
        statusLabel.getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                onPause();
            }
        });
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
        if (!canShoot) return;
        if (playerShots[row][col]) return;

        try {
            canShoot = false;
            out.writeObject(new Message(MessageType.SHOT, new int[]{row, col}));
            statusLabel.setText("Выстрел отправлен...");
        } catch (IOException e) {
            statusLabel.setText("Ошибка!");
            canShoot = true;
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

                playerShots[row][col] = true;
                updateCell(computerGrid, row, col, hit);

                if (hit) {
                    statusLabel.setText("Попадание! Ваш ход продолжается.");
                    setTurnIndicator(true);
                    canShoot = true;
                } else {
                    statusLabel.setText("Промах. Ход противника.");
                    setTurnIndicator(false);
                    canShoot = false;
                }
            }
            case OPPONENT_SHOT -> {
                int[] res = (int[]) msg.getPayload();
                int row = res[0], col = res[1];
                boolean hit = res[2] == 1;

                updateCell(playerGrid, row, col, hit);

                if (hit) {
                    statusLabel.setText("Противник попал! Его ход продолжается.");
                    setTurnIndicator(false);
                    canShoot = false;
                } else {
                    statusLabel.setText("Противник промахнулся! Ваш ход.");
                    setTurnIndicator(true);
                    canShoot = true;
                }
            }
            case GAME_OVER -> {
                boolean win = (boolean) msg.getPayload();
                statusLabel.setText(win ? "ПОБЕДА!" : "ПОРАЖЕНИЕ!");
                setTurnIndicator(false);
                stopListening();
                canShoot = false;
                showGameOverDialog(win);
            }
            case SHIP_SUNK -> {
                Object[] data = (Object[]) msg.getPayload();
                @SuppressWarnings("unchecked")
                List<int[]> cellsList = (List<int[]>) data[0];
                boolean isPlayerField = (boolean) data[1];

                GridPane targetGrid = isPlayerField ? playerGrid : computerGrid;
                Set<String> targetSunkCells = isPlayerField ? playerSunkCells : computerSunkCells;

                for (int[] cell : cellsList) {
                    int row = cell[0];
                    int col = cell[1];
                    targetSunkCells.add(row + "," + col);
                    updateCell(targetGrid, row, col, true);
                }
            }
        }
    }

    private void setTurnIndicator(boolean playerTurn) {
        if (playerTurn) {
            playerTurnIndicator.setFill(Color.web("#a6e3a1"));
            computerTurnIndicator.setFill(Color.web("#585b70"));
        } else {
            playerTurnIndicator.setFill(Color.web("#585b70"));
            computerTurnIndicator.setFill(Color.web("#f38ba8"));
        }
    }

    private void showGameOverDialog(boolean playerWon) {
        Platform.runLater(() -> {
            gameOverController = new GameOverController();
            gameOverController.show(
                    playerWon,
                    this,
                    out,
                    in,
                    this::restartGame,
                    this::exitToMenu
            );
        });
    }

    private void restartGame() {
        try {
            stopListening();

            Stage currentStage = (Stage) statusLabel.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/placement.fxml")
            );
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 1200, 800));
            stage.centerOnScreen();
            stage.setTitle("Морской бой — расстановка");

            Game newGame = new Game(game.playerName);

            PlacementController controller = loader.getController();
            controller.initGame(game, out, in, socket);

            currentStage.close();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка при перезапуске игры");
        }
    }

    @FXML
    private void onPause() {
        MenuDialogController dialog = new MenuDialogController();
        dialog.show(
                this,
                () -> {},
                this::restartGame,
                this::exitToMenu
        );
    }

    private void exitToMenu() {
        try {
            stopListening();

            Stage currentStage = (Stage) statusLabel.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleship/client/view/lobby.fxml"));
            Scene scene = new Scene(loader.load(), 600, 700);
            currentStage.setScene(scene);
            currentStage.centerOnScreen();
            currentStage.setTitle("Морской бой — Лобби");

            LobbyController controller = loader.getController();
            controller.reconnectAfterGame();

            listening = true;
            controller.initData(game.playerName, out, in, socket);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCell(GridPane grid, int row, int col, boolean hit) {
        Rectangle rect = getCellRect(grid, row, col);
        if (rect != null) {
            String key = row + "," + col;
            if ((grid == playerGrid && playerSunkCells.contains(key)) ||
                    (grid == computerGrid && computerSunkCells.contains(key))) {
                rect.setFill(SUNK);
            } else if (hit) {
                rect.setFill(HIT);
            } else {
                rect.setFill(MISS);
            }
            rect.setDisable(true);
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