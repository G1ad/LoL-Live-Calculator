package org.lollivecalculator.ui;

import org.lollivecalculator.config.ThemeConfig;
import org.lollivecalculator.event.GameEventBus;
import org.lollivecalculator.model.ChampionData;
import org.lollivecalculator.model.LiveGameData;
import org.lollivecalculator.network.DataDownloader;
import org.lollivecalculator.network.LiveGameListener;
import org.lollivecalculator.service.CalculatorController;
import org.lollivecalculator.service.GameDataParser;
import org.lollivecalculator.service.GameStateManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main application window.
 * <p>
 * Thin orchestrator that wires together all components using:
 * <ul>
 *   <li>Singleton (ThemeConfig) – centralized styling</li>
 *   <li>Factory (UIComponentFactory) – consistent component creation</li>
 *   <li>Observer (GameEventBus) – decoupled data→UI communication</li>
 *   <li>State Manager (GameStateManager) – session player identity</li>
 *   <li>Separated Panels (UIDashboardPanel) – focused responsibilities</li>
 * </ul>
 */
public class MainFrame extends JFrame {

    private static final ThemeConfig T = ThemeConfig.getInstance();

    private final DataDownloader downloader;
    private final GameDataParser parser;
    private final CalculatorController calculatorController;
    private final GameStateManager gameStateManager;

    private final JButton btnDownloadChamps;
    private final JButton btnDownloadItems;
    private final JButton btnToggleLive;
    private final JLabel lblStatus;
    private final UIDashboardPanel dashboard;

    private LiveGameListener gameListener;
    private boolean trackingLive = false;

    public MainFrame() {
        this.downloader = new DataDownloader();
        this.parser = new GameDataParser();
        this.calculatorController = new CalculatorController();
        this.gameStateManager = new GameStateManager();

        setTitle("League of Legends Real-Time Damage Dashboard");
        setSize(T.FRAME_WIDTH, T.FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(T.BG_DARK);
        setLayout(new BorderLayout(T.GAP_HORIZONTAL, T.GAP_VERTICAL));

        JPanel toolbar = buildToolbar();
        add(toolbar, BorderLayout.NORTH);

        this.dashboard = new UIDashboardPanel(parser, calculatorController);
        add(dashboard, BorderLayout.CENTER);

        JPanel statusPanel = buildStatusBar();
        add(statusPanel, BorderLayout.SOUTH);

        wireEventHandlers();
        subscribeToEventBus();

        tryAutoLoadLocalData();
    }

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

    private JPanel buildStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(T.BG_STATUS_BAR);
        statusPanel.setBorder(new EmptyBorder(T.PADDING_SMALL, 20, T.PADDING_SMALL, 20));

        lblStatus = UIComponentFactory.createStatusLabel("System Ready.");
        statusPanel.add(lblStatus, BorderLayout.WEST);
        return statusPanel;
    }

    private void wireEventHandlers() {
        btnDownloadChamps.addActionListener(e -> downloadChampions());
        btnDownloadItems.addActionListener(e -> downloadItems());
        btnToggleLive.addActionListener(e -> toggleLiveTracking());
    }

    private void subscribeToEventBus() {
        GameEventBus bus = GameEventBus.getInstance();
        bus.subscribeGameData(this::onGameDataReceived);
        bus.subscribeDisconnect(this::onTrackingDisconnected);
        bus.subscribeStatus(this::setStatus);
    }

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
            } catch (Exception ignored) { }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            new MainFrame().setVisible(true);
        });
    }
}