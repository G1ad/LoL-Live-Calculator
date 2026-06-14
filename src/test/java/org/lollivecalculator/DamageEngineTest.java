package org.lollivecalculator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lollivecalculator.model.ChampionData;
import org.lollivecalculator.model.LiveGameData;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link org.lollivecalculator.service.DamageEngine}.
 * <p>
 * Covers: growth formula, penetration/lethality, post-mitigation damage,
 * pre-mitigation damage for Ezreal abilities, and edge cases.
 * </p>
 */
class DamageEngineTest {

    private static ChampionData.Champion ezreal;
    private LiveGameData.ChampionStats stats;

    @BeforeAll
    static void setUpClass() {
        ezreal = buildEzrealStatic();
    }

    /** Reset stats before each test to avoid cross-test contamination. */
    @BeforeEach
    void setUp() {
        stats = new LiveGameData.ChampionStats();
        stats.attackDamage = 65.4;
        stats.abilityPower = 0.0;
        stats.armor = 24.0;
        stats.magicResist = 30.0;
        stats.armorPenetrationPercent = 1.0;
        stats.armorPenetrationFlat = 0.0;
        stats.physicalLethality = 0.0;
        stats.magicPenetrationPercent = 1.0;
        stats.magicPenetrationFlat = 0.0;
    }

    // ── Growth Formula Tests ─────────────────────────────────────────────

    @Test
    void growthFormula_level1_shouldBeZero() {
        double gf = org.lollivecalculator.service.DamageEngine.calculateGrowthFormula(1);
        assertEquals(0.0, gf, 0.0001);
    }

    @Test
    void growthFormula_level2() {
        double gf = org.lollivecalculator.service.DamageEngine.calculateGrowthFormula(2);
        // (2-1) * (0.7025 + 0.0175 * (2-1)) = 0.72
        assertEquals(0.72, gf, 0.0001);
    }

    @Test
    void growthFormula_level18() {
        double gf = org.lollivecalculator.service.DamageEngine.calculateGrowthFormula(18);
        // 17 * (0.7025 + 0.0175 * 17) = 17 * 1.0 = 17.0
        assertEquals(17.0, gf, 0.0001);
    }

    @Test
    void ezrealArmor_growth() {
        double gf = org.lollivecalculator.service.DamageEngine.calculateGrowthFormula(5);
        double armor = 24.0 + 4.2 * gf;
        assertEquals(36.978, armor, 0.01);
    }

    // ── Armor / MR Penetration Tests ─────────────────────────────────────

    @Test
    void effectiveResistance_noPenetration() {
        double effective = org.lollivecalculator.service.DamageEngine.calculateEffectiveResistance(
                50.0, "PHYSICAL", stats);
        assertEquals(50.0, effective, 0.001, "No pen should leave armor unchanged");
    }

    @Test
    void effectiveResistance_percentPenetration() {
        stats.armorPenetrationPercent = 0.7;
        double effective = org.lollivecalculator.service.DamageEngine.calculateEffectiveResistance(
                100.0, "PHYSICAL", stats);
        // 100 * (1.0 - 0.7) = 30
        assertEquals(30.0, effective, 0.001);
    }

    @Test
    void effectiveResistance_lethality() {
        stats.physicalLethality = 18.0;
        double effective = org.lollivecalculator.service.DamageEngine.calculateEffectiveResistance(
                50.0, "PHYSICAL", stats);
        assertEquals(32.0, effective, 0.001);
    }

    @Test
    void effectiveResistance_percentThenFlat() {
        stats.armorPenetrationPercent = 0.7;
        stats.physicalLethality = 10.0;
        double effective = org.lollivecalculator.service.DamageEngine.calculateEffectiveResistance(
                100.0, "PHYSICAL", stats);
        // 100 * (1.0 - 0.7) - 10 = 20
        assertEquals(20.0, effective, 0.001);
    }

    @Test
    void effectiveResistance_magicPen() {
        stats.magicPenetrationPercent = 0.6;
        stats.magicPenetrationFlat = 15.0;
        double effective = org.lollivecalculator.service.DamageEngine.calculateEffectiveResistance(
                100.0, "MAGIC", stats);
        // 100 * (1.0 - 0.6) - 15 = 40 - 15 = 25
        assertEquals(25.0, effective, 0.001);
    }

