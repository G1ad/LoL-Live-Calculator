package org.lollivecalculator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lollivecalculator.model.ChampionData;
import org.lollivecalculator.model.LiveGameData;
import org.lollivecalculator.service.CalculatorController;
import org.lollivecalculator.service.DamageEngine;
import org.lollivecalculator.service.GameDataParser;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link CalculatorController}.
 * <p>
 * Tests the full damage calculation pipeline: parsing champion + item data,
 * computing base + bonus defenses, applying penetration, and computing
 * ability damage from the static effect JSON data.
 * </p>
 */
class CalculatorControllerTest {

    private static GameDataParser parser;
    private CalculatorController controller;

    private LiveGameData.LivePlayer ezrealPlayer;
    private LiveGameData.LivePlayer seraphinePlayer;
    private LiveGameData.Root liveData;
    private LiveGameData.ChampionStats liveStats;
    private ChampionData.Champion ezrealStatic;

    @BeforeAll
    static void setUpClass() throws Exception {
        parser = new GameDataParser();
        parser.loadChampionsData("data/champions.json");
        parser.loadItemsData("data/items.json");
    }

    @BeforeEach
    void setUp() {
        controller = new CalculatorController();

        // Build live stats (Ezreal at level 1, no items)
        liveStats = new LiveGameData.ChampionStats();
        liveStats.attackDamage = 65.4;
        liveStats.abilityPower = 0.0;
        liveStats.armorPenetrationPercent = 1.0;
        liveStats.armorPenetrationFlat = 0.0;
        liveStats.physicalLethality = 0.0;
        liveStats.magicPenetrationPercent = 1.0;
        liveStats.magicPenetrationFlat = 0.0;

        // Build live data
        liveData = new LiveGameData.Root();
        liveData.activePlayer = new LiveGameData.ActivePlayer();
        liveData.activePlayer.level = 1;
        liveData.activePlayer.riotId = "player#ez";
        liveData.activePlayer.summonerName = "EzrealPlayer";
        liveData.activePlayer.championStats = liveStats;

        // Ezreal player
        ezrealPlayer = new LiveGameData.LivePlayer();
        ezrealPlayer.championName = "Ezreal";
        ezrealPlayer.level = 1;
        ezrealPlayer.team = "ORDER";
        ezrealPlayer.riotId = "player#ez";
        ezrealPlayer.items = java.util.List.of();

        // Seraphine (enemy bot)
        seraphinePlayer = new LiveGameData.LivePlayer();
        seraphinePlayer.championName = "Seraphine";
        seraphinePlayer.level = 5;
        seraphinePlayer.team = "CHAOS";
        seraphinePlayer.items = java.util.List.of();

        liveData.allPlayers = java.util.List.of(ezrealPlayer, seraphinePlayer);

        // Load Ezreal static data
        ezrealStatic = parser.getChampion("Ezreal");
        assertNotNull(ezrealStatic);
    }

    @Test
    void computeEnemyMitigation_noEnemyAbilitiesLeveled() {
        // All abilities at level 0 → no results
        var abilities = new java.util.HashMap<String, LiveGameData.LiveAbility>();
        abilities.put("Q", ability(0));
        abilities.put("W", ability(0));
        abilities.put("E", ability(0));
        abilities.put("R", ability(0));
        liveData.activePlayer.abilities = abilities;

        var results = controller.computeEnemyMitigationPipeline(
                seraphinePlayer, liveData, liveStats, ezrealStatic, parser);

        assertTrue(results.isEmpty(), "No abilities leveled → no results");
    }

    @Test
    void computeEnemyMitigation_qRank1() {
        var abilities = new java.util.HashMap<String, LiveGameData.LiveAbility>();
        abilities.put("Q", ability(1));
        liveData.activePlayer.abilities = abilities;

        var results = controller.computeEnemyMitigationPipeline(
                seraphinePlayer, liveData, liveStats, ezrealStatic, parser);

        assertFalse(results.isEmpty(), "Q rank 1 should produce results");

        // Find the Q result
        var qResult = results.stream()
                .filter(r -> "Q".equals(r.slot))
                .findFirst()
                .orElse(null);
        assertNotNull(qResult, "Should have Q result");
        assertTrue(qResult.rawDamage > 0, "Q raw damage should be positive");
        assertTrue(qResult.mitigatedDamage > 0, "Q mitigated damage should be positive");
        assertTrue(qResult.mitigatedDamage < qResult.rawDamage,
                "Mitigated damage should be less than raw damage");
    }

    @Test
    void computeEnemyMitigation_allAbilitiesRank1() {
        var abilities = new java.util.HashMap<String, LiveGameData.LiveAbility>();
        abilities.put("Q", ability(1));
        abilities.put("W", ability(1));
        abilities.put("E", ability(1));
        abilities.put("R", ability(1));
        liveData.activePlayer.abilities = abilities;

        var results = controller.computeEnemyMitigationPipeline(
                seraphinePlayer, liveData, liveStats, ezrealStatic, parser);

        // Should have at least 3 results (Q, W, E, R)
        // Note: R has 2 damage effects (full + half), so we expect 5+ results
        assertTrue(results.size() >= 4, "Expected 4+ ability results, got " + results.size());

        // Verify the ability slots are present
        var slots = results.stream().map(r -> r.slot).distinct().toList();
        assertTrue(slots.contains("Q"), "Should include Q");
        assertTrue(slots.contains("W"), "Should include W");
        assertTrue(slots.contains("E"), "Should include E");
        assertTrue(slots.contains("R"), "Should include R");
    }

    @Test
    void parseEnemyInventory_noItems() {
        var inventory = CalculatorController.parseEnemyInventory(seraphinePlayer, parser);
        assertNotNull(inventory);
        assertEquals(0.0, inventory.bonusArmor, 0.001);
        assertEquals(0.0, inventory.bonusMr, 0.001);
        assertFalse(inventory.hasPlatedSteelcaps);
    }

    @Test
    void parseEnemyInventory_withItems() {
        // Give Seraphine a Chain Vest (ID 1031 → +40 armor) and
        // a Negatron Cloak (ID 1057 → +50 MR)
        var chainVest = new LiveGameData.LiveItem();
        chainVest.itemID = 1031;
        chainVest.count = 1;

        var negatron = new LiveGameData.LiveItem();
        negatron.itemID = 1057;
        negatron.count = 1;

        seraphinePlayer.items = java.util.List.of(chainVest, negatron);

        var inventory = CalculatorController.parseEnemyInventory(seraphinePlayer, parser);
        assertEquals(40.0, inventory.bonusArmor, 0.001, "Chain Vest gives 40 armor");
        assertEquals(45.0, inventory.bonusMr, 0.001, "Negatron Cloak gives 45 MR in this dataset");
        assertFalse(inventory.hasPlatedSteelcaps);

        seraphinePlayer.items = java.util.List.of(); // reset
    }

    @Test
    void parseEnemyInventory_platedSteelcaps() {
        var steelcaps = new LiveGameData.LiveItem();
        steelcaps.itemID = 3047;
        steelcaps.count = 1;
        seraphinePlayer.items = java.util.List.of(steelcaps);

        var inventory = CalculatorController.parseEnemyInventory(seraphinePlayer, parser);
        assertTrue(inventory.hasPlatedSteelcaps);

        seraphinePlayer.items = java.util.List.of(); // reset
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private static LiveGameData.LiveAbility ability(int level) {
        var a = new LiveGameData.LiveAbility();
        a.abilityLevel = level;
        return a;
    }
}