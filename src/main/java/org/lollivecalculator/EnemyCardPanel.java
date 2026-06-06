package org.lollivecalculator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class EnemyCardPanel extends JPanel {

    public EnemyCardPanel(LiveGameData.LivePlayer enemy, LiveGameData.Root liveData, LiveGameData.ChampionStats liveStats, ChampionData.Champion myStaticChamp, GameDataParser parser) {
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

        // 1. Calculate Enemy Core Base Stats (Riot's Official Non-Linear Growth Curve)
        double baseArmor = 30.0;
        double baseMr = 30.0;

        ChampionData.Champion enemyStatic = parser.getChampion(enemy.championName);
        if (enemyStatic != null && enemyStatic.stats != null) {
            int n = enemy.level;
            if (enemyStatic.stats.containsKey("armor")) {
                ChampionData.StatValue armorStat = enemyStatic.stats.get("armor");
                baseArmor = armorStat.flat + (armorStat.perLevel * (n - 1) * (0.7025 + 0.0175 * (n - 1)));
            }
            if (enemyStatic.stats.containsKey("magicResistance")) {
                ChampionData.StatValue mrStat = enemyStatic.stats.get("magicResistance");
                baseMr = mrStat.flat + (mrStat.perLevel * (n - 1) * (0.7025 + 0.0175 * (n - 1)));
            }
        }

        // 2. Scan Enemy Inventory for Additive Resistances and Plated Steelcaps
        double bonusArmorFromItems = 0.0;
        double bonusMrFromItems = 0.0;
        boolean hasPlatedSteelcaps = false;

        if (enemy.items != null) {
            for (LiveGameData.LiveItem liveItem : enemy.items) {
                if (liveItem.itemID == 3047) {
                    hasPlatedSteelcaps = true;
                }

                ItemData.Item staticItem = parser.getItem(liveItem.itemID);
                if (staticItem != null && staticItem.stats != null) {
                    if (staticItem.stats.containsKey("armor")) {
                        bonusArmorFromItems += staticItem.stats.get("armor").flat * liveItem.count;
                    }
                    if (staticItem.stats.containsKey("magicResistance")) {
                        bonusMrFromItems += staticItem.stats.get("magicResistance").flat * liveItem.count;
                    }
                }
            }
        }

        double totalEnemyArmor = baseArmor + bonusArmorFromItems;
        double totalEnemyMr = baseMr + bonusMrFromItems;

        // 3. Compute Effective Values matching the strict LoL Wiki Order
        double effectiveArmor = calculateEffectiveResistance(totalEnemyArmor, "PHYSICAL", liveStats);
        double effectiveMr = calculateEffectiveResistance(totalEnemyMr, "MAGIC", liveStats);

        bodyPanel.add(createStatLine("True Enemy Armor:", String.format("%.1f (Eff: %.1f)", totalEnemyArmor, effectiveArmor)));
        bodyPanel.add(createStatLine("True Enemy MR:", String.format("%.1f (Eff: %.1f)", totalEnemyMr, effectiveMr)));
        bodyPanel.add(Box.createVerticalStrut(15));

        int myLevel = liveData.activePlayer != null ? liveData.activePlayer.level : 1;
        String[] slots = {"Q", "W", "E", "R"};

        for (String slot : slots) {
            LiveGameData.LiveAbility liveAbility = liveData.activePlayer.abilities.get(slot);
            java.util.List<ChampionData.Ability> staticVariants = myStaticChamp.abilities.get(slot);

            double preMitigationDmg = -1.0;
            double finalDmg = -1.0;
            String spellName = "Not Skilled";

            if (liveAbility != null && staticVariants != null && !staticVariants.isEmpty()) {
                ChampionData.Ability variant = staticVariants.get(0);
                spellName = variant.name;

                preMitigationDmg = DamageCalculator.calculatePreMitigationDamage(variant, liveAbility.abilityLevel, liveStats, myStaticChamp, myLevel);

                if (preMitigationDmg >= 0.0) {
                    double targetedResist = variant.damageType.contains("PHYSICAL") ? effectiveArmor : effectiveMr;

                    // Official Phase 2 Multiplier
                    double multiplier = 100.0 / (100.0 + targetedResist);

                    // Official Phase 3 Truncation
                    finalDmg = Math.floor(preMitigationDmg * multiplier);

                    // Steelcaps 12% Reduction applies strictly to Basic Attacks and explicitly tagged on-hit abilities (like Ezreal Q)
                    if (hasPlatedSteelcaps && "Q".equals(slot) && variant.name.toLowerCase().contains("mystic shot")) {
                        finalDmg = Math.floor(finalDmg * 0.88);
                    }
                }
            }

            bodyPanel.add(createAbilityRow(slot, spellName, preMitigationDmg, finalDmg));
            bodyPanel.add(Box.createVerticalStrut(10));
        }

        add(bodyPanel, BorderLayout.CENTER);
    }

    private double calculateEffectiveResistance(double baseResist, String damageType, LiveGameData.ChampionStats myStats) {
        double effective = baseResist;

        if ("PHYSICAL".equals(damageType)) {
            // Priority 1: Percent Penetration (Multiplier Stack)
            double percentPenFactor = myStats.armorPenetrationPercent;
            if (percentPenFactor > 1.0) {
                percentPenFactor /= 100.0;
            }
            effective = effective * (1.0 - percentPenFactor);

            // Priority 2: Flat Penetration (Lethality strictly evaluated 1:1)
            double flatPen = myStats.armorPenetrationFlat;
            effective = effective - flatPen;

        } else if ("MAGIC".equals(damageType)) {
            // Priority 1: Percent Penetration
            double percentPenFactor = myStats.magicPenetrationPercent;
            if (percentPenFactor > 1.0) {
                percentPenFactor /= 100.0;
            }
            effective = effective * (1.0 - percentPenFactor);

            // Priority 2: Flat Penetration
            effective = effective - myStats.magicPenetrationFlat;
        }

        return Math.max(0.0, effective);
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