    @Test
    void effectiveResistance_negative_boundedToZero() {
        stats.physicalLethality = 999.0;
        double effective = org.lollivecalculator.service.DamageEngine.calculateEffectiveResistance(
                10.0, "PHYSICAL", stats);
        assertEquals(0.0, effective, 0.001, "Resistance should not go below 0");
    }

    // ── Post-Mitigation Damage Tests ─────────────────────────────────────

    @Test
    void postMitigationDamage_zeroResist() {
        double dmg = org.lollivecalculator.service.DamageEngine.calculatePostMitigationDamage(100.0, 0.0);
        assertEquals(100.0, dmg, 0.001);
    }

    @Test
    void postMitigationDamage_100Resist() {
        double dmg = org.lollivecalculator.service.DamageEngine.calculatePostMitigationDamage(100.0, 100.0);
        assertEquals(50.0, dmg, 0.001);
    }

    @Test
    void postMitigationDamage_negativePre_returnsNegative() {
        double dmg = org.lollivecalculator.service.DamageEngine.calculatePostMitigationDamage(-5.0, 50.0);
        assertTrue(dmg < 0);
    }

    @Test
    void postMitigationDamage_truncation() {
        double dmg = org.lollivecalculator.service.DamageEngine.calculatePostMitigationDamage(100.0, 30.0);
        assertEquals(76.0, dmg, 0.001, "Should truncate (not floor/round)");
    }

    // ── Pre-Mitigation Damage Tests (Ezreal) ────────────────────────────

    @Test
    void ezrealQ_rank1_noItems() {
        var effect = buildEzrealQEffect();
        double dmg = org.lollivecalculator.service.DamageEngine.calculatePreMitigationDamage(
                effect, 1, stats, ezreal, 1);
        double expected = 20.0 + 1.3 * 65.4; // 105.02 → truncation = 105
        assertEquals(105, dmg, 0.001);
    }

    @Test
    void ezrealQ_rank5_withAD() {
        var effect = buildEzrealQEffect();
        stats.attackDamage = 150.0;
        stats.abilityPower = 30.0;
        double dmg = org.lollivecalculator.service.DamageEngine.calculatePreMitigationDamage(
                effect, 5, stats, ezreal, 9);
        double expected = 120.0 + 1.3 * 150.0 + 0.15 * 30.0; // 319.5 → 319
        assertEquals(319, dmg, 0.001);
    }

    @Test
    void ezrealW_rank1_noItems() {
        var effect = buildEzrealWEffect();
        double dmg = org.lollivecalculator.service.DamageEngine.calculatePreMitigationDamage(
                effect, 1, stats, ezreal, 1);
        // 80 + 1.0 * (65.4 - 60) + 0.7 * 0.0 = 85.4 → 85
        assertEquals(85, dmg, 0.001);
    }

    @Test
    void ezrealR_rank1_noItems() {
        var effect = buildEzrealREffect();
        double dmg = org.lollivecalculator.service.DamageEngine.calculatePreMitigationDamage(
                effect, 1, stats, ezreal, 1);
        // 350 + 1.0 * (65.4-60) + 0.9 * 0.0 = 355.4 → 355
        assertEquals(355, dmg, 0.001);
    }

    @Test
    void ability_notLeveled_returnsNegative() {
        var effect = buildEzrealQEffect();
        double dmg = org.lollivecalculator.service.DamageEngine.calculatePreMitigationDamage(
                effect, 0, stats, ezreal, 1);
        assertTrue(dmg < 0);
    }

    @Test
    void nullEffect_returnsNegative() {
        double dmg = org.lollivecalculator.service.DamageEngine.calculatePreMitigationDamage(
                null, 1, stats, ezreal, 1);
        assertTrue(dmg < 0);
    }

    // ── Full Pipeline ────────────────────────────────────────────────────

