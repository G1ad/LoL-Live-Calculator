package org.lollivecalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CalculatorController {

    public static class CalculatedAbilityResult {
        public final String slot;
        public final String name;
        public final double rawDamage;
        public final double mitigatedDamage;

        public CalculatedAbilityResult(String slot, String name, double rawDamage, double mitigatedDamage) {
            this.slot = slot;
            this.name = name;
            this.rawDamage = rawDamage;
            this.mitigatedDamage = mitigatedDamage;
        }
    }

    public List<CalculatedAbilityResult> computeEnemyMitigationPipeline(
            LiveGameData.LivePlayer enemy,
            LiveGameData.Root liveData,
            LiveGameData.ChampionStats liveStats,
            ChampionData.Champion myStaticChamp,
            GameDataParser parser) {

        List<CalculatedAbilityResult> results = new ArrayList<>();
        if (myStaticChamp == null || myStaticChamp.abilities == null) return results;

        // ESTRAZIONE CRITICA DEL NEMICO: Cerchiamo Corki dentro il file ChampionsJson.
        // Usiamo un controllo robusto per assicurarci che il nome corrisponda (es. "Corki")
        ChampionData.Champion enemyStatic = parser.getChampion(enemy.championName);

        // Se il parser fallisce, stampiamo un errore gigante in console per capire se il file JSON è corretto
        if (enemyStatic == null || enemyStatic.stats == null) {
            System.err.println("❌ ERROR: Impossibile trovare le statistiche di " + enemy.championName + " nel ChampionsJson!");
            return results;
        }

        LoggerUtils.logStartPipeline(myStaticChamp.name, enemyStatic.name);

        int myLevel = liveData.activePlayer != null ? liveData.activePlayer.level : 1;
        LoggerUtils.logAttackerStats(myLevel, liveStats);

        // 1. Calcola le resistenze base passando direttamente l'oggetto statico estratto dal JSON, senza ri-cercarlo
        double baseArmor = calculateBaseArmor(enemyStatic, enemy.level);
        double baseMr = calculateBaseMagicResist(enemyStatic, enemy.level);

        // 2. Scansiona l'inventario del target nemico per sommare l'apporto degli oggetti comprati
        InventoryStats inventory = parseEnemyInventory(enemy, parser);

        // 3. Somma l'Armor e la MR derivate unicamente dal calcolo statico del campione specifico + item
        double totalArmor = baseArmor + inventory.bonusArmor;
        double totalMr = baseMr + inventory.bonusMr;

        // 4. Applica la penetrazione/lethality di Ezreal sulle difese totali calcolate del bersaglio
        double effectiveArmor = DamageEngine.calculateEffectiveResistance(totalArmor, "PHYSICAL", liveStats);
        double effectiveMr = DamageEngine.calculateEffectiveResistance(totalMr, "MAGIC", liveStats);

        // LOG 3: Scomposizione dettagliata basata solo sui file statici locali
        LoggerUtils.logTargetDefenses(enemy.level, baseArmor, inventory.bonusArmor, effectiveArmor,
                baseMr, inventory.bonusMr, effectiveMr, inventory.hasPlatedSteelcaps);

        // 5. Esegue il calcolo dinamico sui singoli slot del kit di abilità di Ezreal dividendo gli effetti
        calculateKitDamages(results, myStaticChamp, liveData, liveStats, myLevel, effectiveArmor, effectiveMr, inventory.hasPlatedSteelcaps);

        return results;
    }

    /**
     * Calcola l'Armor leggendo DIRETTAMENTE dall'oggetto passato dal metodo principale.
     */
    private double calculateBaseArmor(ChampionData.Champion enemyStatic, int enemyLevel) {
        if (enemyStatic.stats.containsKey("armor")) {
            ChampionData.StatValue armorStat = enemyStatic.stats.get("armor");
            double growthFactor = calculateGrowthFormula(enemyLevel);

            // Formula di Riot: Flat (Livello 1) + (PerLevel * Moltiplicatore di crescita non lineare)
            return Math.floor(armorStat.flat + (armorStat.perLevel * growthFactor));
        }
        return 30.0;
    }

    /**
     * Calcola la MR leggendo DIRETTAMENTE dall'oggetto passato dal metodo principale.
     */
    private double calculateBaseMagicResist(ChampionData.Champion enemyStatic, int enemyLevel) {
        if (enemyStatic.stats.containsKey("magicResistance")) {
            ChampionData.StatValue mrStat = enemyStatic.stats.get("magicResistance");
            double growthFactor = calculateGrowthFormula(enemyLevel);

            return Math.floor(mrStat.flat + (mrStat.perLevel * growthFactor));
        }
        return 30.0;
    }

    /**
     * Official LoL Wiki growth formula:
     * Statistic = base + growth × (level - 1) × (0.7025 + 0.0175 × (level - 1))
     * This returns the growth factor: (level - 1) × (0.7025 + 0.0175 × (level - 1))
     */
    private double calculateGrowthFormula(int level) {
        double levelUps = level - 1;
        return levelUps * (0.7025 + 0.0175 * levelUps);
    }

    /**
     * Analizza gli oggetti del nemico sommandone i valori difensivi statici.
     */
    private InventoryStats parseEnemyInventory(LiveGameData.LivePlayer enemy, GameDataParser parser) {
        InventoryStats inv = new InventoryStats();
        if (enemy.items == null) return inv;

        for (LiveGameData.LiveItem liveItem : enemy.items) {
            if (liveItem.itemID == 3047) {
                inv.hasPlatedSteelcaps = true;
            }

            ItemData.Item staticItem = parser.getItem(liveItem.itemID);
            if (staticItem != null && staticItem.stats != null) {
                if (staticItem.stats.containsKey("armor")) {
                    inv.bonusArmor += staticItem.stats.get("armor").flat * liveItem.count;
                }
                if (staticItem.stats.containsKey("magicResistance")) {
                    inv.bonusMr += staticItem.stats.get("magicResistance").flat * liveItem.count;
                }
            }
        }
        return inv;
    }

    /**
     * Elabora il kit del giocatore suddividendo ogni abilità nei rispettivi sotto-effetti JSON autonomi.
     */
    private void calculateKitDamages(
            List<CalculatedAbilityResult> results,
            ChampionData.Champion myStaticChamp,
            LiveGameData.Root liveData,
            LiveGameData.ChampionStats liveStats,
            int myLevel,
            double effectiveArmor,
            double effectiveMr,
            boolean hasPlatedSteelcaps) {

        for (Map.Entry<String, List<ChampionData.Ability>> entry : myStaticChamp.abilities.entrySet()) {
            String slot = entry.getKey();
            List<ChampionData.Ability> staticVariants = entry.getValue();
            LiveGameData.LiveAbility liveAbility = liveData.activePlayer.abilities.get(slot);

            if (liveAbility == null || staticVariants == null || staticVariants.isEmpty()) {
                continue;
            }

            for (ChampionData.Ability variant : staticVariants) {
                if (variant.effects == null) continue;

                for (int i = 0; i < variant.effects.size(); i++) {
                    ChampionData.Effect effect = variant.effects.get(i);
                    if (effect.leveling == null || effect.leveling.isEmpty()) continue;

                    double preMitigationDmg = DamageEngine.calculatePreMitigationDamage(effect, liveAbility.abilityLevel, liveStats, myStaticChamp, myLevel);
                    if (preMitigationDmg < 0.0) continue;

                    String dmgType = variant.damageType != null ? variant.damageType.toUpperCase() : "PHYSICAL_DAMAGE";
                    double targetedResist = dmgType.contains("MAGIC") ? effectiveMr : effectiveArmor;

                    double finalDamage = DamageEngine.calculatePostMitigationDamage(preMitigationDmg, targetedResist);

                    if (hasPlatedSteelcaps && variant.spellEffects != null && "proc".equalsIgnoreCase(variant.spellEffects.trim())) {
                        finalDamage = Math.floor(finalDamage * 0.88);
                    }

                    String displayName = variant.name + (variant.effects.size() > 1 ? " (Effetto " + (i + 1) + ")" : "");
                    boolean isProcAbility = (hasPlatedSteelcaps && variant.spellEffects != null && "proc".equalsIgnoreCase(variant.spellEffects.trim()));

                    LoggerUtils.logAbilityPipeline(slot, displayName, liveAbility.abilityLevel, preMitigationDmg, targetedResist, finalDamage, isProcAbility);

                    results.add(new CalculatedAbilityResult(slot, displayName, preMitigationDmg, finalDamage));
                }
            }
        }
    }

    private static class InventoryStats {
        double bonusArmor = 0.0;
        double bonusMr = 0.0;
        boolean hasPlatedSteelcaps = false;
    }
}