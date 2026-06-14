package org.lollivecalculator.util;

import org.lollivecalculator.config.AppConfig;
import org.lollivecalculator.model.LiveGameData;

import java.io.PrintStream;

/**
 * Centralized logging utility with configurable verbosity and ANSI color support.
 * <p>
 * Respects {@link AppConfig#isVerboseLogging()} and {@link AppConfig#isAnsiColorsEnabled()}.
 * When verbose mode is off, only pipeline headlines are printed (no per-ability breakdowns).
 * When ANSI is off, all color codes are stripped.
 * </p>
 */
public final class LoggerUtils {

    private static final AppConfig CFG = AppConfig.getInstance();

    // ANSI color constants (only used when enabled)
    private static final String ANSI_RESET  = "\u001B[0m";
    private static final String ANSI_CYAN   = "\u001B[36m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED    = "\u001B[31m";
    private static final String ANSI_GREEN  = "\u001B[32m";
    private static final String ANSI_PURPLE = "\u001B[35m";

    // Actual output — empty strings when ANSI disabled
    private static final String RESET;
    private static final String CYAN;
    private static final String YELLOW;
    private static final String RED;
    private static final String GREEN;
    private static final String PURPLE;

    static {
        if (CFG.isAnsiColorsEnabled()) {
            RESET  = ANSI_RESET;
            CYAN   = ANSI_CYAN;
            YELLOW = ANSI_YELLOW;
            RED    = ANSI_RED;
            GREEN  = ANSI_GREEN;
            PURPLE = ANSI_PURPLE;
        } else {
            RESET  = "";
            CYAN   = "";
            YELLOW = "";
            RED    = "";
            GREEN  = "";
            PURPLE = "";
        }
    }

    private LoggerUtils() { }

    // ── Public API ──────────────────────────────────────────────────────

    /** Always printed — marks the start of a damage pipeline. */
    public static void logStartPipeline(String attacker, String target) {
        PrintStream o = System.out;
        o.println();
        o.println(CYAN + "══════════════════════════════════════════════════════════════════════════" + RESET);
        o.println(CYAN + " 🔄 ENGINE PIPELINE RUN: " + attacker + " ──> " + target + RESET);
        o.println(CYAN + "══════════════════════════════════════════════════════════════════════════" + RESET);
    }

    /** Always printed — summarizes attacker stats from the live frame. */
    public static void logAttackerStats(int level, LiveGameData.ChampionStats stats) {
        PrintStream o = System.out;
        o.println(YELLOW + " 👤 ATTACKER STATS:" + RESET);
        o.printf("  ├── Level: %d | Total AD: %.2f | AP: %.2f%n", level, stats.attackDamage, stats.abilityPower);
        o.printf("  └── Lethality: %.2f | Flat Pen: %.2f | %% Pen: %.1f%%%n",
                stats.physicalLethality, stats.armorPenetrationFlat,
                (1.0 - stats.armorPenetrationPercent) * 100);
    }

    /** Always printed — summarizes the target's defenses. */
    public static void logTargetDefenses(int level, double baseArmor, double bonusArmor, double effArmor,
                                         double baseMr, double bonusMr, double effMr, boolean steelcaps) {
        PrintStream o = System.out;
        o.println(RED + " 🛡️  TARGET DEFENSES:" + RESET);
        o.printf("  ├── Level: %d%n", level);
        o.printf("  ├── Armor: base=%.2f + items=%.2f → effective=%.2f%n", baseArmor, bonusArmor, effArmor);
        o.printf("  ├── MR:    base=%.2f + items=%.2f → effective=%.2f%n", baseMr, bonusMr, effMr);
        o.printf("  └── Plated Steelcaps: %s (-12%% on-hit)%n", steelcaps ? "YES" : "no");
    }

    /**
     * Per-ability log line. Only printed when {@code verbose} mode is on.
     * Suppressing this reduces console noise for players who just want final numbers.
     */
    public static void logAbilityPipeline(String slot, String name, int rank, double raw,
                                          double resist, double finalDmg, boolean isProc) {
        if (!CFG.isVerboseLogging()) return;

        PrintStream o = System.out;
        o.printf("%s[%s] %s (Rank %d)%s%n", CYAN, slot, name, rank, RESET);
        o.printf("  ├── Raw damage:  %s%.2f%s%n", YELLOW, raw, RESET);
        o.printf("  ├── Resistance:  %s%.2f%s%n", RED, resist, RESET);
        if (isProc) {
            o.printf("  ├── On-hit proc: %sYES (-12%%)%s%n", PURPLE, RESET);
        }
        o.printf("  └── Final:       %s%d%s%n", GREEN, (int) finalDmg, RESET);
    }
}