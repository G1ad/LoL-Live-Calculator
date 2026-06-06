package org.lollivecalculator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {

    private final DataDownloader downloader;
    private final GameDataParser parser;
    private LiveGameListener gameListener;

    private final JButton btnDownloadChamps;
    private final JButton btnDownloadItems;
    private final JButton btnToggleLive;
    private final JLabel lblStatus;
    private final JPanel gridPanel;

    private boolean trackingLive = false;

    public MainFrame() {
        downloader = new DataDownloader();
        parser = new GameDataParser();

        setTitle("League of Legends Real-Time Damage Dashboard");
        setSize(1150, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(14, 18, 22));
        setLayout(new BorderLayout(15, 15));

        JPanel panelControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        panelControls.setBackground(new Color(22, 28, 35));

        btnDownloadChamps = createStyledButton("Update Champions", new Color(40, 50, 65));
        btnDownloadItems = createStyledButton("Update Items", new Color(40, 50, 65));
        btnToggleLive = createStyledButton("Start Live Tracking", new Color(40, 167, 69));

        panelControls.add(btnDownloadChamps);
        panelControls.add(btnDownloadItems);
        panelControls.add(btnToggleLive);
        add(panelControls, BorderLayout.NORTH);

        gridPanel = new JPanel(new GridLayout(1, 5, 15, 15));
        gridPanel.setBackground(new Color(14, 18, 22));
        gridPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        showPlaceholderCards("Application initialized. Awaiting game connection.");
        add(gridPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(10, 12, 16));
        statusPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        lblStatus = new JLabel("System Ready.");
        lblStatus.setForeground(new Color(140, 155, 170));
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusPanel.add(lblStatus, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);

        setupEventHandlers();
        tryAutoLoadLocalData();
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.brighter(), 1),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showPlaceholderCards(String message) {
        gridPanel.removeAll();
        for (int i = 0; i < 5; i++) {
            JPanel placeholder = new JPanel(new GridBagLayout());
            placeholder.setBackground(new Color(22, 28, 35));
            placeholder.setBorder(BorderFactory.createLineBorder(new Color(35, 45, 55), 1));
            JLabel lbl = new JLabel(i == 2 ? message : "Awaiting Radar...");
            lbl.setForeground(new Color(90, 105, 120));
            lbl.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            placeholder.add(lbl);
            gridPanel.add(placeholder);
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void tryAutoLoadLocalData() {
        java.nio.file.Path champPath = java.nio.file.Paths.get("champions.json");
        if (java.nio.file.Files.exists(champPath)) {
            try {
                parser.loadChampionsData(champPath.toString());
                setStatus("Auto-loaded data for " + parser.getLoadedChampionsCount() + " champions.");
            } catch (Exception e) {
                setStatus("Failed to auto-load champions: " + e.getMessage());
            }
        }
        java.nio.file.Path itemPath = java.nio.file.Paths.get("items.json");
        if (java.nio.file.Files.exists(itemPath)) {
            try {
                parser.loadItemsData(itemPath.toString());
            } catch (Exception ignored) {}
        }
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> lblStatus.setText(text));
    }

    private void updateDashboard(LiveGameData.Root liveData, String activeChampName, LiveGameData.ChampionStats liveStats, ChampionData.Champion staticChamp) {
        SwingUtilities.invokeLater(() -> {
            gridPanel.removeAll();

            String myTeam = null;
            String myRiotId = liveData.activePlayer.riotId;
            String myName = liveData.activePlayer.summonerName;

            for (LiveGameData.LivePlayer p : liveData.allPlayers) {
                if ((myRiotId != null && myRiotId.equals(p.riotId)) || (myName != null && myName.equals(p.summonerName))) {
                    myTeam = p.team;
                    break;
                }
            }

            if (myTeam == null) return;

            int enemiesAdded = 0;
            for (LiveGameData.LivePlayer player : liveData.allPlayers) {
                if (!myTeam.equals(player.team)) {
                    gridPanel.add(new EnemyCardPanel(player, liveData, liveStats, staticChamp, parser));
                    enemiesAdded++;
                }
            }

            while (enemiesAdded < 5) {
                JPanel emptyCard = new JPanel(new GridBagLayout());
                emptyCard.setBackground(new Color(22, 28, 35));
                emptyCard.setBorder(BorderFactory.createLineBorder(new Color(35, 45, 55), 1));
                JLabel lbl = new JLabel("Awaiting Enemy...");
                lbl.setForeground(new Color(90, 105, 120));
                emptyCard.add(lbl);
                gridPanel.add(emptyCard);
                enemiesAdded++;
            }

            setStatus(String.format("Live Link Active | Playing: %s | Stats: AD=%.1f AP=%.1f",
                    activeChampName, liveStats.attackDamage, liveStats.abilityPower));
            gridPanel.revalidate();
            gridPanel.repaint();
        });
    }

    private void setupEventHandlers() {
        btnDownloadChamps.addActionListener(e -> {
            setStatus("Downloading champions framework updates...");
            downloader.downloadChampionsAsync().thenAccept(path -> {
                try {
                    parser.loadChampionsData(path.toString());
                    setStatus("Successfully loaded " + parser.getLoadedChampionsCount() + " champions.");
                } catch (Exception ex) {
                    setStatus("Parsing failed: " + ex.getMessage());
                }
            });
        });

        btnDownloadItems.addActionListener(e -> {
            setStatus("Downloading weapons data matrices...");
            downloader.downloadItemsAsync().thenAccept(path -> {
                try {
                    parser.loadItemsData(path.toString());
                    setStatus("Successfully indexed global game armory files.");
                } catch (Exception ex) {
                    setStatus("Parsing failed: " + ex.getMessage());
                }
            });
        });

        btnToggleLive.addActionListener(e -> {
            if (!trackingLive) {
                setStatus("Opening tactical loop socket...");
                gameListener = new LiveGameListener(
                        jsonPayload -> {
                            try {
                                LiveGameData.Root liveData = parser.parseLiveGameData(jsonPayload);
                                if (liveData != null && liveData.activePlayer != null && liveData.allPlayers != null) {
                                    String activeChampName = null;
                                    String myRiotId = liveData.activePlayer.riotId;
                                    String myName = liveData.activePlayer.summonerName;

                                    for (LiveGameData.LivePlayer p : liveData.allPlayers) {
                                        if ((myRiotId != null && myRiotId.equals(p.riotId)) || (myName != null && myName.equals(p.summonerName))) {
                                            activeChampName = p.championName;
                                            break;
                                        }
                                    }

                                    if (activeChampName == null) return;

                                    LiveGameData.ChampionStats liveStats = liveData.activePlayer.championStats;
                                    ChampionData.Champion staticChamp = parser.getChampion(activeChampName);

                                    if (liveStats != null && staticChamp != null) {
                                        updateDashboard(liveData, activeChampName, liveStats, staticChamp);
                                    }
                                }
                            } catch (Exception parseEx) {
                                setStatus("Dashboard calculation gap: " + parseEx.getMessage());
                            }
                        },
                        errorMsg -> {
                            showPlaceholderCards("Searching for active match connection...");
                            setStatus("Searching for active match connection...");
                        }
                );
                gameListener.startListening();
                btnToggleLive.setText("Disconnect Radar");
                btnToggleLive.setBackground(new Color(220, 53, 69));
                trackingLive = true;
            } else {
                gameListener.stopListening();
                btnToggleLive.setText("Start Live Tracking");
                btnToggleLive.setBackground(new Color(40, 167, 69));
                showPlaceholderCards("Radar offline.");
                setStatus("Live tracking suspended.");
                trackingLive = false;
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainFrame().setVisible(true);
        });
    }
}