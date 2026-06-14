package org.lollivecalculator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main application window.
 * <p>
 * Acts as a thin orchestrator that wires together the application's
 * components using established design patterns:
 * <ul>
 *   <li><b>Singleton ({@link ThemeConfig})</b> – centralized styling constants</li>
 *   <li><b>Factory ({@link UIComponentFactory})</b> – consistent component creation</li>
 *   <li><b>Observer ({@link GameEventBus})</b> – decoupled data→UI communication</li>
 *   <li><b>State Manager ({@link GameStateManager})</b> – session player identity</li>
 *   <li><b>Separated Panels ({@link UIDashboardPanel})</b> – focused responsibilities</li>
 * </ul>
 * </p>
 */
public class MainFrame extends JFrame {

    private static final ThemeConfig T = ThemeConfig.getInstance();

    // ── Core dependencies ────────────────────────────────────────────────
    private final DataDownloader downloader;
    private final GameDataParser parser;
    private final CalculatorController calculatorController;
    private final GameStateManager gameStateManager;

    // ── UI components (created via factories, owned by this frame) ───────
    private final JButton btnDownloadChamps;
    private final JButton btnDownloadItems;
    private final JButton btnToggleLive;
    private final JLabel lblStatus;
    private final UIDashboardPanel dashboard;

    // ── State ────────────────────────────────────────────────────────────
    private LiveGameListener gameListener;
    private boolean trackingLive = false;

    // ── Construction ─────────────────────────────────────────────────────

    public MainFrame() {
        // Domain dependencies
        this.downloader = new DataDownloader();
        this.parser = new GameDataParser();
        this.calculatorController = new CalculatorController();
        this.gameStateManager = new GameStateManager();

        // ── Frame setup ──────────────────────────────────────────────────
        setTitle("League of Legends Real-Time Damage Dashboard");
        setSize(T.FRAME_WIDTH, T.FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(T.BG_DARK);
        setLayout(new BorderLayout(T.GAP_HORIZONTAL, T.GAP_VERTICAL));

        // ── Top toolbar ──────────────────────────────────────────────────
        JPanel toolbar = buildToolbar();
        add(toolbar, BorderLayout.NORTH);

        // ── Center dashboard ─────────────────────────────────────────────
        this.dashboard = new UIDashboardPanel(parser, calculatorController);
        add(dashboard, BorderLayout.CENTER);

        // ── Status bar ───────────────────────────────────────────────────
        JPanel statusPanel = buildStatusBar();
        add(statusPanel, BorderLayout.SOUTH);

        // ── Wire events ──────────────────────────────────────────────────
        wireEventHandlers();
        subscribeToEventBus();

        // ── Initialization ───────────────────────────────────────────────
        tryAutoLoadLocalData();
    }

    // ── Toolbar builder ──────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, T.PADDING_TOOLBAR));
        panel.setBackground(T.BG_TOOLBAR);

        btnDownloadChamps = UIComponentFactory.createButton("Update Champions");
        btnDownloadItems  = UIComponentFactory.createButton("Update Items");
        btnToggleLive     = UIComponentFactory.createToggleLiveButton();

        panel.add(btnDownloadChamps);
        panel.add(btnDownloadItems);
        panel.add(btnToggleLive);
        return panel;
    }

    // ── Status bar builder ───────────────────────────────────────────────

    private JPanel buildStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(T.BG_STATUS_BAR);
        statusPanel.setBorder(new EmptyBorder(T.PADDING_SMALL, 20, T.PADDING_SMALL, 20));

