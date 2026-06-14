package org.lollivecalculator.service;

import org.lollivecalculator.model.ChampionData;
import org.lollivecalculator.model.ItemData;
import org.lollivecalculator.model.LiveGameData;
import org.lollivecalculator.util.LoggerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CalculatorController {

    public static class CalculatedAbilityResult {
        public final String slot;
        public final String name;
        public final double rawDamage;
        public final double mitigatedDamage;

        public CalculatedAbilityResult(String slot, String name, double rawDamage, double mitigatedDamage) {
            this.slot = slot;
            this.name = name;
            this.rawDamage = rawDamage;
            this.mitigatedDamage = mitigatedDamage;
        }
    }

    public List<CalculatedAbilityResult> computeEnemyMitigationPipeline(
            LiveGameData.LivePlayer enemy,
            LiveGameData.Root liveData,
            LiveGameData.ChampionStats liveStats,
            ChampionData.Champion myStaticChamp,
            GameDataParser parser) {

        List<CalculatedAbilityResult> results = new ArrayList<>();
        if (myStaticChamp == null || myStaticChamp.abilities == null) return results;

        ChampionData.Champion enemyStatic = parser.getChampion(enemy.championName);

        if (enemyStatic == null || enemyStatic.stats == null) {
            System.err.println("❌ ERROR: Impossibile trovare le statistiche di " + enemy.championName + " nel ChampionsJson!");
            return results;
        }

        LoggerUtils.logStartPipeline(myStaticChamp.name, enemyStatic.name);

        int myLevel = liveData.activePlayer != null ? liveData.activePlayer.level : 1;
        LoggerUtils.logAttackerStats(myLevel, liveStats);

        double baseArmor = calculateBaseArmor(enemyStatic, enemy.level);
        double baseMr = calculateBaseMagicResist(enemyStatic, enemy.level);

        InventoryStats inventory = parseEnemyInventory(enemy, parser);

        double totalArmor = baseArmor + inventory.bonusArmor;
        double totalMr = baseMr + inventory.bonusMr;

        double effectiveArmor = DamageEngine.calculateEffectiveResistance(totalArmor, "PHYSICAL", liveStats);
        double effectiveMr = DamageEngine.calculateEffectiveResistance(totalMr, "MAGIC", liveStats);

        LoggerUtils.logTargetDefenses(enemy.level, baseArmor, inventory.bonusArmor, effectiveArmor,
                baseMr, inventory.bonusMr, effectiveMr, inventory.hasPlatedSteelcaps);

        calculateKitDamages(results, myStaticChamp, liveData, liveStats, myLevel, effectiveArmor, effectiveMr, inventory.hasPlatedSteelcaps);

        return results;
    }

    private double calculateBaseArmor(ChampionData.Champion enemyStatic, int enemyLevel) {
        if (enemyStatic.stats.containsKey("armor")) {
            ChampionData.StatValue armorStat = enemyStatic.stats.get("armor");
            double growthFactor = DamageEngine.calculateGrowthFormula(enemyLevel);
            return Math.floor(armorStat.flat + (armorStat.perLevel * growthFactor));
        }
        return 30.0;
    }

    private double calculateBaseMagicResist(ChampionData.Champion enemyStatic, int enemyLevel) {
        if (enemyStatic.stats.containsKey("magicResistance")) {
            ChampionData.StatValue mrStat = enemyStatic.stats.get("magicResistance");
            double growthFactor = DamageEngine.calculateGrowthFormula(enemyLevel);
            return Math.floor(mrStat.flat + (mrStat.perLevel * growthFactor));
        }
        return 30.0;
    }

    private InventoryStats parseEnemyInventory(LiveGameData.LivePlayer enemy, GameDataParser parser) {
        InventoryStats inv = new InventoryStats();
        if (enemy.items == null) return inv;

        for (LiveGameData.LiveItem liveItem : enemy.items) {
            if (liveItem.itemID == 3047) {
                inv.hasPlatedSteelcaps = true;
            }

            ItemData.Item staticItem = parser.getItem(liveItem.itemID);
            if (staticItem != null && staticItem.stats != null) {
                if (staticItem.stats.containsKey("armor")) {
                    inv.bonusArmor += staticItem.stats.get("armor").flat * liveItem.count;
                }
                if (staticItem.stats.containsKey("magicResistance")) {
                    inv.bonusMr += staticItem.stats.get("magicResistance").flat * liveItem.count;
                }
            }
        }
        return inv;
    }

    private void calculateKitDamages(
            List<CalculatedAbilityResult> results,
            ChampionData.Champion myStaticChamp,
            LiveGameData.Root liveData,
            LiveGameData.ChampionStats liveStats,
            int myLevel,
            double effectiveArmor,
            double effectiveMr,
            boolean hasPlatedSteelcaps) {

        for (Map.Entry<String, List<ChampionData.Ability>> entry : myStaticChamp.abilities.entrySet()) {
            String slot = entry.getKey();
            List<ChampionData.Ability> staticVariants = entry.getValue();
            LiveGameData.LiveAbility liveAbility = liveData.activePlayer.abilities.get(slot);

            if (liveAbility == null || staticVariants == null || staticVariants.isEmpty()) {
                continue;
            }

            for (ChampionData.Ability variant : staticVariants) {
                if (variant.effects == null) continue;

                for (int i = 0; i < variant.effects.size(); i++) {
                    ChampionData.Effect effect = variant.effects.get(i);
                    if (effect.leveling == null || effect.leveling.isEmpty()) continue;

                    double preMitigationDmg = DamageEngine.calculatePreMitigationDamage(effect, liveAbility.abilityLevel, liveStats, myStaticChamp, myLevel);
                    if (preMitigationDmg < 0.0) continue;

                    String dmgType = variant.damageType != null ? variant.damageType.toUpperCase() : "PHYSICAL_DAMAGE";
                    double targetedResist = dmgType.contains("MAGIC") ? effectiveMr : effectiveArmor;

                    double finalDamage = DamageEngine.calculatePostMitigationDamage(preMitigationDmg, targetedResist);

                    if (hasPlatedSteelcaps && variant.spellEffects != null && "proc".equalsIgnoreCase(variant.spellEffects.trim())) {
                        finalDamage = Math.floor(finalDamage * 0.88);
                    }

                    String displayName = variant.name + (variant.effects.size() > 1 ? " (Effetto " + (i + 1) + ")" : "");
                    boolean isProcAbility = (hasPlatedSteelcaps && variant.spellEffects != null && "proc".equalsIgnoreCase(variant.spellEffects.trim()));

                    LoggerUtils.logAbilityPipeline(slot, displayName, liveAbility.abilityLevel, preMitigationDmg, targetedResist, finalDamage, isProcAbility);

                    results.add(new CalculatedAbilityResult(slot, displayName, preMitigationDmg, finalDamage));
                }
            }
        }
    }

    private static class InventoryStats {
        double bonusArmor = 0.0;
        double bonusMr = 0.0;
        boolean hasPlatedSteelcaps = false;
    }
}