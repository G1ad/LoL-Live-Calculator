package org.lollivecalculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lollivecalculator.model.LiveGameData;
import org.lollivecalculator.service.GameDataParser;
import org.lollivecalculator.service.GameStateManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link GameStateManager}.
 * <p>
 * Uses the real champions.json to validate player identity resolution
 * from live game data.
 * </p>
 */
class GameStateManagerTest {

    private GameStateManager manager;
    private GameDataParser parser;

    @BeforeEach
    void setUp() throws Exception {
        manager = new GameStateManager();
        parser = new GameDataParser();
        parser.loadChampionsData("data/champions.json");
    }

    @Test
    void initialState_shouldNotHaveValidSession() {
        assertFalse(manager.hasValidSession());
        assertNull(manager.getCurrentData());
        assertNull(manager.getActiveChampionName());
        assertNull(manager.getMyTeam());
        assertNull(manager.getMyStaticChampion());
    }

    @Test
    void processNullData_returnsFalse() {
        assertFalse(manager.processGameData(null, parser));
    }

    @Test
    void processGameData_resolvesEzreal() {
        var data = buildLiveData("fakeid#example", "fakeid", "Ezreal", "ORDER");
        assertTrue(manager.processGameData(data, parser));

        assertTrue(manager.hasValidSession());
        assertEquals("Ezreal", manager.getActiveChampionName());
        assertEquals("ORDER", manager.getMyTeam());
        assertNotNull(manager.getMyStaticChampion());
        assertEquals("Ezreal", manager.getMyStaticChampion().name);
        assertNotNull(manager.getLiveStats());
        assertEquals(65.4, manager.getLiveStats().attackDamage, 0.001);
    }

    @Test
    void processGameData_resolvesBySummonerName() {
        var data = buildLiveData(null, "PlayerOne", "Seraphine", "CHAOS");
        assertTrue(manager.processGameData(data, parser));

        assertTrue(manager.hasValidSession());
        assertEquals("Seraphine", manager.getActiveChampionName());
        assertEquals("CHAOS", manager.getMyTeam());
    }

    @Test
    void reset_clearsAllState() {
        var data = buildLiveData("test#test", "test", "Ezreal", "ORDER");
        assertTrue(manager.processGameData(data, parser));
        assertTrue(manager.hasValidSession());

        manager.reset();

        assertFalse(manager.hasValidSession());
        assertNull(manager.getCurrentData());
        assertNull(manager.getActiveChampionName());
        assertNull(manager.getMyTeam());
        assertNull(manager.getMyStaticChampion());
    }

    @Test
    void processGameData_championNotFound_returnsFalse() {
        // Champion "UnknownChamp" doesn't exist in the JSON
        var data = buildLiveData("x#x", "x", "UnknownChamp", "ORDER");
        assertFalse(manager.processGameData(data, parser));
        assertFalse(manager.hasValidSession());
    }

    // ── Helper ───────────────────────────────────────────────────────────

    private static LiveGameData.Root buildLiveData(String riotId, String summonerName,
                                                   String championName, String team) {
        var root = new LiveGameData.Root();
        root.activePlayer = new LiveGameData.ActivePlayer();
        root.activePlayer.riotId = riotId;
        root.activePlayer.summonerName = summonerName;
        root.activePlayer.level = 1;

        var stats = new LiveGameData.ChampionStats();
        stats.attackDamage = 65.4;
        stats.abilityPower = 0.0;
        stats.armorPenetrationPercent = 1.0;
        stats.magicPenetrationPercent = 1.0;
        root.activePlayer.championStats = stats;

        var player = new LiveGameData.LivePlayer();
        player.championName = championName;
        player.team = team;
        player.riotId = riotId;
        player.summonerName = summonerName;
        player.level = 1;

        root.allPlayers = java.util.List.of(player);
        return root;
    }
}