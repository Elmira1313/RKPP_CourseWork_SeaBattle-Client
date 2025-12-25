package com.battleship.client;

import com.battleship.common.Game;
import com.battleship.common.Message;
import com.battleship.common.MessageType;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NetworkManager {

    private static NetworkManager instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Thread listenerThread;
    private volatile boolean listening = false;

    private final Map<MessageType, Consumer<Message>> messageHandlers = new HashMap<>();
    private Consumer<String> onErrorReceived;
    private Runnable onConnected;

    private NetworkManager() {}

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public void registerHandler(MessageType type, Consumer<Message> handler) {
        messageHandlers.put(type, handler);
    }

    public void unregisterHandler(MessageType type) {
        messageHandlers.remove(type);
    }

    public void clearHandlers() {
        messageHandlers.clear();
    }

    public void unregisterHandlers(MessageType... types) {
        for (MessageType type : types) {
            messageHandlers.remove(type);
        }
    }

    private final Map<MessageType, Consumer<Message>> systemHandlers = new HashMap<>();

    public void registerSystemHandler(MessageType type, Consumer<Message> handler) {
        systemHandlers.put(type, handler);
    }

    public void connect(String host, int port) {
        new Thread(() -> {
            try {
                close();

                socket = new Socket(host, port);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                startListening();

                Platform.runLater(() -> {
                    if (onConnected != null) onConnected.run();
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (onErrorReceived != null) {
                        onErrorReceived.accept("Ошибка подключения: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    public void send(Message msg) {
        if (out != null) {
            try {
                out.writeObject(msg);
                out.flush();
                System.out.println("[Клиент] Отправлено: " + msg.getType());
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (onErrorReceived != null) {
                        onErrorReceived.accept("Ошибка отправки: " + e.getMessage());
                    }
                });
            }
        }
    }

    public void send(MessageType type, Object payload) {
        send(new Message(type, payload));
    }

    private void startListening() {
        listening = true;
        listenerThread = new Thread(() -> {
            try {
                while (listening) {
                    Object obj = in.readObject();
                    if (obj instanceof Message msg) {
                        handleIncomingMessage(msg);
                    }
                }
            } catch (EOFException e) {
                System.out.println("[Клиент] Соединение закрыто сервером");
            } catch (Exception e) {
                if (listening) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        if (onErrorReceived != null) {
                            onErrorReceived.accept("Ошибка соединения: " + e.getMessage());
                        }
                    });
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleIncomingMessage(Message msg) {
        Platform.runLater(() -> {
            System.out.println("[Клиент] Получено: " + msg.getType());

            Consumer<Message> systemHandler = systemHandlers.get(msg.getType());
            if (systemHandler != null) {
                systemHandler.accept(msg);
            }

            Consumer<Message> handler = messageHandlers.get(msg.getType());
            if (handler != null) {
                handler.accept(msg);
            } else if (onErrorReceived != null && msg.getType() == MessageType.ERROR) {
                if (msg.getPayload() instanceof String error) {
                    onErrorReceived.accept(error);
                }
            } else {
                System.out.println("[Клиент] Необработанный тип: " + msg.getType());
            }
        });
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void close() {
        listening = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        } finally {
            out = null;
            in = null;
            socket = null;
        }
    }

    public void setOnErrorReceived(Consumer<String> callback) { this.onErrorReceived = callback; }
    public void setOnConnected(Runnable callback) { this.onConnected = callback; }

    public ObjectOutputStream getOutputStream() { return out; }
    public ObjectInputStream getInputStream() { return in; }
    public Socket getSocket() { return socket; }
}