package com.battleship.client.controller;

import com.battleship.client.NetworkManager;
import com.battleship.common.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;

public class GameController implements Initializable {

    @FXML private GridPane playerGrid;
    @FXML private GridPane computerGrid;
    @FXML private Label statusLabel;
    @FXML private Rectangle playerTurnIndicator;
    @FXML private Rectangle computerTurnIndicator;

    @FXML private Rectangle playerShip4, playerShip3, playerShip2, playerShip1;
    @FXML private Label playerCount4, playerCount3, playerCount2, playerCount1;

    @FXML private Rectangle computerShip4, computerShip3, computerShip2, computerShip1;
    @FXML private Label computerCount4, computerCount3, computerCount2, computerCount1;

    private Game game;
    private NetworkManager networkManager;
    private GameOverController gameOverController;

    private final Set<String> playerSunkCells = new HashSet<>();
    private final Set<String> computerSunkCells = new HashSet<>();
    private boolean canShoot = true;

    private final Map<Integer, Integer> playerRemainingShips = new HashMap<>();
    private final Map<Integer, Integer> computerRemainingShips = new HashMap<>();

    private static final Color WATER = Color.web("#1e1e2e");
    private static final Color SHIP = Color.web("#89b4fa");
    private static final Color HIT = Color.web("#f9e2af");
    private static final Color SUNK = Color.web("#f38ba8");
    private static final Color MISS = Color.web("#585b70");
    private static final Color LINE = Color.web("#585b70");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statusLabel.setText("Ваш ход!");
        initNetworkHandlers();

