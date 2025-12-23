package com.battleship.client.controller;

import com.battleship.common.Game;
import com.battleship.common.Message;
import com.battleship.common.MessageType;
import com.battleship.common.Ship;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.*;

public class PlacementController implements Initializable {

    @FXML private GridPane playerGrid;
    @FXML private Button readyButton;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> difficultyCombo;

    @FXML private Rectangle ship4Sample, ship3Sample, ship2Sample, ship1Sample;
    @FXML private Label count4Label, count3Label, count2Label, count1Label;

    private Game game;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Rectangle selectedShip = null;
    private int selectedSize = 0;
    private boolean isVertical = false;

    private final Map<Integer, Integer> availableShips = new HashMap<>();
    private final Map<Integer, Integer> maxShips = new HashMap<>();

    private Integer selectedShipType = null;

    private int lastHighlightRow = -1;
    private int lastHighlightCol = -1;

    private static final Color WATER = Color.web("#1e1e2e");
    private static final Color SHIP = Color.web("#89b4fa");
    private static final Color SELECTED = Color.web("#f9e2af");
    private static final Color OK = Color.web("#a6e3a1");
    private static final Color BAD = Color.web("#f38ba8");
    private static final Color LINE = Color.web("#585b70");
    private static final Color UNAVAILABLE = Color.web("#585b70"); // Серый для недоступных

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildGrid();
        initShipCounts();
        statusLabel.setText("Выберите корабль.");
        statusLabel.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 16px;");
        difficultyCombo.setValue("Средний");
    }

    public void initGame(Game game, ObjectOutputStream out, ObjectInputStream in) {
        this.game = game;
        this.out = out;
        this.in = in;
        readyButton.setDisable(true);

        updateShipVisuals();
        playerGrid.getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.R && selectedShip != null) {
                rotateSelected();
            }
        });
    }

    private void buildGrid() {
        playerGrid.getChildren().clear();

        playerGrid.getColumnConstraints().clear();
        playerGrid.getRowConstraints().clear();

        for (int i = 0; i < 10; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setPrefWidth(40);
            colConst.setMinWidth(40);
            colConst.setMaxWidth(40);
            playerGrid.getColumnConstraints().add(colConst);

            RowConstraints rowConst = new RowConstraints();
            rowConst.setPrefHeight(40);
            rowConst.setMinHeight(40);
            rowConst.setMaxHeight(40);
            playerGrid.getRowConstraints().add(rowConst);
        }

        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                StackPane cell = new StackPane();
                cell.setAlignment(Pos.CENTER);
                Rectangle rect = new Rectangle(38, 38);
                rect.setFill(WATER);
                rect.setStroke(LINE);
                rect.setStrokeWidth(1);

                final int r = row;
                final int c = col;

                rect.setOnMouseEntered(e -> {
                    if (selectedShipType != null) {
                        lastHighlightRow = r;
                        lastHighlightCol = c;
                        highlight(r, c);
                    }
                });

                rect.setOnMouseExited(e -> {
                    lastHighlightRow = -1;
                    lastHighlightCol = -1;
                    clearHighlight();
                });

                rect.setOnMouseMoved(e -> {
                    if (selectedShipType != null) {
                        lastHighlightRow = r;
                        lastHighlightCol = c;
                    }
                });

                rect.setOnMouseClicked(e -> onFieldClick(r, c));

                cell.getChildren().add(rect);
                playerGrid.add(cell, col, row);
            }
        }
    }

    private void initShipCounts() {
        maxShips.put(4, 1);
        maxShips.put(3, 2);
        maxShips.put(2, 3);
        maxShips.put(1, 4);

        availableShips.put(4, 1);
        availableShips.put(3, 2);
        availableShips.put(2, 3);
        availableShips.put(1, 4);
    }

    @FXML
    private void onShipSelect(MouseEvent event) {
        Rectangle ship = (Rectangle) event.getSource();
        int size = Integer.parseInt(ship.getUserData().toString());

        if (availableShips.get(size) <= 0) return;

        selectShip(ship, size);
    }

    private void selectShip(Rectangle ship, int size) {
        if (selectedShip != null) {
            selectedShip.setScaleX(1.0);
            selectedShip.setScaleY(1.0);
            selectedShip.setStrokeWidth(3);
            if (availableShips.get(size) > 0) {
                selectedShip.setFill(SHIP);
            } else {
                selectedShip.setFill(UNAVAILABLE);
            }
            selectedShip.setStroke(LINE);
        }

        selectedShip = ship;
        selectedSize = size;
        selectedShipType = size;
        isVertical = ship.getHeight() > ship.getWidth();

        ship.setFill(SELECTED);
        ship.setScaleX(1.15);
        ship.setScaleY(1.15);
        ship.setStrokeWidth(4);

        statusLabel.setText("Корабль выбран. Кликните на поле.");
    }

    private void rotateSelected() {
        if (selectedShipType == null) return;

        isVertical = !isVertical;
        statusLabel.setText("Корабль повернут.");
        if (lastHighlightRow != -1 && lastHighlightCol != -1) {
            highlight(lastHighlightRow, lastHighlightCol);
        }
    }

    private void onFieldClick(int row, int col) {
        if (selectedShipType == null) {
            for (Ship ship : game.playerShips) {
                if (isCellInShip(row, col, ship)) {
                    removeShip(ship);
                    return;
                }
            }
            return;
        }

        if (canPlace(row, col)) {
            placeShip(row, col);
            availableShips.put(selectedSize, availableShips.get(selectedSize) - 1);

            selectedShip.setScaleX(1.0);
            selectedShip.setScaleY(1.0);
            selectedShip.setStrokeWidth(3);
            selectedShip = null;
            selectedShipType = null;

            updateShipVisuals();
            checkReady();
            statusLabel.setText("Корабль установлен!");
        } else {
            statusLabel.setText("Нельзя здесь ставить!");
        }
    }

    private boolean isCellInShip(int row, int col, Ship ship) {
        for (int[] cell : ship.cells) {
            if (cell[0] == row && cell[1] == col) return true;
        }
        return false;
    }

    private void removeShip(Ship shipToRemove) {
        int size = shipToRemove.size;

        availableShips.put(size, availableShips.get(size) + 1);

        game.playerShips.removeIf(s -> s == shipToRemove);
        for (int[] cell : shipToRemove.cells) {
            game.playerField[cell[0]][cell[1]] = false;
            getCellRect(cell[0], cell[1]).setFill(WATER);
        }

        updateShipVisuals();
        statusLabel.setText("Корабль снят. Выберите заново.");
        checkReady();
    }

    private boolean canPlace(int row, int col) {
        if (selectedShipType == null) return false;

        if (isVertical && row + selectedSize > 10) return false;
        if (!isVertical && col + selectedSize > 10) return false;

        for (int i = 0; i < selectedSize; i++) {
            int r = isVertical ? row + i : row;
            int c = isVertical ? col : col + i;

            if (game.playerField[r][c]) return false;

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = r + dr;
                    int nc = c + dc;
                    if (nr >= 0 && nr < 10 && nc >= 0 && nc < 10 && game.playerField[nr][nc]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void placeShip(int row, int col) {
        Ship ship = new Ship(selectedSize);
        ship.isVertical = isVertical;
        for (int i = 0; i < selectedSize; i++) {
            int r = isVertical ? row + i : row;
            int c = isVertical ? col : col + i;
            game.playerField[r][c] = true;
            ship.cells.add(new int[]{r, c});
            getCellRect(r, c).setFill(SHIP);
        }
        game.playerShips.add(ship);
    }

    private void highlight(int row, int col) {
        clearHighlight();
        if (selectedShipType == null) return;

        boolean ok = canPlace(row, col);
        Color color = ok ? OK : BAD;

        for (int i = 0; i < selectedSize; i++) {
            int r = isVertical ? row + i : row;
            int c = isVertical ? col : col + i;
            if (r < 10 && c < 10) {
                getCellRect(r, c).setFill(color);
            }
        }
    }

    private void clearHighlight() {
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                Color fillColor = game.playerField[r][c] ? SHIP : WATER;
                getCellRect(r, c).setFill(fillColor);
            }
        }
    }

    private Rectangle getCellRect(int row, int col) {
        Node cell = getCellNode(row, col);
        return cell != null ? (Rectangle) ((StackPane) cell).getChildren().get(0) : null;
    }

    private Node getCellNode(int row, int col) {
        for (Node node : playerGrid.getChildren()) {
            Integer r = GridPane.getRowIndex(node);
            Integer c = GridPane.getColumnIndex(node);
            if ((r == null ? 0 : r) == row && (c == null ? 0 : c) == col) {
                return node;
            }
        }
        return null;
    }

    private void updateShipVisuals() {
        count4Label.setText("×" + availableShips.get(4));
        count3Label.setText("×" + availableShips.get(3));
        count2Label.setText("×" + availableShips.get(2));
        count1Label.setText("×" + availableShips.get(1));

        updateShipColor(ship4Sample, 4);
        updateShipColor(ship3Sample, 3);
        updateShipColor(ship2Sample, 2);
        updateShipColor(ship1Sample, 1);
    }

    private void updateShipColor(Rectangle ship, int size) {
        if (selectedShip == ship) return;
        if (availableShips.get(size) > 0) {
            ship.setFill(SHIP);
            ship.setStroke(LINE);
        } else {
            ship.setFill(UNAVAILABLE);
            ship.setStroke(Color.web("#45475a"));
        }
    }

    @FXML
    private void onRandomPlacement() {
        clearBoard();
        Random rnd = new Random();
        int[] sizes = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

        initShipCounts();

        for (int size : sizes) {
            while (true) {
                boolean v = rnd.nextBoolean();
                int row = rnd.nextInt(v ? 10 : 11 - size);
                int col = rnd.nextInt(v ? 11 - size : 10);
                if (canPlace(row, col, size, v)) {
                    placeManual(row, col, size, v);
                    availableShips.put(size, availableShips.get(size) - 1);
                    break;
                }
            }
        }

        statusLabel.setText("Расставлено случайно!");
        updateShipVisuals();
        checkReady();
    }

    private boolean canPlace(int row, int col, int size, boolean vertical) {
        if (vertical && row + size > 10) return false;
        if (!vertical && col + size > 10) return false;
        for (int i = 0; i < size; i++) {
            int r = vertical ? row + i : row;
            int c = vertical ? col : col + i;
            if (game.playerField[r][c]) return false;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = r + dr;
                    int nc = c + dc;
                    if (nr >= 0 && nr < 10 && nc >= 0 && nc < 10 && game.playerField[nr][nc]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void placeManual(int row, int col, int size, boolean vertical) {
        Ship ship = new Ship(size);
        ship.isVertical = vertical;
        for (int i = 0; i < size; i++) {
            int r = vertical ? row + i : row;
            int c = vertical ? col : col + i;
            game.playerField[r][c] = true;
            ship.cells.add(new int[]{r, c});
            getCellRect(r, c).setFill(SHIP);
        }
        game.playerShips.add(ship);
    }

    @FXML
    private void onClearBoard() {
        clearBoard();
    }

    private void clearBoard() {
        for (boolean[] row : game.playerField) Arrays.fill(row, false);
        game.playerShips.clear();

        initShipCounts();

        if (selectedShip != null) {
            selectedShip.setScaleX(1.0);
            selectedShip.setScaleY(1.0);
            selectedShip.setStrokeWidth(3);
            selectedShip = null;
            selectedShipType = null;
        }

        buildGrid();
        updateShipVisuals();
        statusLabel.setText("Поле очищено.");
        checkReady();
    }

    private void checkReady() {
        boolean ready = game.playerShips.size() == 10;
        readyButton.setDisable(!ready);
        if (ready) {
            statusLabel.setText("Все корабли расставлены! Нажмите «Готов!»");
        }
    }

    @FXML
    private void onReady() {
        try {
            String difficulty = difficultyCombo.getValue();
            if (difficulty == null) difficulty = "Средний";
            game.difficulty = difficulty;

            out.writeObject(new Message(MessageType.PLACE_SHIPS, game));
            out.flush();
            statusLabel.setText("Ожидаем начала боя...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleship/client/view/game.fxml"));
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1300, 800));
            stage.centerOnScreen();
            stage.setTitle("Морской бой — Битва");

            GameController controller = loader.getController();
            controller.initGame(game, out, in);

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Ошибка перехода в бой");
        }
    }
}