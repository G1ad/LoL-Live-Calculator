package org.lollivecalculator.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Centralized application configuration.
 * <p>
 * Singleton that loads from {@code config.properties} in this order:
 * <ol>
 *   <li>Classpath resource {@code /config.properties} (works when running via Maven)</li>
 *   <li>File system path {@code config.properties} (works when running as packaged JAR)</li>
 *   <li>Hardcoded defaults (always works)</li>
 * </ol>
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
        Properties props = loadProperties();

        // ── LoL API ──────────────────────────────────────────────────────
        this.lolApiHost         = props.getProperty("lol.api.host", "127.0.0.1");
        this.lolApiPort         = parseInt(props.getProperty("lol.api.port", "2999"), 2999);
        this.lolApiLiveGamePath = props.getProperty("lol.api.livegame.path",
                "/liveclientdata/allgamedata");

        // ── Data sources ─────────────────────────────────────────────────
        this.championsUrl = props.getProperty("data.champions.url",
                "http://cdn.merakianalytics.com/riot/lol/resources/latest/en-US/champions.json");
        this.itemsUrl = props.getProperty("data.items.url",
                "http://cdn.merakianalytics.com/riot/lol/resources/latest/en-US/items.json");

        // ── Local files ──────────────────────────────────────────────────
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

    /**
     * Tries to load config.properties from:
     * 1. Classpath (works with Maven: mvn compile exec:java)
     * 2. File system (works with: java -jar app.jar)
     * Falls back to empty properties (all defaults).
     */
    private static Properties loadProperties() {
        Properties props = new Properties();

        // Try classpath first (src/main/resources/config.properties → target/classes/config.properties)
        try (InputStream is = AppConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                props.load(is);
                System.out.println(" [AppConfig] Loaded config.properties from classpath");
                return props;
            }
        } catch (Exception ignored) {
            // Fall through to file system attempt
        }

        // Try file system (same directory as working dir)
        Path fsPath = Paths.get("config.properties");
        if (Files.exists(fsPath)) {
            try (InputStream is = Files.newInputStream(fsPath)) {
                props.load(is);
                System.out.println(" [AppConfig] Loaded config.properties from " + fsPath.toAbsolutePath());
                return props;
            } catch (IOException ignored) {
                // Fall through to defaults
            }
        }

        System.out.println(" [AppConfig] No config.properties found, using defaults");
        return props;
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

    /** Returns the primary path: data/champions.json */
    public Path getChampionsLocalPath()    { return Paths.get(localDataDir, localChampionsFile); }
    /** Returns the primary path: data/items.json */
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