package org.lollivecalculator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Manages the central dashboard grid that displays enemy player cards and
 * placeholder slots.
 * <p>
 * Separate responsibility: this panel knows how to lay out enemy cards
 * and fill remaining slots with placeholder cards.
 * </p>
 */
public class UIDashboardPanel extends JPanel {

    private static final ThemeConfig T = ThemeConfig.getInstance();

    private final GameDataParser parser;
    private final CalculatorController calculatorController;

    /**
     * Creates the dashboard panel.
     *
     * @param parser              shared game data parser
     * @param calculatorController damage calculation pipeline
     */
    public UIDashboardPanel(GameDataParser parser, CalculatorController calculatorController) {
        this.parser = parser;
        this.calculatorController = calculatorController;

        setLayout(new GridLayout(1, T.ENEMY_CARDS, T.GAP_HORIZONTAL, T.GAP_VERTICAL));
        setBackground(T.BG_DARK);
        setBorder(new EmptyBorder(T.PADDING_SMALL, 20, T.PADDING_SMALL, 20));

        showPlaceholders("Application initialized. Awaiting game connection.");
    }

    /** Updates the dashboard with live enemy data. */
    public void updateEnemies(
            LiveGameData.Root liveData,
            String myTeam,
            LiveGameData.ChampionStats liveStats,
            ChampionData.Champion myStaticChamp) {

        removeAll();

        int enemiesAdded = 0;
        for (LiveGameData.LivePlayer player : liveData.allPlayers) {
            if (!myTeam.equals(player.team)) {
                add(new EnemyCardPanel(
                        player, liveData, liveStats, myStaticChamp, parser, calculatorController));
                enemiesAdded++;
            }
        }

        // Fill remaining slots with placeholders
        while (enemiesAdded < T.ENEMY_CARDS) {
            add(UIComponentFactory.createPlaceholderCard("Awaiting Enemy..."));
            enemiesAdded++;
        }

        revalidate();
        repaint();
    }

    /** Shows placeholder cards with a centered message. */
    public void showPlaceholders(String message) {
        removeAll();
        for (int i = 0; i < T.ENEMY_CARDS; i++) {
            String text = (i == T.ENEMY_CARDS / 2) ? message : "Awaiting Radar...";
            add(UIComponentFactory.createPlaceholderCard(text));
        }
        revalidate();
        repaint();
    }
}