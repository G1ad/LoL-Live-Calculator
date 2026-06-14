package org.lollivecalculator.service;

import org.lollivecalculator.model.ChampionData;
import org.lollivecalculator.model.LiveGameData;

/**
 * Documentation-only interface describing the public API of {@link DamageEngine}.
 * <p>
 * {@link DamageEngine} currently exposes all methods as {@code static} and does
 * not implement this interface. This interface exists as a specification of
 * what a future non-static implementation should provide (e.g. for DI / testing).
 * </p>
 */
public interface IDamageEngine {

    double calculatePreMitigationDamage(
            ChampionData.Effect specificEffect,
            int liveAbilityLevel,
            LiveGameData.ChampionStats liveStats,
            ChampionData.Champion myStaticChamp,
            int myLevel);

    double calculateEffectiveResistance(
            double baseResist,
            String damageType,
            LiveGameData.ChampionStats myStats);

    double calculatePostMitigationDamage(double preMitigationDmg, double effectiveResist);

    double calculateGrowthFormula(int level);
}