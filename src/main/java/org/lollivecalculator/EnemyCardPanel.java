package org.lollivecalculator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Displays a single enemy champion card within the dashboard.
 * <p>
 * Shows the champion name, level, team, computed defenses, and a list of
 * incoming ability damage calculations. Uses {@link ThemeConfig} and
 * {@link UIComponentFactory} for consistent styling.
 * </p>
 */
public class EnemyCardPanel extends JPanel {

    private static final ThemeConfig T = ThemeConfig.getInstance();

    /**
     * Constructs an enemy card that renders all damage calculations.
     *
     * @param enemy              the enemy player from live game data
     * @param liveData           full live game data root
     * @param liveStats          the active player's live stats
     * @param myStaticChamp      the active player's static champion data
     * @param parser             game data parser (champions + items)
     * @param controller         damage calculation pipeline
     */
    public EnemyCardPanel(LiveGameData.LivePlayer enemy,
                          LiveGameData.Root liveData,
                          LiveGameData.ChampionStats liveStats,
                          ChampionData.Champion myStaticChamp,
                          GameDataParser parser,
                          CalculatorController controller) {

        setBackground(T.BG_PANEL);
        setBorder(T.CARD_BORDER);
        setLayout(new BorderLayout());

        // ── Header ───────────────────────────────────────────────────────
        add(buildHeader(enemy), BorderLayout.NORTH);

        // ── Body ─────────────────────────────────────────────────────────
        JPanel body = buildBody(enemy, liveData, liveStats, myStaticChamp, parser, controller);
        add(body, BorderLayout.CENTER);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private JPanel buildHeader(LiveGameData.LivePlayer enemy) {
        JPanel header = new JPanel(new GridLayout(2, 1, 2, 2));
        header.setBackground(T.BG_HEADER);
        header.setBorder(new EmptyBorder(T.PADDING_MEDIUM, T.PADDING_MEDIUM, T.PADDING_MEDIUM, T.PADDING_MEDIUM));

        header.add(UIComponentFactory.createTitle(enemy.championName));
        header.add(UIComponentFactory.createSubtitle(
                String.format("Level %d  |  %s Team", enemy.level, enemy.team)));

        return header;
    }

    private JPanel buildBody(LiveGameData.LivePlayer enemy,
                             LiveGameData.Root liveData,
                             LiveGameData.ChampionStats liveStats,
                             ChampionData.Champion myStaticChamp,
                             GameDataParser parser,
                             CalculatorController controller) {

        JPanel body = new JPanel();
        body.setBackground(T.BG_PANEL);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(T.PADDING_LARGE, T.PADDING_MEDIUM, T.PADDING_LARGE, T.PADDING_MEDIUM));

        // Compute effective defenses via the same pipeline used by the controller.
        double effectiveArmor = 0;
        double effectiveMr = 0;

        // Use the same growth formula for base stats
        ChampionData.Champion enemyStatic = parser.getChampion(enemy.championName);
        if (enemyStatic != null && enemyStatic.stats != null) {
            double baseArmor = calculateBaseStat(enemyStatic, "armor", enemy.level);
            double baseMr = calculateBaseStat(enemyStatic, "magicResistance", enemy.level);

            double bonusArmor = 0;
            double bonusMr = 0;
            if (enemy.items != null) {
                for (LiveGameData.LiveItem item : enemy.items) {
                    ItemData.Item si = parser.getItem(item.itemID);
                    if (si != null && si.stats != null) {
                        if (si.stats.containsKey("armor"))
                            bonusArmor += si.stats.get("armor").flat * item.count;
                        if (si.stats.containsKey("magicResistance"))
                            bonusMr += si.stats.get("magicResistance").flat * item.count;
                    }
                }
            }

            effectiveArmor = DamageEngine.calculateEffectiveResistance(
                    baseArmor + bonusArmor, "PHYSICAL", liveStats);
            effectiveMr = DamageEngine.calculateEffectiveResistance(
                    baseMr + bonusMr, "MAGIC", liveStats);
        }

        // Stat summary
        body.add(UIComponentFactory.createStatLine("Effective Armor:",
                String.format("%.1f", effectiveArmor)));
        body.add(UIComponentFactory.createStatLine("Effective MR:",
                String.format("%.1f", effectiveMr)));
        body.add(Box.createVerticalStrut(T.PADDING_LARGE));

        // Ability damage list
        List<CalculatorController.CalculatedAbilityResult> calculations =
                controller.computeEnemyMitigationPipeline(
                        enemy, liveData, liveStats, myStaticChamp, parser);

        JPanel abilityContainer = new JPanel();
        abilityContainer.setBackground(T.BG_PANEL);
        abilityContainer.setLayout(new BoxLayout(abilityContainer, BoxLayout.Y_AXIS));

        for (CalculatorController.CalculatedAbilityResult res : calculations) {
            abilityContainer.add(buildAbilityRow(res));
            abilityContainer.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = UIComponentFactory.createScrollPane(abilityContainer);
        body.add(scrollPane);
        return body;
    }

    /** Builds a row showing one ability's damage calculation. */
    private JPanel buildAbilityRow(CalculatorController.CalculatedAbilityResult res) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        panel.setBackground(T.BG_PANEL);
        panel.setMaximumSize(new Dimension(T.CARD_MAX_WIDTH, T.CARD_ABILITY_HEIGHT));

        // Slot + name
        JPanel labelSubPanel = new JPanel(new BorderLayout());
        labelSubPanel.setBackground(T.BG_PANEL);

        JLabel lblSlot = UIComponentFactory.createLabel(
                res.slot + " ", T.FONT_ABILITY_SLOT, T.TEXT_ACCENT);
        JLabel lblName = UIComponentFactory.createLabel(
                res.name, T.FONT_ABILITY_DESC, T.TEXT_ABILITY_NAME);

        labelSubPanel.add(lblSlot, BorderLayout.WEST);
        labelSubPanel.add(lblName, BorderLayout.CENTER);
        panel.add(labelSubPanel, BorderLayout.NORTH);

        // Damage bar
        JProgressBar bar = T.createProgressBar();
        if (res.mitigatedDamage > 0) {
            bar.setValue((int) res.mitigatedDamage);
        }
        if (res.mitigatedDamage < 0) {
            bar.setString("-");
        } else {
            bar.setString(String.format("Raw:%.0f → Real:%.0f",
                    res.rawDamage, res.mitigatedDamage));
        }
        panel.add(bar, BorderLayout.SOUTH);
        return panel;
    }

    /** Calculates a champion's base stat at the given level using the official growth formula. */
    private static double calculateBaseStat(ChampionData.Champion champ, String statKey, int level) {
        if (champ.stats != null && champ.stats.containsKey(statKey)) {
            ChampionData.StatValue sv = champ.stats.get(statKey);
            double levelUps = level - 1;
            double growthFactor = levelUps * (0.7025 + 0.0175 * levelUps);
            return sv.flat + (sv.perLevel * growthFactor);
        }
        return 0;
    }
}