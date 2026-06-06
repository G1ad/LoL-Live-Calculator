package org.lollivecalculator;

public class DamageCalculator {

    public static double calculatePreMitigationDamage(
            ChampionData.Ability staticAbility,
            int liveAbilityLevel,
            LiveGameData.ChampionStats liveStats,
            ChampionData.Champion myStaticChamp,
            int myLevel) {

        if (liveAbilityLevel <= 0 || staticAbility.effects == null || staticAbility.effects.isEmpty()) {
            return -1.0;
        }

        if (staticAbility.damageType == null || staticAbility.damageType.trim().isEmpty()) {
            return -1.0;
        }

        double baseDamage = 0.0;
        double scalingDamage = 0.0;
        int rankIndex = liveAbilityLevel - 1;

        String name = staticAbility.name != null ? staticAbility.name.toLowerCase() : "";
        boolean isEzrealQ = name.contains("mystic shot");
        boolean hasCalculatedStats = false;

        for (ChampionData.Effect effect : staticAbility.effects) {
            if (effect.leveling == null) continue;

            for (ChampionData.LevelingBlock block : effect.leveling) {
                if (block.modifiers == null) continue;

                for (ChampionData.ModifierGroup group : block.modifiers) {
                    if (group.values == null || group.values.isEmpty()) continue;

                    int currentRankIndex = Math.min(rankIndex, group.values.size() - 1);
                    double valueAtRank = group.values.get(currentRankIndex);

                    // Explicit Base Damage: no units array
                    if (group.units == null || group.units.isEmpty()) {
                        baseDamage += valueAtRank;
                        hasCalculatedStats = true;
                        continue;
                    }

                    boolean matchedScaling = false;
                    for (String unit : group.units) {
                        if (unit != null) {
                            if (unit.trim().isEmpty()) {
                                baseDamage += valueAtRank;
                                matchedScaling = true;
                                hasCalculatedStats = true;
                                break;
                            } else if (unit.contains("% AD")) {
                                scalingDamage += (valueAtRank / 100.0) * liveStats.attackDamage;
                                matchedScaling = true;
                                hasCalculatedStats = true;
                                break;
                            } else if (unit.contains("% AP")) {
                                // Master data correction override for known static dataset bugs
                                double apRatio = isEzrealQ ? 0.40 : (valueAtRank / 100.0);
                                scalingDamage += apRatio * liveStats.abilityPower;
                                matchedScaling = true;
                                hasCalculatedStats = true;
                                break;
                            } else if (unit.contains("% bonus AD")) {
                                // Calculate accurate base AD to extract bonus AD
                                double myBaseAd = 60.0;
                                if (myStaticChamp != null && myStaticChamp.stats != null && myStaticChamp.stats.containsKey("attackDamage")) {
                                    ChampionData.StatValue adStat = myStaticChamp.stats.get("attackDamage");
                                    myBaseAd = adStat.flat + (adStat.perLevel * (myLevel - 1) * (0.7025 + 0.0175 * (myLevel - 1)));
                                }
                                double bonusAd = Math.max(0.0, liveStats.attackDamage - myBaseAd);
                                scalingDamage += (valueAtRank / 100.0) * bonusAd;
                                matchedScaling = true;
                                hasCalculatedStats = true;
                                break;
                            }
                        }
                    }

                    if (!matchedScaling) {
                        baseDamage += valueAtRank;
                        hasCalculatedStats = true;
                    }
                }
            }
        }

        if (!hasCalculatedStats) return -1.0;

        // League of Legends Engine Phase 1: Truncate raw pre-mitigation output
        return Math.floor(baseDamage + scalingDamage);
    }
}