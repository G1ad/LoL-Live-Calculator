package org.lollivecalculator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class EnemyCardPanel extends JPanel {

    public EnemyCardPanel(LiveGameData.LivePlayer enemy, LiveGameData.Root liveData,
                          LiveGameData.ChampionStats liveStats, ChampionData.Champion myStaticChamp,
                          GameDataParser parser, CalculatorController controller) {

        setBackground(new Color(22, 28, 35));
        setBorder(BorderFactory.createLineBorder(new Color(35, 45, 55), 1));
        setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        headerPanel.setBackground(new Color(30, 38, 48));
        headerPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel lblName = new JLabel(enemy.championName);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblName.setForeground(new Color(240, 244, 248));

        JLabel lblDetails = new JLabel(String.format("Level %d  |  %s Team", enemy.level, enemy.team));
        lblDetails.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDetails.setForeground(new Color(140, 155, 170));

        headerPanel.add(lblName);
        headerPanel.add(lblDetails);
        add(headerPanel, BorderLayout.NORTH);

        JPanel bodyPanel = new JPanel();
        bodyPanel.setBackground(new Color(22, 28, 35));
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBorder(new EmptyBorder(15, 12, 15, 12));

        List<CalculatorController.CalculatedAbilityResult> calculations =
                controller.computeEnemyMitigationPipeline(enemy, liveData, liveStats, myStaticChamp, parser);

        double baseArmor = 30.0;
        double baseMr = 30.0;
        ChampionData.Champion enemyStatic = parser.getChampion(enemy.championName);
        if (enemyStatic != null && enemyStatic.stats != null) {
            int n = enemy.level;
            if (enemyStatic.stats.containsKey("armor")) {
                baseArmor = enemyStatic.stats.get("armor").flat + (enemyStatic.stats.get("armor").perLevel * (n - 1) * (0.7025 + 0.0175 * (n - 1)));
            }
            if (enemyStatic.stats.containsKey("magicResistance")) {
                baseMr = enemyStatic.stats.get("magicResistance").flat + (enemyStatic.stats.get("magicResistance").perLevel * (n - 1) * (0.7025 + 0.0175 * (n - 1)));
            }
        }

        double bonusArmorFromItems = 0.0;
        double bonusMrFromItems = 0.0;
        if (enemy.items != null) {
            for (LiveGameData.LiveItem liveItem : enemy.items) {
                ItemData.Item staticItem = parser.getItem(liveItem.itemID);
                if (staticItem != null && staticItem.stats != null) {
                    if (staticItem.stats.containsKey("armor")) bonusArmorFromItems += staticItem.stats.get("armor").flat * liveItem.count;
                    if (staticItem.stats.containsKey("magicResistance")) bonusMrFromItems += staticItem.stats.get("magicResistance").flat * liveItem.count;
                }
            }
        }

        double totalEnemyArmor = baseArmor + bonusArmorFromItems;
        double totalEnemyMr = baseMr + bonusMrFromItems;
        int myLevel = liveData.activePlayer != null ? liveData.activePlayer.level : 1;
        double effectiveArmor = DamageEngine.calculateEffectiveResistance(totalEnemyArmor, "PHYSICAL", liveStats);
        double effectiveMr = DamageEngine.calculateEffectiveResistance(totalEnemyMr, "MAGIC", liveStats);

        bodyPanel.add(createStatLine("True Enemy Armor:", String.format("%.1f (Eff: %.1f)", totalEnemyArmor, effectiveArmor)));
        bodyPanel.add(createStatLine("True Enemy MR:", String.format("%.1f (Eff: %.1f)", totalEnemyMr, effectiveMr)));
        bodyPanel.add(Box.createVerticalStrut(15));

        JPanel scrollableAbilityContainer = new JPanel();
        scrollableAbilityContainer.setBackground(new Color(22, 28, 35));
        scrollableAbilityContainer.setLayout(new BoxLayout(scrollableAbilityContainer, BoxLayout.Y_AXIS));

        for (CalculatorController.CalculatedAbilityResult res : calculations) {
            scrollableAbilityContainer.add(createAbilityRow(res.slot, res.name, res.rawDamage, res.mitigatedDamage));
            scrollableAbilityContainer.add(Box.createVerticalStrut(10));
        }

        JScrollPane rowScrollPane = new JScrollPane(scrollableAbilityContainer);
        rowScrollPane.setBorder(null);
        rowScrollPane.setOpaque(false);
        rowScrollPane.getViewport().setOpaque(false);
        rowScrollPane.getVerticalScrollBar().setUnitIncrement(12);

        add(bodyPanel, BorderLayout.CENTER);
        bodyPanel.add(rowScrollPane, BorderLayout.CENTER);
    }

    private JPanel createStatLine(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(22, 28, 35));
        panel.setMaximumSize(new Dimension(300, 20));

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblLabel.setForeground(new Color(140, 155, 170));

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblVal.setForeground(new Color(255, 193, 7));

        panel.add(lblLabel, BorderLayout.WEST);
        panel.add(lblVal, BorderLayout.EAST);
        return panel;
    }

    private JPanel createAbilityRow(String slot, String name, double rawDmg, double realDmg) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        panel.setBackground(new Color(22, 28, 35));
        panel.setMaximumSize(new Dimension(300, 48));

        JLabel lblSlot = new JLabel(slot + " ");
        lblSlot.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSlot.setForeground(new Color(255, 193, 7));

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblName.setForeground(new Color(160, 175, 190));

        JPanel labelSubPanel = new JPanel(new BorderLayout());
        labelSubPanel.setBackground(new Color(22, 28, 35));
        labelSubPanel.add(lblSlot, BorderLayout.WEST);
        labelSubPanel.add(lblName, BorderLayout.CENTER);
        panel.add(labelSubPanel, BorderLayout.NORTH);

        JProgressBar bar = new JProgressBar(0, 1000);
        bar.setValue(realDmg > 0 ? (int) realDmg : 0);
        bar.setPreferredSize(new Dimension(100, 18));
        bar.setStringPainted(true);
        bar.setFont(new Font("Consolas", Font.BOLD, 11));
        bar.setForeground(new Color(180, 40, 55));
        bar.setBackground(new Color(45, 52, 60));
        bar.setBorder(BorderFactory.createLineBorder(new Color(60, 70, 85), 1));

        if (realDmg < 0) {
            bar.setString("-");
        } else {
            bar.setString(String.format("Raw:%.0f → Real:%.0f", rawDmg, realDmg));
        }

        panel.add(bar, BorderLayout.SOUTH);
        return panel;
    }
}