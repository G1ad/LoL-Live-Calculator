package org.lollivecalculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link org.lollivecalculator.service.GameDataParser}.
 * <p>
 * Uses the real champions.json and items.json from the {@code data/} directory,
 * validating that the JSON structure is parseable and that known champions
 * (Ezreal, Seraphine) are loaded correctly.
 * </p>
 */
class GameDataParserTest {

    private org.lollivecalculator.service.GameDataParser parser;

    @BeforeEach
    void setUp() {
        parser = new org.lollivecalculator.service.GameDataParser();
    }

    @Test
    void loadChampionsData_shouldLoadEzreal() throws Exception {
        parser.loadChampionsData("data/champions.json");

        assertTrue(parser.getLoadedChampionsCount() > 100,
                "Expected 100+ champions, got " + parser.getLoadedChampionsCount());

        var ezreal = parser.getChampion("Ezreal");
        assertNotNull(ezreal, "Ezreal should be present");
        assertEquals("Ezreal", ezreal.name);
        assertNotNull(ezreal.stats, "Ezreal should have stats");
        assertTrue(ezreal.stats.containsKey("attackDamage"), "Ezreal should have attackDamage stat");
        assertEquals(60.0, ezreal.stats.get("attackDamage").flat, 0.001, "Ezreal base AD should be 60");

        // Check ability Q exists
        assertNotNull(ezreal.abilities, "Ezreal should have abilities");
        assertTrue(ezreal.abilities.containsKey("Q"), "Ezreal should have Q ability");
    }

    @Test
    void loadChampionsData_shouldLoadSeraphine() throws Exception {
        parser.loadChampionsData("data/champions.json");

        var seraphine = parser.getChampion("Seraphine");
        assertNotNull(seraphine, "Seraphine should be present");
        assertNotNull(seraphine.stats);
        assertTrue(seraphine.stats.containsKey("armor"));
    }

    @Test
    void loadChampionsData_throwsOnMissingFile() {
        assertThrows(Exception.class,
                () -> parser.loadChampionsData("nonexistent.json"));
    }

    @Test
    void loadItemsData_shouldLoadItems() throws Exception {
        parser.loadItemsData("data/items.json");

        assertTrue(parser.getLoadedItemsCount() > 100,
                "Expected 100+ items, got " + parser.getLoadedItemsCount());

        // Check a known item: Ruby Crystal (ID 1028)
        var rubyCrystal = parser.getItem(1028);
        assertNotNull(rubyCrystal, "Ruby Crystal (1028) should exist");
        assertTrue(rubyCrystal.stats.containsKey("health"),
                "Ruby Crystal should give health");
        assertEquals(150.0, rubyCrystal.stats.get("health").flat, 0.001);
    }

    @Test
    void loadItemsData_throwsOnMissingFile() {
        assertThrows(Exception.class,
                () -> parser.loadItemsData("nonexistent.json"));
    }

    @Test
    void parseLiveGameData_shouldParseExampleFile() throws Exception {
        String json = Files.readString(
                Paths.get("src/test/resources/live_game_example.json"),
                StandardCharsets.UTF_8);

        var liveData = parser.parseLiveGameData(json);
        assertNotNull(liveData, "Live game data should be parseable");

        // Active player
        assertNotNull(liveData.activePlayer);
        assertEquals(1, liveData.activePlayer.level);
        assertNotNull(liveData.activePlayer.championStats);

        // All players
        assertNotNull(liveData.allPlayers);
        assertEquals(2, liveData.allPlayers.size());

        // First player is Ezreal
        var ezrealPlayer = liveData.allPlayers.get(0);
        assertEquals("Ezreal", ezrealPlayer.championName);
        assertEquals(1, ezrealPlayer.level);
        assertEquals("ORDER", ezrealPlayer.team);

        // Second player is Seraphine (bot)
        var seraphinePlayer = liveData.allPlayers.get(1);
        assertEquals("Seraphine", seraphinePlayer.championName);
        assertEquals(5, seraphinePlayer.level);
        assertEquals("CHAOS", seraphinePlayer.team);
        assertTrue(seraphinePlayer.isBot);
    }

    @Test
    void parseLiveGameData_returnsNullOnEmptyInput() {
        assertNull(parser.parseLiveGameData(null));
        assertNull(parser.parseLiveGameData(""));
        assertNull(parser.parseLiveGameData("   "));
    }
}