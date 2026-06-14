package org.lollivecalculator.ui;

import org.lollivecalculator.config.AppConfig;
import org.lollivecalculator.config.ThemeConfig;
import org.lollivecalculator.event.GameEventBus;
import org.lollivecalculator.model.LiveGameData;
import org.lollivecalculator.network.DataDownloader;
import org.lollivecalculator.network.LiveGameListener;
import org.lollivecalculator.service.CalculatorController;
import org.lollivecalculator.service.GameDataParser;
import org.lollivecalculator.service.GameStateManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainFrame extends JFrame {

    private static final ThemeConfig T = ThemeConfig.getInstance();
    private static final AppConfig CFG = AppConfig.getInstance();

    private final DataDownloader downloader;
    private final GameDataParser parser;
    private final CalculatorController calculatorController;
    private final GameStateManager gameStateManager;

    private JButton btnDownloadChamps;
    private JButton btnDownloadItems;
    private JButton btnToggleLive;
    private JButton btnLoadExample;
    private JLabel lblStatus;
    private UIDashboardPanel dashboard;

    private LiveGameListener gameListener;
    private boolean trackingLive = false;

    public MainFrame() {
        this.downloader = new DataDownloader();
        this.parser = new GameDataParser();
        this.calculatorController = new CalculatorController();
        this.gameStateManager = new GameStateManager();

        System.out.println("══════════════════════════════════════════════");
        System.out.println(" LoL Live Calculator  v1.0");
        System.out.println("══════════════════════════════════════════════");
        System.out.println(" Working directory: " + Paths.get(".").toAbsolutePath().normalize());

        setTitle("League of Legends Real-Time Damage Dashboard");
        setSize(T.FRAME_WIDTH, T.FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(T.BG_DARK);
        setLayout(new BorderLayout(T.GAP_HORIZONTAL, T.GAP_VERTICAL));

        // ── Toolbar ──────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, T.PADDING_TOOLBAR));
        toolbar.setBackground(T.BG_TOOLBAR);

        this.btnDownloadChamps = UIComponentFactory.createButton("Update Champions");
        this.btnDownloadItems  = UIComponentFactory.createButton("Update Items");
        this.btnToggleLive     = UIComponentFactory.createToggleLiveButton();
        this.btnLoadExample    = UIComponentFactory.createButton("Load Example Data", new Color(70, 100, 150));

        toolbar.add(btnDownloadChamps);
        toolbar.add(btnDownloadItems);
        toolbar.add(btnToggleLive);
        toolbar.add(btnLoadExample);
        add(toolbar, BorderLayout.NORTH);

        // ── Dashboard ────────────────────────────────────────────────────
        this.dashboard = new UIDashboardPanel(parser, calculatorController);
        add(dashboard, BorderLayout.CENTER);

        // ── Status bar ───────────────────────────────────────────────────
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(T.BG_STATUS_BAR);
        statusPanel.setBorder(new EmptyBorder(T.PADDING_SMALL, 20, T.PADDING_SMALL, 20));

        this.lblStatus = UIComponentFactory.createStatusLabel("Initializing...");
        statusPanel.add(lblStatus, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);

        // ── Wire events ──────────────────────────────────────────────────
        wireEventHandlers();
        subscribeToEventBus();

        // ── Load data ────────────────────────────────────────────────────
        tryAutoLoadData();
    }

    private void wireEventHandlers() {
        btnDownloadChamps.addActionListener(e -> downloadChampions());
        btnDownloadItems.addActionListener(e -> downloadItems());
        btnToggleLive.addActionListener(e -> toggleLiveTracking());
        btnLoadExample.addActionListener(e -> loadExampleData());
    }

    private void subscribeToEventBus() {
        GameEventBus bus = GameEventBus.getInstance();
        bus.subscribeGameData(this::onGameDataReceived);
        bus.subscribeDisconnect(this::onTrackingDisconnected);
        bus.subscribeStatus(this::onStatusChanged);
    }

    // ── Live data pipeline ───────────────────────────────────────────────

    private void onGameDataReceived(LiveGameData.Root liveData) {
        if (!gameStateManager.hasValidSession()) return;

        SwingUtilities.invokeLater(() -> {
            dashboard.updateEnemies(
                    gameStateManager.getCurrentData(),
                    gameStateManager.getMyTeam(),
                    gameStateManager.getLiveStats(),
                    gameStateManager.getMyStaticChampion()
            );

            String msg = "Live | Playing: " + gameStateManager.getActiveChampionName()
                    + " | AD=" + (int) gameStateManager.getLiveStats().attackDamage
                    + " AP=" + (int) gameStateManager.getLiveStats().abilityPower;
            GameEventBus.getInstance().publishStatus(msg);
        });
    }

    private void onTrackingDisconnected() {
        SwingUtilities.invokeLater(() -> {
            dashboard.showPlaceholders("Disconnected — retrying...");
            setStatus("Disconnected — retrying...");
        });
    }

    // ── Load Example Data (no live game needed) ──────────────────────────

    private void loadExampleData() {
        setStatus("Loading example data...");

        Path[] locations = {
            Path.of("live_game_example.txt"),
            Path.of("src/test/resources/live_game_example.json"),
            Path.of("test-data/live_game_example.json")
        };

        Path found = null;
        for (Path p : locations) {
            System.out.println("   Looking for: " + p.toAbsolutePath().normalize() + " exists=" + Files.exists(p));
            if (Files.exists(p)) { found = p; break; }
        }

        if (found == null) {
            setStatus("File not found: live_game_example.txt — check working directory");
            System.out.println(" [ERROR] Example data file not found!");
            System.out.println("   Tried: " + locations[0].toAbsolutePath().normalize());
            System.out.println("   Tried: " + locations[1].toAbsolutePath().normalize());
            return;
        }

        try {
            String json = Files.readString(found, StandardCharsets.UTF_8);
            LiveGameData.Root data = parser.parseLiveGameData(json);
            if (data == null) { setStatus("Failed to parse example JSON"); return; }

            if (gameStateManager.processGameData(data, parser)) {
                System.out.println(" ✅ Example data loaded: " + gameStateManager.getActiveChampionName()
                    + " vs " + gameStateManager.getLiveStats().attackDamage + " AD");
                GameEventBus.getInstance().publishGameData(data);
            } else {
                setStatus("Champion data not loaded — click 'Update Champions' first");
            }
        } catch (Exception ex) {
            setStatus("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ── Download ─────────────────────────────────────────────────────────

    private void downloadChampions() {
        setStatus("Downloading champions...");
        downloader.downloadChampionsAsync().thenAccept(path -> {
            try {
                parser.loadChampionsData(path);
                setStatus("Loaded " + parser.getLoadedChampionsCount() + " champions");
            } catch (Exception ex) {
                setStatus("Parse error: " + ex.getMessage());
            }
        });
    }

    private void downloadItems() {
        setStatus("Downloading items...");
        downloader.downloadItemsAsync().thenAccept(path -> {
            try {
                parser.loadItemsData(path);
                setStatus("Loaded " + parser.getLoadedItemsCount() + " items");
            } catch (Exception ex) {
                setStatus("Parse error: " + ex.getMessage());
            }
        });
    }

    // ── Live tracking ────────────────────────────────────────────────────

    private void toggleLiveTracking() {
        if (!trackingLive) startLiveTracking();
        else stopLiveTracking();
    }

    private void startLiveTracking() {
        // Ensure data is loaded first
        if (parser.getLoadedChampionsCount() == 0) {
            setStatus("No champion data — click 'Update Champions' first");
            return;
        }

        setStatus("Connecting to game client at " + CFG.getLolApiUrl() + " ...");

        gameListener = new LiveGameListener(
                json -> {
                    try {
                        LiveGameData.Root data = parser.parseLiveGameData(json);
                        if (data != null && gameStateManager.processGameData(data, parser)) {
                            GameEventBus.getInstance().publishGameData(data);
                        }
                    } catch (Exception ex) {
                        setStatus("Parse error: " + ex.getMessage());
                    }
                },
                err -> GameEventBus.getInstance().publishDisconnect()
        );

        gameListener.startListening();
        btnToggleLive.setText("Disconnect");
        btnToggleLive.setBackground(T.RED_DANGER);
        trackingLive = true;
    }

    private void stopLiveTracking() {
        gameListener.stopListening();
        gameStateManager.reset();

        btnToggleLive.setText("Start Live Tracking");
        btnToggleLive.setBackground(T.GREEN_ACTIVE);
        trackingLive = false;
        dashboard.showPlaceholders("Tracking stopped");
        setStatus("Live tracking stopped");
    }

    private void onStatusChanged(String text) {
        SwingUtilities.invokeLater(() -> lblStatus.setText(text));
    }

    private void setStatus(String text) {
        System.out.println(" [STATUS] " + text);
        SwingUtilities.invokeLater(() -> lblStatus.setText(text));
    }

    // ── Auto-load champions.json and items.json on startup ───────────────
    //     Checks multiple locations in order, prints what it found

    private void tryAutoLoadData() {
        boolean loaded = tryLoadChampions();

        if (!loaded) {
            setStatus("No data found — click 'Update Champions' or 'Load Example Data'");
            System.out.println(" ⚠️  Champions data could not be loaded automatically.");
        }

        // Items are optional
        tryLoadItems();
    }

    private boolean tryLoadChampions() {
        Path[] paths = {
            CFG.getChampionsLocalPath(),          // data/champions.json
            Path.of("data", "champions.json"),    // explicit data/champions.json
            Path.of("champions.json")             // root
        };

        for (Path p : paths) {
            if (Files.exists(p)) {
                try {
                    parser.loadChampionsData(p);
                    String msg = "Loaded " + parser.getLoadedChampionsCount() + " champions from " + p.getFileName();
                    setStatus(msg);
                    System.out.println(" ✅ " + msg);
                    System.out.println("   Path: " + p.toAbsolutePath().normalize());
                    return true;
                } catch (Exception e) {
                    System.out.println(" ⚠️  Found " + p.getFileName() + " but failed to parse: " + e.getMessage());
                }
            } else {
                System.out.println("   Not found: " + p.toAbsolutePath().normalize());
            }
        }
        return false;
    }

    private void tryLoadItems() {
        Path[] paths = {
            CFG.getItemsLocalPath(),          // data/items.json
            Path.of("data", "items.json"),    // explicit data/items.json
            Path.of("items.json")             // root
        };

        for (Path p : paths) {
            if (Files.exists(p)) {
                try {
                    parser.loadItemsData(p);
                    System.out.println(" ✅ Loaded " + parser.getLoadedItemsCount() + " items from " + p.getFileName());
                    return;
                } catch (Exception ignored) { }
            }
        }
    }
}