package org.lollivecalculator.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Centralized application configuration.
 * <p>
 * Singleton that loads from {@code config.properties} if it exists on the
 * classpath, or falls back to sensible defaults. All hardcoded URLs, paths,
 * ports and tunables live here.
 * </p>
 */
public final class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();

    // ── LoL Live Client API ──────────────────────────────────────────────
    private final String lolApiHost;
    private final int    lolApiPort;
    private final String lolApiLiveGamePath;

    // ── Data download sources ────────────────────────────────────────────
    private final String championsUrl;
    private final String itemsUrl;

    // ── Local file paths (relative to working directory) ──────────────────
    private final String localDataDir;
    private final String localChampionsFile;
    private final String localItemsFile;

    // ── Polling / timing ─────────────────────────────────────────────────
    private final long   pollIntervalMs;
    private final long   pollBackoffInitialMs;
    private final long   pollBackoffMaxMs;

    // ── Logging ──────────────────────────────────────────────────────────
    private final boolean verboseLogging;
    private final boolean enableAnsiColors;

    private AppConfig() {
        Properties props = new Properties();
        try (var is = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ignored) { /* use defaults */ }

        // ── LoL API ──────────────────────────────────────────────────────
        this.lolApiHost         = props.getProperty("lol.api.host", "127.0.0.1");
        this.lolApiPort         = parseInt(props.getProperty("lol.api.port", "2999"), 2999);
        this.lolApiLiveGamePath = props.getProperty("lol.api.livegame.path", "/liveclientdata/allgamedata");

        // ── Data sources ─────────────────────────────────────────────────
        this.championsUrl = props.getProperty("data.champions.url",
                "http://cdn.merakianalytics.com/riot/lol/resources/latest/en-US/champions.json");
        this.itemsUrl = props.getProperty("data.items.url",
                "http://cdn.merakianalytics.com/riot/lol/resources/latest/en-US/items.json");

        // ── Local files ───────────────────────────────────────────────────
        this.localDataDir       = props.getProperty("data.local.dir", "data");
        this.localChampionsFile = props.getProperty("data.local.champions", "champions.json");
        this.localItemsFile     = props.getProperty("data.local.items", "items.json");

        // ── Polling ──────────────────────────────────────────────────────
        this.pollIntervalMs       = parseLong(props.getProperty("poll.interval.ms", "1500"), 1500L);
        this.pollBackoffInitialMs = parseLong(props.getProperty("poll.backoff.initial.ms", "1000"), 1000L);
        this.pollBackoffMaxMs     = parseLong(props.getProperty("poll.backoff.max.ms", "30000"), 30000L);

        // ── Logging ──────────────────────────────────────────────────────
        this.verboseLogging  = Boolean.parseBoolean(props.getProperty("log.verbose", "true"));
        this.enableAnsiColors = Boolean.parseBoolean(props.getProperty("log.ansi", "true"));
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public String getLolApiUrl() {
        return String.format("https://%s:%d%s", lolApiHost, lolApiPort, lolApiLiveGamePath);
    }

    public String getChampionsUrl()        { return championsUrl; }
    public String getItemsUrl()            { return itemsUrl; }

    public Path getChampionsLocalPath()    { return Paths.get(localDataDir, localChampionsFile); }
    public Path getItemsLocalPath()        { return Paths.get(localDataDir, localItemsFile); }
    public String getLocalDataDir()        { return localDataDir; }

    public long getPollIntervalMs()        { return pollIntervalMs; }
    public long getPollBackoffInitialMs()  { return pollBackoffInitialMs; }
    public long getPollBackoffMaxMs()      { return pollBackoffMaxMs; }

    public boolean isVerboseLogging()      { return verboseLogging; }
    public boolean isAnsiColorsEnabled()   { return enableAnsiColors; }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (Exception e) { return def; }
    }
}