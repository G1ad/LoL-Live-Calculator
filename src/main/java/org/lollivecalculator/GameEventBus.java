package org.lollivecalculator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Observer / Event-Bus pattern.
 * <p>
 * Decouples the data producers (LiveGameListener, network fetchers) from the
 * UI components. Any part of the application can subscribe to events without
 * the publisher needing to know about specific consumers.
 * </p>
 */
public final class GameEventBus {

    // ── Singleton ────────────────────────────────────────────────────────
    private static final GameEventBus INSTANCE = new GameEventBus();

    private final List<Consumer<LiveGameData.Root>> gameDataListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> disconnectListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> statusListeners = new CopyOnWriteArrayList<>();

    private GameEventBus() { }

    public static GameEventBus getInstance() {
        return INSTANCE;
    }

    // ── Game data events ─────────────────────────────────────────────────
    public void subscribeGameData(Consumer<LiveGameData.Root> listener) {
        gameDataListeners.add(listener);
    }

    public void unsubscribeGameData(Consumer<LiveGameData.Root> listener) {
        gameDataListeners.remove(listener);
    }

    public void publishGameData(LiveGameData.Root data) {
        for (Consumer<LiveGameData.Root> l : gameDataListeners) {
            l.accept(data);
        }
    }

    // ── Disconnect (tracking stopped) events ─────────────────────────────
    public void subscribeDisconnect(Runnable listener) {
        disconnectListeners.add(listener);
    }

    public void unsubscribeDisconnect(Runnable listener) {
        disconnectListeners.remove(listener);
    }

    public void publishDisconnect() {
        for (Runnable r : disconnectListeners) {
            r.run();
        }
    }

    // ── Status bar events ────────────────────────────────────────────────
    public void subscribeStatus(Consumer<String> listener) {
        statusListeners.add(listener);
    }

    public void unsubscribeStatus(Consumer<String> listener) {
        statusListeners.remove(listener);
    }

    public void publishStatus(String message) {
        for (Consumer<String> l : statusListeners) {
            l.accept(message);
        }
    }
}