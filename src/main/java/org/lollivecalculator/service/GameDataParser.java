package org.lollivecalculator.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.lollivecalculator.model.ChampionData;
import org.lollivecalculator.model.ItemData;
import org.lollivecalculator.model.LiveGameData;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class GameDataParser {

    private final Gson gson;
    private Map<String, ChampionData.Champion> championMap;
    private Map<String, ItemData.Item> itemMap;

    public GameDataParser() {
        this.gson = new Gson();
    }

    public void loadChampionsData(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Data file not found at: " + path.toAbsolutePath());
        }
        try (FileReader reader = new FileReader(filePath)) {
            Type mapType = new TypeToken<Map<String, ChampionData.Champion>>() {}.getType();
            championMap = gson.fromJson(reader, mapType);
        }
    }

    public void loadItemsData(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Data file not found at: " + path.toAbsolutePath());
        }
        try (FileReader reader = new FileReader(filePath)) {
            Type mapType = new TypeToken<Map<String, ItemData.Item>>() {}.getType();
            itemMap = gson.fromJson(reader, mapType);
        }
    }

    public ChampionData.Champion getChampion(String championName) {
        if (championMap == null) {
            throw new IllegalStateException("Game data has not been parsed yet!");
        }
        return championMap.get(championName);
    }

    public ItemData.Item getItem(int itemId) {
        if (itemMap == null) {
            return null;
        }
        return itemMap.get(String.valueOf(itemId));
    }

    public int getLoadedChampionsCount() {
        return championMap != null ? championMap.size() : 0;
    }

    public int getLoadedItemsCount() {
        return itemMap != null ? itemMap.size() : 0;
    }

    public LiveGameData.Root parseLiveGameData(String rawJsonString) {
        if (rawJsonString == null || rawJsonString.trim().isEmpty()) {
            return null;
        }
        return gson.fromJson(rawJsonString, LiveGameData.Root.class);
    }
}