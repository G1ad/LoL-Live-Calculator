package org.lollivecalculator.model;

import java.util.List;
import java.util.Map;

public class LiveGameData {

    public static class Root {
        public ActivePlayer activePlayer;
        public List<LivePlayer> allPlayers;
        public GameMetaData gameData;
    }

    public static class ActivePlayer {
        public String riotId;
        public String summonerName;
        public int level;
        public double currentGold;
        public Map<String, LiveAbility> abilities;
        public ChampionStats championStats;
    }

    public static class LiveAbility {
        public int abilityLevel;
        public String displayName;
        public String id;
    }

    public static class ChampionStats {
        public double attackDamage;
        public double abilityPower;
        public double armor;
        public double magicResist;
        public double armorPenetrationFlat;
        public double armorPenetrationPercent;
        public double magicPenetrationFlat;
        public double magicPenetrationPercent;
        public double physicalLethality;
        public double magicLethality;
        public double attackSpeed;
        public double abilityHaste;
        public double critChance;
        public double critDamage;
    }

    public static class LivePlayer {
        public String championName;
        public int level;
        public String team;
        public boolean isDead;
        public List<LiveItem> items;
        public String summonerName;
        public String riotId;
        public double armor;
        public double magicResist;
    }

    public static class LiveItem {
        public int itemID;
        public String displayName;
        public int count;
        public int slot;
        public boolean canUse;
        public boolean consumable;
        public int price;
    }

    public static class GameMetaData {
        public String gameMode;
        public double gameTime;
    }
}