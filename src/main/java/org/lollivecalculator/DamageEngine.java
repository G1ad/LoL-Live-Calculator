package org.lollivecalculator;

import java.util.List;

public class DamageEngine {

    // Cambiato il ritorno per elaborare un SINGOLO effetto specifico alla volta, non l'intera lista indiscriminatamente
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
            // Usa l'AD Totale live (es. per la Q di Ezreal)
            accumulator.addScalingDamage((valueAtRank / 100.0) * liveStats.attackDamage);
        } else if (unit.contains("% AP")) {
            accumulator.addScalingDamage((valueAtRank / 100.0) * liveStats.abilityPower);
        } else if (unit.contains("% bonus AD")) {
            // Calcola l'AD Base effettivo del campione al livello attuale
            double baseAd = calculateBaseAttackDamage(myStaticChamp, myLevel);
            // AD Bonus = AD Totale - AD Base Naturale
            double bonusAd = Math.max(0.0, liveStats.attackDamage - baseAd);
            accumulator.addScalingDamage((valueAtRank / 100.0) * bonusAd);
        } else {
            accumulator.addBaseDamage(valueAtRank);
        }
    }

    /**
     * Ripristinato il calcolo corretto dell'AD Naturale del campione basato sul livello.
     */
    private static double calculateBaseAttackDamage(ChampionData.Champion myStaticChamp, int myLevel) {
        if (myStaticChamp != null && myStaticChamp.stats != null && myStaticChamp.stats.containsKey("attackDamage")) {
            ChampionData.StatValue adStat = myStaticChamp.stats.get("attackDamage");
            double growthFactor = calculateGrowthFormula(myLevel);
            // Ritorna l'AD base corretto per il livello attuale (es. a livello 11 considererà la crescita)
            return adStat.flat + (adStat.perLevel * growthFactor);
        }
        return 60.0;
    }

    /**
     * Official LoL Wiki growth formula:
     * Statistic = base + growth × (level - 1) × (0.7025 + 0.0175 × (level - 1))
     * This returns the growth factor: (level - 1) × (0.7025 + 0.0175 × (level - 1))
     */
    private static double calculateGrowthFormula(int level) {
        double levelUps = level - 1;
        return levelUps * (0.7025 + 0.0175 * levelUps);
    }

    public static double calculateEffectiveResistance(double baseResist, String damageType, LiveGameData.ChampionStats myStats) {
        double effective = baseResist;

        if ("PHYSICAL".equals(damageType)) {
            // Order of operations per LoL Wiki:
            // 1. Percentage armor penetration (applied to total armor)
            // 2. Flat armor penetration (lethality)
            // Note: physicalLethality and armorPenetrationFlat are the same stat in the API
            double percentPenFactor = parsePenetrationPercent(myStats.armorPenetrationPercent);
            effective = (effective * (1.0 - percentPenFactor)) - myStats.physicalLethality;

        } else if ("MAGIC".equals(damageType)) {
            // Order of operations per LoL Wiki:
            // 1. Percentage magic penetration (applied to total MR)
            // 2. Flat magic penetration
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