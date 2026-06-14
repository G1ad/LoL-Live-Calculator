package org.lollivecalculator.event;

import org.lollivecalculator.model.LiveGameData;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Observer / Event-Bus pattern.
 * <p>
 * Decouples the data producers (LiveGameListener, network fetchers) from the
 * UI components. Uses {@link WeakReference} wrappers so that dynamically added
 * UI panels can be garbage collected without requiring explicit unsubscription.
 * </p>
 */
public final class GameEventBus {

    private static final GameEventBus INSTANCE = new GameEventBus();

    private final List<WeakReference<Consumer<LiveGameData.Root>>> gameDataListeners = new CopyOnWriteArrayList<>();
    private final List<WeakReference<Runnable>> disconnectListeners = new CopyOnWriteArrayList<>();
    private final List<WeakReference<Consumer<String>>> statusListeners = new CopyOnWriteArrayList<>();

    private GameEventBus() { }

    public static GameEventBus getInstance() {
        return INSTANCE;
    }

    // ── Game data events ─────────────────────────────────────────────────

    public void subscribeGameData(Consumer<LiveGameData.Root> listener) {
        gameDataListeners.add(new WeakReference<>(listener));
    }

    public void unsubscribeGameData(Consumer<LiveGameData.Root> listener) {
        gameDataListeners.removeIf(ref -> {
            Consumer<LiveGameData.Root> l = ref.get();
            return l == null || l == listener;
        });
    }

    public void publishGameData(LiveGameData.Root data) {
        gameDataListeners.removeIf(ref -> ref.get() == null);
        for (WeakReference<Consumer<LiveGameData.Root>> ref : gameDataListeners) {
            Consumer<LiveGameData.Root> l = ref.get();
            if (l != null) l.accept(data);
        }
    }

    // ── Disconnect events ────────────────────────────────────────────────

    public void subscribeDisconnect(Runnable listener) {
        disconnectListeners.add(new WeakReference<>(listener));
    }

    public void unsubscribeDisconnect(Runnable listener) {
        disconnectListeners.removeIf(ref -> {
            Runnable r = ref.get();
            return r == null || r == listener;
        });
    }

    public void publishDisconnect() {
        disconnectListeners.removeIf(ref -> ref.get() == null);
        for (WeakReference<Runnable> ref : disconnectListeners) {
            Runnable r = ref.get();
            if (r != null) r.run();
        }
    }

    // ── Status bar events ────────────────────────────────────────────────

    public void subscribeStatus(Consumer<String> listener) {
        statusListeners.add(new WeakReference<>(listener));
    }

    public void unsubscribeStatus(Consumer<String> listener) {
        statusListeners.removeIf(ref -> {
            Consumer<String> l = ref.get();
            return l == null || l == listener;
        });
    }

    public void publishStatus(String message) {
        statusListeners.removeIf(ref -> ref.get() == null);
        for (WeakReference<Consumer<String>> ref : statusListeners) {
            Consumer<String> l = ref.get();
            if (l != null) l.accept(message);
        }
    }
}