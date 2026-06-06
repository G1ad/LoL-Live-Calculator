package org.lollivecalculator;

import java.util.Map;

public class ItemData {

    public static class Item {
        public int id;
        public String name;
        public Map<String, StatDetails> stats;
    }

    public static class StatDetails {
        public double flat;
        public double percent;
        public double perLevel;
        public double percentPerLevel;
        public double percentBase;
        public double percentBonus;
    }
}