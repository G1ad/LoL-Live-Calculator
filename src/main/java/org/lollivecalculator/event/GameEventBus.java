package org.lollivecalculator.event;

import org.lollivecalculator.model.LiveGameData;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Observer / Event-Bus pattern.
 * <p>
 * Decouples the data producers (LiveGameListener, network fetchers) from the
 * UI components. Uses strong references (not WeakReference) to prevent
 * listeners from being garbage collected immediately after subscription.
 * </p>
 */
public final class GameEventBus {

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

    // ── Disconnect events ────────────────────────────────────────────────

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