    @Test
    void fullPipeline_ezrealQ_vsSeraphine() {
        var effect = buildEzrealQEffect();
        double preDmg = org.lollivecalculator.service.DamageEngine.calculatePreMitigationDamage(
                effect, 1, stats, ezreal, 1);
        assertEquals(105, preDmg, 0.001);

        double seraphineArmor = 30.0 + 4.2 * org.lollivecalculator.service.DamageEngine.calculateGrowthFormula(5);
        double effectiveArmor = org.lollivecalculator.service.DamageEngine.calculateEffectiveResistance(
                seraphineArmor, "PHYSICAL", stats);
        assertEquals(seraphineArmor, effectiveArmor, 0.001);

        double postDmg = org.lollivecalculator.service.DamageEngine.calculatePostMitigationDamage(preDmg, effectiveArmor);
        assertEquals(73, postDmg, 0.001);
    }

    // ── Helper Methods ───────────────────────────────────────────────────

    private static ChampionData.Champion buildEzrealStatic() {
        var champ = new ChampionData.Champion();
        champ.name = "Ezreal";
        champ.stats = Map.of(
                "attackDamage",   stat(60.0, 2.75),
                "armor",          stat(24.0, 4.2),
                "magicResistance", stat(30.0, 1.3)
        );
        return champ;
    }

    private static ChampionData.StatValue stat(double flat, double perLevel) {
        var s = new ChampionData.StatValue();
        s.flat = flat;
        s.perLevel = perLevel;
        return s;
    }

    private static ChampionData.Effect buildEzrealQEffect() {
        var effect = new ChampionData.Effect();
        var block = new ChampionData.LevelingBlock();
        block.attribute = "damage";

        var baseMod = new ChampionData.ModifierGroup();
        baseMod.values = List.of(20.0, 45.0, 70.0, 95.0, 120.0);
        baseMod.units = List.of("", "", "", "", "");

        var adMod = new ChampionData.ModifierGroup();
        adMod.values = List.of(130.0, 130.0, 130.0, 130.0, 130.0);
        adMod.units = List.of("% AD", "% AD", "% AD", "% AD", "% AD");

        var apMod = new ChampionData.ModifierGroup();
        apMod.values = List.of(15.0, 15.0, 15.0, 15.0, 15.0);
        apMod.units = List.of("% AP", "% AP", "% AP", "% AP", "% AP");

        block.modifiers = List.of(baseMod, adMod, apMod);
        effect.leveling = List.of(block);
        return effect;
    }

    private static ChampionData.Effect buildEzrealWEffect() {
        var effect = new ChampionData.Effect();
        var block = new ChampionData.LevelingBlock();
        block.attribute = "damage";

        var baseMod = new ChampionData.ModifierGroup();
        baseMod.values = List.of(80.0, 135.0, 190.0, 245.0, 300.0);
        baseMod.units = List.of("", "", "", "", "");

        var bonusAdMod = new ChampionData.ModifierGroup();
        bonusAdMod.values = List.of(100.0, 100.0, 100.0, 100.0, 100.0);
        bonusAdMod.units = List.of("% bonus AD", "% bonus AD", "% bonus AD", "% bonus AD", "% bonus AD");

        var apMod = new ChampionData.ModifierGroup();
        apMod.values = List.of(70.0, 75.0, 80.0, 85.0, 90.0);
        apMod.units = List.of("% AP", "% AP", "% AP", "% AP", "% AP");

        block.modifiers = List.of(baseMod, bonusAdMod, apMod);
        effect.leveling = List.of(block);
        return effect;
    }

    private static ChampionData.Effect buildEzrealREffect() {
        var effect = new ChampionData.Effect();
        var block = new ChampionData.LevelingBlock();
        block.attribute = "damage";

        var baseMod = new ChampionData.ModifierGroup();
        baseMod.values = List.of(350.0, 550.0, 750.0);
        baseMod.units = List.of("", "", "");

        var bonusAdMod = new ChampionData.ModifierGroup();
        bonusAdMod.values = List.of(100.0, 100.0, 100.0);
        bonusAdMod.units = List.of("% bonus AD", "% bonus AD", "% bonus AD");

        var apMod = new ChampionData.ModifierGroup();
        apMod.values = List.of(90.0, 90.0, 90.0);
        apMod.units = List.of("% AP", "% AP", "% AP");

        block.modifiers = List.of(baseMod, bonusAdMod, apMod);
        effect.leveling = List.of(block);
        return effect;
    }
}