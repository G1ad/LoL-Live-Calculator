package org.lollivecalculator.network;

import org.lollivecalculator.config.AppConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Polls the LoL Live Client API at a configurable interval.
 * <p>
 * Features exponential backoff: if the client is not reachable, the polling
 * interval doubles (from the initial backoff up to the configured max) so
 * that we don't spam the network when the game isn't running. As soon as a
 * request succeeds, the interval resets to the normal polling interval.
 * </p>
 */
public class LiveGameListener {

    private static final AppConfig CFG = AppConfig.getInstance();

    private final LiveClientClient client;
    private final ScheduledExecutorService scheduler;
    private final Consumer<String> onDataReceived;
    private final Consumer<String> onErrorReceived;

    private final AtomicLong currentBackoffMs = new AtomicLong(CFG.getPollBackoffInitialMs());
    private boolean isRunning;

    public LiveGameListener(Consumer<String> onDataReceived, Consumer<String> onErrorReceived) {
        this.client = new LiveClientClient();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LiveGameListener");
            t.setDaemon(true);
            return t;
        });
        this.onDataReceived = onDataReceived;
        this.onErrorReceived = onErrorReceived;
        this.isRunning = false;
    }

    public synchronized void startListening() {
        if (isRunning) return;
        isRunning = true;
        currentBackoffMs.set(CFG.getPollBackoffInitialMs());
        scheduleNext(0); // Poll immediately
    }

    private void scheduleNext(long delayMs) {
        if (!isRunning) return;
        scheduler.schedule(this::pollGameClient, delayMs, TimeUnit.MILLISECONDS);
    }

    private void pollGameClient() {
        if (!isRunning) return;

        client.fetchLiveGameDataAsync()
                .thenAccept(data -> {
                    // Success — reset backoff to normal interval
                    currentBackoffMs.set(CFG.getPollIntervalMs());
                    onDataReceived.accept(data);
                    if (isRunning) scheduleNext(CFG.getPollIntervalMs());
                })
                .exceptionally(ex -> {
                    // Failure — apply exponential backoff
                    long backoff = currentBackoffMs.getAndUpdate(
                            v -> Math.min(v * 2, CFG.getPollBackoffMaxMs()));
                    onErrorReceived.accept(ex.getMessage());
                    if (isRunning) scheduleNext(backoff);
                    return null;
                });
    }

    public synchronized void stopListening() {
        if (!isRunning) return;
        isRunning = false;
        scheduler.shutdownNow();
    }
}