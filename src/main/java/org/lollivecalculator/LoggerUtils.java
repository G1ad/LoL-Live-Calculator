package org.lollivecalculator;

public class LoggerUtils {

    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";    // Intestazioni / Struttura
    private static final String YELLOW = "\u001B[33m";  // Statistiche Attaccante
    private static final String RED = "\u001B[31m";     // Resistenze / Difese Target
    private static final String GREEN = "\u001B[32m";   // Danni Finali Effettivi
    private static final String PURPLE = "\u001B[35m";  // Passive / Item speciali

    public static void logStartPipeline(String attacker, String target) {
        System.out.println("\n" + CYAN + "========================================================================" + RESET);
        System.out.println(CYAN + " 🔄 ENGINE PIPELINE RUN: " + attacker + " ──> " + target + RESET);
        System.out.println(CYAN + "========================================================================" + RESET);
    }

    /**
     * Traccia le statistiche offensive estratte in tempo reale dall'API client.
     */
    public static void logAttackerStats(int level, LiveGameData.ChampionStats stats) {
        System.out.println(YELLOW + " 👤 STATS ATTACCANTE (LIVE FRAME):" + RESET);
        System.out.printf("  ├── Livello: %d | AD Totale: %.2f | AP Totale: %.2f\n", level, stats.attackDamage, stats.abilityPower);
        System.out.printf("  └── Lethality: %.2f | ArmorPen Piatta: %.2f | ArmorPen: %.1f%%\n",
                stats.physicalLethality, stats.armorPenetrationFlat, (1.0 - stats.armorPenetrationPercent) * 100);
        System.out.println(CYAN + "  ──────────────────────────────────────────────────────────────────────" + RESET);
    }

    /**
     * Scompone analiticamente la composizione della difesa del bersaglio.
     */
    public static void logTargetDefenses(int level, double baseArmor, double bonusArmor, double effArmor,
                                         double baseMr, double bonusMr, double effMr, boolean steelcaps) {
        System.out.println(RED + " 🛡️  PROFILO DIFENSIVO TARGET (LIVE FRAME):" + RESET);
        System.out.printf("  ├── Livello Target: %d\n", level);
        System.out.printf("  ├── ARMOR: Base: %.2f + Item: %.2f (Totale: %.2f) ──> Effettiva: %.2f\n",
                baseArmor, bonusArmor, (baseArmor + bonusArmor), effArmor);
        System.out.printf("  ├── M.RES: Base: %.2f + Item: %.2f (Totale: %.2f) ──> Effettiva: %.2f\n",
                baseMr, bonusMr, (baseMr + bonusMr), effMr);
        System.out.printf("  └── Plated Steelcaps in Inventario: %s%s%s\n",
                steelcaps ? PURPLE + "SI" : RED + "NO", RESET, steelcaps ? PURPLE + " (-12% danni On-Hit)" : "");
        System.out.println(CYAN + "========================================================================" + RESET);
    }

    /**
     * Stampa i singoli stadi del danno di una variante di abilità.
     */
    public static void logAbilityPipeline(String slot, String name, int rank, double raw, double resist, double finalDmg, boolean isProc) {
        System.out.printf("%s[%s] %s (Rank %d)%s\n", CYAN, slot, name, rank, RESET);
        System.out.printf("  ├── 💥 Danno Grezzo (Pre-Mitigazione): %s%.2f%s\n", YELLOW, raw, RESET);
        System.out.printf("  ├── 🛡️  Resistenza Applicata:          %s%.2f%s\n", RED, resist, RESET);

        if (isProc) {
            System.out.printf("  ├── 👟 Passiva On-Hit (Steelcaps):     %sATTIVA (-12%%, Math.floor)%s\n", PURPLE, RESET);
        }

        System.out.printf("  └── 🎯 Danno A Schermo (Post-Mit):     %s%d%s\n", GREEN, (int)finalDmg, RESET);
        System.out.println(CYAN + "  ──────────────────────────────────────────────────────────────────────" + RESET);
    }
}