        Platform.runLater(() -> {
            if (statusLabel.getScene() != null) {
                statusLabel.getScene().setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ESCAPE) {
                        onPause();
                    }
                });
            }
        });
    }

    public void initGame(Game game) {
        this.game = game;
        this.networkManager = NetworkManager.getInstance();

        playerSunkCells.clear();
        computerSunkCells.clear();

        restoreGameState();

        renderFields();
        initRemainingShips();
        updateShipsUI();
        updateTurnFromGameState();

        setTurnIndicator(game.isPlayerTurn);
        canShoot = game.isPlayerTurn && !game.gameOver;
    }

    private void restoreGameState() {
        for (Ship ship : game.playerShips) {
            boolean sunk = true;
            for (int[] cell : ship.cells) {
                if (game.playerField[cell[0]][cell[1]] != Game.CELL_SUNK &&
                        game.playerField[cell[0]][cell[1]] != Game.CELL_HIT) {
                    sunk = false;
                    break;
                }
            }
            if (sunk) {
                for (int[] cell : ship.cells) {
                    playerSunkCells.add(cell[0] + "," + cell[1]);
                }
            }
        }

        for (Ship ship : game.computerShips) {
            boolean sunk = true;
            for (int[] cell : ship.cells) {
                if (game.computerField[cell[0]][cell[1]] != Game.CELL_SUNK &&
                        game.computerField[cell[0]][cell[1]] != Game.CELL_HIT) {
                    sunk = false;
                    break;
                }
            }
            if (sunk) {
                for (int[] cell : ship.cells) {
                    computerSunkCells.add(cell[0] + "," + cell[1]);
                }
            }
        }
    }

    private void updateTurnFromGameState() {
        canShoot = game.isPlayerTurn && !game.gameOver;
    }

    private void initNetworkHandlers() {
        NetworkManager manager = NetworkManager.getInstance();

        manager.registerHandler(MessageType.SHOT_RESULT, msg -> handleShotResult(msg));
        manager.registerHandler(MessageType.OPPONENT_SHOT, msg -> handleOpponentShot(msg));
        manager.registerHandler(MessageType.GAME_OVER, msg -> handleGameOver(msg));
        manager.registerHandler(MessageType.SHIP_SUNK, msg -> handleShipSunk(msg));

        manager.setOnErrorReceived(error -> {
            Platform.runLater(() -> statusLabel.setText("Ошибка: " + error));
        });
    }

    private void renderFields() {
        renderGrid(playerGrid, game.playerField, false);
        renderGrid(computerGrid, game.computerField, true);
    }

    private void renderGrid(GridPane grid, int[][] field, boolean isEnemy) {
        grid.getChildren().clear();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                StackPane cell = new StackPane();
                cell.setAlignment(Pos.CENTER);
                Rectangle rect = new Rectangle(39, 39);
                rect.setStroke(LINE);
                rect.setStrokeWidth(1);

                Color fillColor = getCellColor(field, row, col, isEnemy);
                rect.setFill(fillColor);

                if (isEnemy && canShoot && game.playerShots[row][col] == Game.CELL_EMPTY) {
                    final int r = row, c = col;
                    rect.setOnMouseClicked(e -> shoot(r, c));
                } else {
                    rect.setDisable(true);
                }

                cell.getChildren().add(rect);
                grid.add(cell, col, row);
            }
        }
    }

    private Color getCellColor(int[][] field, int row, int col, boolean isEnemy) {
        int cellState = field[row][col];

        switch (cellState) {
            case Game.CELL_SHIP:
                return isEnemy ? WATER : SHIP;

            case Game.CELL_HIT:
                return HIT;

            case Game.CELL_SUNK:
                return SUNK;

            case Game.CELL_MISS:
                return MISS;

            case Game.CELL_EMPTY:
            default:
                if (isEnemy && game.playerShots[row][col] == Game.CELL_MISS) {
                    return MISS;
                }
                return WATER;
        }
    }

    private void shoot(int row, int col) {
        if (!canShoot) {
            statusLabel.setText("Сейчас не ваш ход!");
            return;
        }

        if (game.playerShots[row][col] != Game.CELL_EMPTY) {
            statusLabel.setText("Сюда уже стреляли!");
            return;
        }

        try {
            canShoot = false;
            networkManager.send(MessageType.SHOT, new int[]{row, col});
            statusLabel.setText("Выстрел отправлен...");
        } catch (Exception e) {
            statusLabel.setText("Ошибка отправки!");
            canShoot = true;
        }
    }

    private void handleShotResult(Message msg) {
        int[] res = (int[]) msg.getPayload();
        int row = res[0], col = res[1];
        boolean hit = res[2] == 1;

        if (hit) {
            game.playerShots[row][col] = Game.CELL_HIT;
            game.computerField[row][col] = Game.CELL_HIT;
        } else {
            game.playerShots[row][col] = Game.CELL_MISS;
            game.computerField[row][col] = Game.CELL_MISS;
            game.isPlayerTurn = false;
        }

        updateCell(computerGrid, row, col, hit);

        game.isPlayerTurn = hit;
        setTurnIndicator(game.isPlayerTurn);
        canShoot = game.isPlayerTurn && !game.gameOver;

        if (hit) {
            statusLabel.setText("Попадание! Ваш ход продолжается.");
        } else {
            statusLabel.setText("Промах. Ход противника.");
        }
    }

    private void handleOpponentShot(Message msg) {
        int[] res = (int[]) msg.getPayload();
        int row = res[0], col = res[1];
        boolean hit = res[2] == 1;

        if (hit) {
            game.computerShots[row][col] = Game.CELL_HIT;
            game.playerField[row][col] = Game.CELL_HIT;
        } else {
            game.computerShots[row][col] = Game.CELL_MISS;
            game.playerField[row][col] = Game.CELL_MISS;
            game.isPlayerTurn = true;
        }

        updateCell(playerGrid, row, col, hit);

        game.isPlayerTurn = !hit;
        setTurnIndicator(game.isPlayerTurn);
        canShoot = game.isPlayerTurn && !game.gameOver;

        if (hit) {
            statusLabel.setText("Противник попал! Его ход продолжается.");
        } else {
            statusLabel.setText("Противник промахнулся! Ваш ход.");
        }
    }

    private void handleGameOver(Message msg) {
        boolean win = (boolean) msg.getPayload();
        game.gameOver = true;
        game.playerWon = win;

        statusLabel.setText(win ? "ПОБЕДА!" : "ПОРАЖЕНИЕ!");
        setTurnIndicator(false);
        canShoot = false;
        showGameOverDialog(win);
    }

    private void handleShipSunk(Message msg) {
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

            if (isPlayerField) {
                game.playerField[row][col] = Game.CELL_SUNK;
                game.computerShots[row][col] = Game.CELL_SUNK;
            } else {
                game.computerField[row][col] = Game.CELL_SUNK;
                game.playerShots[row][col] = Game.CELL_SUNK;
            }

            updateCell(targetGrid, row, col, true);
        }

        int shipSize = cellsList.size();
        if (isPlayerField) {
            playerRemainingShips.put(shipSize, playerRemainingShips.get(shipSize) - 1);
        } else {
            computerRemainingShips.put(shipSize, computerRemainingShips.get(shipSize) - 1);
        }

        updateShipsUI();
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
                    networkManager,
                    this::restartGame,
                    this::exitToMenu
            );
        });
    }

    private void restartGame() {
        try {
            Stage currentStage = (Stage) statusLabel.getScene().getWindow();

            NetworkManager nm = NetworkManager.getInstance();
            nm.clearHandlers();

            nm.registerHandler(MessageType.GAME_STATE, msg -> {
                if (msg.getPayload() instanceof Game game) {
                    Platform.runLater(() -> {
                        System.out.println("Получена НОВАЯ игра из restartGame, открываю placement");
                        openPlacementScreen(game, currentStage);
                    });
                }
            });

            nm.send(MessageType.START_NEW_GAME, null);

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка при перезапуске игры");
        }
    }

    private void openPlacementScreen(Game game, Stage currentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/placement.fxml")
            );
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load(), 1200, 800));
            stage.centerOnScreen();
            stage.setTitle("Морской бой — расстановка");

            PlacementController controller = loader.getController();
            controller.initGame(game);

            currentStage.close();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка при открытии расстановки");
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
            Stage currentStage = (Stage) statusLabel.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/battleship/client/view/lobby.fxml")
            );
            Scene scene = new Scene(loader.load(), 600, 700);
            currentStage.setScene(scene);
            currentStage.centerOnScreen();
            currentStage.setTitle("Морской бой — Лобби");

            LobbyController controller = loader.getController();
            controller.reconnectAfterGame();

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

    private void initRemainingShips() {
        int player4 = 0, player3 = 0, player2 = 0, player1 = 0;
        for (Ship ship : game.playerShips) {
            boolean sunk = true;
            for (int[] cell : ship.cells) {
                if (game.playerField[cell[0]][cell[1]] != Game.CELL_SUNK &&
                        game.playerField[cell[0]][cell[1]] != Game.CELL_HIT) {
                    sunk = false;
                    break;
                }
            }
            if (!sunk) {
                switch (ship.size) {
                    case 4: player4++; break;
                    case 3: player3++; break;
                    case 2: player2++; break;
                    case 1: player1++; break;
                }
            }
        }

        int computer4 = 0, computer3 = 0, computer2 = 0, computer1 = 0;
        for (Ship ship : game.computerShips) {
            boolean sunk = true;
            for (int[] cell : ship.cells) {
                if (game.computerField[cell[0]][cell[1]] != Game.CELL_SUNK &&
                        game.computerField[cell[0]][cell[1]] != Game.CELL_HIT) {
                    sunk = false;
                    break;
                }
            }
            if (!sunk) {
                switch (ship.size) {
                    case 4: computer4++; break;
                    case 3: computer3++; break;
                    case 2: computer2++; break;
                    case 1: computer1++; break;
                }
            }
        }

        playerRemainingShips.put(4, player4);
        playerRemainingShips.put(3, player3);
        playerRemainingShips.put(2, player2);
        playerRemainingShips.put(1, player1);

        computerRemainingShips.put(4, computer4);
        computerRemainingShips.put(3, computer3);
        computerRemainingShips.put(2, computer2);
        computerRemainingShips.put(1, computer1);
    }

    private void updateShipsUI() {
        updateShipUI(playerShip4, playerCount4, 4, playerRemainingShips);
        updateShipUI(playerShip3, playerCount3, 3, playerRemainingShips);
        updateShipUI(playerShip2, playerCount2, 2, playerRemainingShips);
        updateShipUI(playerShip1, playerCount1, 1, playerRemainingShips);

        updateShipUI(computerShip4, computerCount4, 4, computerRemainingShips);
        updateShipUI(computerShip3, computerCount3, 3, computerRemainingShips);
        updateShipUI(computerShip2, computerCount2, 2, computerRemainingShips);
        updateShipUI(computerShip1, computerCount1, 1, computerRemainingShips);
    }

    private void updateShipUI(Rectangle ship, Label countLabel, int size, Map<Integer, Integer> remaining) {
        int count = remaining.getOrDefault(size, 0);
        countLabel.setText("×" + count);
        if (count > 0) {
            ship.setFill(Color.web("#89b4fa"));
        } else {
            ship.setFill(Color.web("#585b70"));
        }
    }

    public Label getStatusLabel() {
        return statusLabel;
    }
}