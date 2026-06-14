package org.lollivecalculator.service;

import org.lollivecalculator.model.ChampionData;
import org.lollivecalculator.model.LiveGameData;

import java.util.List;

public class DamageEngine {

    public static double calculatePreMitigationDamage(
            ChampionData.Effect specificEffect,
            int liveAbilityLevel,
            LiveGameData.ChampionStats liveStats,
            ChampionData.Champion myStaticChamp,
            int myLevel) {

        if (liveAbilityLevel <= 0 || specificEffect == null || specificEffect.leveling == null) {
            return -1.0;
        }

        DamageAccumulator accumulator = new DamageAccumulator();
        int rankIndex = liveAbilityLevel - 1;

        for (ChampionData.LevelingBlock block : specificEffect.leveling) {
            if (block.modifiers == null) continue;
            processModifierGroups(block.modifiers, rankIndex, liveStats, myStaticChamp, myLevel, accumulator);
        }

        if (!accumulator.hasCalculatedStats) return -1.0;

        return Math.floor(accumulator.baseDamage + accumulator.scalingDamage);
    }

    private static void processModifierGroups(
            List<ChampionData.ModifierGroup> modifiers,
            int rankIndex,
            LiveGameData.ChampionStats liveStats,
            ChampionData.Champion myStaticChamp,
            int myLevel,
            DamageAccumulator accumulator) {

        for (ChampionData.ModifierGroup group : modifiers) {
            if (group.values == null || group.values.isEmpty()) continue;

            int currentRankIndex = Math.min(rankIndex, group.values.size() - 1);
            double valueAtRank = group.values.get(currentRankIndex);

            if (group.units == null || group.units.isEmpty()) {
                accumulator.addBaseDamage(valueAtRank);
                continue;
            }

            String primaryUnit = extractPrimaryUnit(group.units);
            resolveScalingValue(primaryUnit, valueAtRank, liveStats, myStaticChamp, myLevel, accumulator);
        }
    }

    private static String extractPrimaryUnit(List<String> units) {
        for (String unit : units) {
            if (unit != null && !unit.trim().isEmpty()) {
                return unit.trim();
            }
        }
        return "";
    }

    private static void resolveScalingValue(
            String unit,
            double valueAtRank,
            LiveGameData.ChampionStats liveStats,
            ChampionData.Champion myStaticChamp,
            int myLevel,
            DamageAccumulator accumulator) {

        if (unit.isEmpty()) {
            accumulator.addBaseDamage(valueAtRank);
        } else if (unit.contains("% AD")) {
            accumulator.addScalingDamage((valueAtRank / 100.0) * liveStats.attackDamage);
        } else if (unit.contains("% AP")) {
            accumulator.addScalingDamage((valueAtRank / 100.0) * liveStats.abilityPower);
        } else if (unit.contains("% bonus AD")) {
            double baseAd = calculateBaseAttackDamage(myStaticChamp, myLevel);
            double bonusAd = Math.max(0.0, liveStats.attackDamage - baseAd);
            accumulator.addScalingDamage((valueAtRank / 100.0) * bonusAd);
        } else {
            accumulator.addBaseDamage(valueAtRank);
        }
    }

    private static double calculateBaseAttackDamage(ChampionData.Champion myStaticChamp, int myLevel) {
        if (myStaticChamp != null && myStaticChamp.stats != null && myStaticChamp.stats.containsKey("attackDamage")) {
            ChampionData.StatValue adStat = myStaticChamp.stats.get("attackDamage");
            double growthFactor = calculateGrowthFormula(myLevel);
            return adStat.flat + (adStat.perLevel * growthFactor);
        }
        return 60.0;
    }

    /**
     * Official LoL Wiki growth formula:
     * Statistic = base + growth × (level - 1) × (0.7025 + 0.0175 × (level - 1))
     */
    public static double calculateGrowthFormula(int level) {
        double levelUps = level - 1;
        return levelUps * (0.7025 + 0.0175 * levelUps);
    }

    public static double calculateEffectiveResistance(double baseResist, String damageType, LiveGameData.ChampionStats myStats) {
        double effective = baseResist;

        if ("PHYSICAL".equals(damageType)) {
            double percentPenFactor = parsePenetrationPercent(myStats.armorPenetrationPercent);
            effective = (effective * (1.0 - percentPenFactor)) - myStats.physicalLethality;

        } else if ("MAGIC".equals(damageType)) {
            double percentPenFactor = parsePenetrationPercent(myStats.magicPenetrationPercent);
            effective = (effective * (1.0 - percentPenFactor)) - myStats.magicPenetrationFlat;
        }

        return Math.max(0.0, effective);
    }

    private static double parsePenetrationPercent(double rawPercent) {
        if (rawPercent == 1.0) {
            return 0.0;
        }
        return rawPercent > 1.0 ? rawPercent / 100.0 : rawPercent;
    }

    public static double calculatePostMitigationDamage(double preMitigationDmg, double effectiveResist) {
        if (preMitigationDmg < 0) return -1.0;
        double multiplier = 100.0 / (100.0 + effectiveResist);
        return Math.floor(preMitigationDmg * multiplier);
    }

    private static class DamageAccumulator {
        double baseDamage = 0.0;
        double scalingDamage = 0.0;
        boolean hasCalculatedStats = false;

        void addBaseDamage(double value) {
            this.baseDamage += value;
            this.hasCalculatedStats = true;
        }

        void addScalingDamage(double value) {
            this.scalingDamage += value;
            this.hasCalculatedStats = true;
        }
    }
}