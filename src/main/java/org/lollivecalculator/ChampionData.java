package org.lollivecalculator;

import java.util.List;
import java.util.Map;

public class ChampionData {

    public static class ChampionRoot {
        public Map<String, Champion> champions;
    }

    public static class Champion {
        public int id;
        public String key;
        public String name;
        public String title;
        public String resource;
        public String attackType;
        public String adaptiveType;
        public Map<String, StatValue> stats;
        public List<String> positions;
        public List<String> roles;
        public Map<String, Integer> attributeRatings;
        public Map<String, List<Ability>> abilities;
    }

    public static class StatValue {
        public double flat;
        public double percent;
        public double perLevel;
        public double percentPerLevel;
    }

    public static class Ability {
        public String name;
        public String icon;
        public String targeting;
        public String affects;
        public String damageType;
        public String blurb;
        public String castTime;
        public String spellEffects; // EXPOSING THIS JSON FIELD FROM THE DATASET
        public CooldownBlock cooldown;
        public List<Effect> effects;
    }

    public static class CooldownBlock {
        public List<ModifierGroup> modifiers;
        public boolean affectedByCdr;
    }

    public static class Effect {
        public String description;
        public List<LevelingBlock> leveling;
    }

    public static class LevelingBlock {
        public String attribute;
        public List<ModifierGroup> modifiers;
    }

    public static class ModifierGroup {
        public List<Double> values;
        public List<String> units;
    }
}