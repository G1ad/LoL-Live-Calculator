package org.lollivecalculator.service;

import org.lollivecalculator.model.ChampionData;
import org.lollivecalculator.model.LiveGameData;

/**
 * Interface for the damage calculation engine.
 * <p>
 * Allows different implementations (e.g. for testing or different game modes)
 * and makes the calculation pipeline easier to mock in unit tests.
 * </p>
 */
public interface IDamageEngine {

    /**
     * Computes the total raw damage (pre-mitigation) for a single ability effect.
     *
     * @return raw damage value, or negative if the ability is not leveled / unavailable
     */
    double calculatePreMitigationDamage(
            ChampionData.Effect specificEffect,
            int liveAbilityLevel,
            LiveGameData.ChampionStats liveStats,
            ChampionData.Champion myStaticChamp,
            int myLevel);

    /**
     * Applies penetration/lethality to a resistance value.
     *
     * @param baseResist total resistance (base + items)
     * @param damageType "PHYSICAL" or "MAGIC"
     * @param myStats    attacker's live stats (penetration values)
     * @return effective resistance after penetration (minimum 0)
     */
    double calculateEffectiveResistance(
            double baseResist,
            String damageType,
            LiveGameData.ChampionStats myStats);

    /**
     * Computes post-mitigation damage given a target's effective resistance.
     *
     * @param preMitigationDmg raw damage before mitigation
     * @param effectiveResist  target's effective resistance
     * @return damage after mitigation (truncated to int per Riot conventions)
     */
    double calculatePostMitigationDamage(double preMitigationDmg, double effectiveResist);

    /**
     * Official LoL Wiki growth formula:
     * (level - 1) × (0.7025 + 0.0175 × (level - 1))
     *
     * @return the multiplicative growth factor for champion stats
     */
    double calculateGrowthFormula(int level);
}