        lblStatus = UIComponentFactory.createStatusLabel("System Ready.");
        statusPanel.add(lblStatus, BorderLayout.WEST);
        return statusPanel;
    }

    // ── Event wiring: UI controls ────────────────────────────────────────

    private void wireEventHandlers() {
        btnDownloadChamps.addActionListener(e -> downloadChampions());
        btnDownloadItems.addActionListener(e -> downloadItems());
        btnToggleLive.addActionListener(e -> toggleLiveTracking());
    }

    private void subscribeToEventBus() {
        GameEventBus bus = GameEventBus.getInstance();

        // React to new game data arriving
        bus.subscribeGameData(this::onGameDataReceived);

        // React to tracking disconnection
        bus.subscribeDisconnect(this::onTrackingDisconnected);

        // Update status bar from any publisher
        bus.subscribeStatus(this::setStatus);
    }

    // ── Event handlers ───────────────────────────────────────────────────

    private void onGameDataReceived(LiveGameData.Root liveData) {
        if (!gameStateManager.hasValidSession()) return;

        SwingUtilities.invokeLater(() -> {
            dashboard.updateEnemies(
                    gameStateManager.getCurrentData(),
                    gameStateManager.getMyTeam(),
                    gameStateManager.getLiveStats(),
                    gameStateManager.getMyStaticChampion()
            );

            GameEventBus.getInstance().publishStatus(
                    String.format("Live Link Active | Playing: %s | Stats: AD=%.1f AP=%.1f",
                            gameStateManager.getActiveChampionName(),
                            gameStateManager.getLiveStats().attackDamage,
                            gameStateManager.getLiveStats().abilityPower));
        });
    }

    private void onTrackingDisconnected() {
        SwingUtilities.invokeLater(() -> {
            dashboard.showPlaceholders("Searching for active match connection...");
            setStatus("Searching for active match connection...");
        });
    }

    // ── Download operations ──────────────────────────────────────────────

    private void downloadChampions() {
        setStatus("Downloading champions framework updates...");
        downloader.downloadChampionsAsync().thenAccept(path -> {
            try {
                parser.loadChampionsData(path.toString());
                setStatus("Successfully loaded " + parser.getLoadedChampionsCount() + " champions.");
            } catch (Exception ex) {
                setStatus("Parsing failed: " + ex.getMessage());
            }
        });
    }

    private void downloadItems() {
        setStatus("Downloading weapons data matrices...");
        downloader.downloadItemsAsync().thenAccept(path -> {
            try {
                parser.loadItemsData(path.toString());
                setStatus("Successfully indexed global game armory files.");
            } catch (Exception ex) {
                setStatus("Parsing failed: " + ex.getMessage());
            }
        });
    }

    // ── Live tracking toggle ─────────────────────────────────────────────

    private void toggleLiveTracking() {
        if (!trackingLive) {
            startLiveTracking();
        } else {
            stopLiveTracking();
        }
    }

    private void startLiveTracking() {
        setStatus("Opening tactical loop socket...");

        gameListener = new LiveGameListener(
                // onDataReceived
                jsonPayload -> {
                    try {
                        LiveGameData.Root liveData = parser.parseLiveGameData(jsonPayload);
                        if (liveData != null && gameStateManager.processGameData(liveData, parser)) {
                            GameEventBus.getInstance().publishGameData(liveData);
                        }
                    } catch (Exception ex) {
                        setStatus("Dashboard calculation gap: " + ex.getMessage());
                    }
                },
                // onErrorReceived
                errorMsg -> GameEventBus.getInstance().publishDisconnect()
        );

        gameListener.startListening();

        btnToggleLive.setText("Disconnect Radar");
        btnToggleLive.setBackground(T.RED_DANGER);
        trackingLive = true;
    }

    private void stopLiveTracking() {
        gameListener.stopListening();

        btnToggleLive.setText("Start Live Tracking");
        btnToggleLive.setBackground(T.GREEN_ACTIVE);
        trackingLive = false;

        dashboard.showPlaceholders("Radar offline.");
        setStatus("Live tracking suspended.");
    }

    // ── Utilities ────────────────────────────────────────────────────────

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> lblStatus.setText(text));
    }

    private void tryAutoLoadLocalData() {
        Path champPath = Paths.get("champions.json");
        if (Files.exists(champPath)) {
            try {
                parser.loadChampionsData(champPath.toString());
                setStatus("Auto-loaded data for " + parser.getLoadedChampionsCount() + " champions.");
            } catch (Exception e) {
                setStatus("Failed to auto-load champions: " + e.getMessage());
            }
        }
        Path itemPath = Paths.get("items.json");
        if (Files.exists(itemPath)) {
            try {
                parser.loadItemsData(itemPath.toString());
            } catch (Exception ignored) { /* optional, will load later */ }
        }
    }

    // ── Entry point ──────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { /* use default */ }
            new MainFrame().setVisible(true);
        });
    }
}