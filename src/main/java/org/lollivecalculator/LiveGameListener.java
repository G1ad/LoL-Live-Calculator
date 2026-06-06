package org.lollivecalculator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LiveGameListener {

    private final LiveClientClient client;
    private final ScheduledExecutorService scheduler;
    private final Consumer<String> onDataReceived;
    private final Consumer<String> onErrorReceived;
    private boolean isRunning;

    public LiveGameListener(Consumer<String> onDataReceived, Consumer<String> onErrorReceived) {
        this.client = new LiveClientClient();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.onDataReceived = onDataReceived;
        this.onErrorReceived = onErrorReceived;
        this.isRunning = false;
    }

    public synchronized void startListening() {
        if (isRunning) return;
        isRunning = true;

        scheduler.scheduleAtFixedRate(this::pollGameClient, 0, 1500, TimeUnit.MILLISECONDS);
    }

    private void pollGameClient() {
        client.fetchLiveGameDataAsync()
                .thenAccept(onDataReceived)
                .exceptionally(ex -> {
                    onErrorReceived.accept(ex.getMessage());
                    return null;
                });
    }

    public synchronized void stopListening() {
        if (!isRunning) return;
        isRunning = false;
        scheduler.shutdown();
    }
}