package org.lollivecalculator;

/**
 * Manages the current game session state.
 * <p>
 * Resolves the player's identity (champion name, team) from the live data
 * and exposes helper methods so that UI panels don't need to duplicate the
 * "find my champion" logic.
 * </p>
 */
public final class GameStateManager {

    private LiveGameData.Root currentData;
    private String activeChampionName;
    private String myTeam;
    private ChampionData.Champion myStaticChampion;

    /**
     * Process raw game data and resolve the active player's identity.
     *
     * @param data  parsed live game data from the API
     * @param parser parser with loaded champion/item data
     * @return true if the data was successfully processed
     */
    public boolean processGameData(LiveGameData.Root data, GameDataParser parser) {
        if (data == null || data.activePlayer == null || data.allPlayers == null) return false;

        this.currentData = data;

        // Resolve the active player identity
        String myRiotId = data.activePlayer.riotId;
        String myName = data.activePlayer.summonerName;

        String champName = null;
        String team = null;

        for (LiveGameData.LivePlayer p : data.allPlayers) {
            boolean matchesRiotId = (myRiotId != null && myRiotId.equals(p.riotId));
            boolean matchesName = (myName != null && myName.equals(p.summonerName));
            if (matchesRiotId || matchesName) {
                champName = p.championName;
                team = p.team;
                break;
            }
        }

        if (champName == null || team == null) return false;

        this.activeChampionName = champName;
        this.myTeam = team;
        this.myStaticChampion = parser.getChampion(champName);

        return this.myStaticChampion != null;
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public LiveGameData.Root getCurrentData() {
        return currentData;
    }

    public String getActiveChampionName() {
        return activeChampionName;
    }

    public String getMyTeam() {
        return myTeam;
    }

    public ChampionData.Champion getMyStaticChampion() {
        return myStaticChampion;
    }

    public LiveGameData.ChampionStats getLiveStats() {
        return currentData != null ? currentData.activePlayer.championStats : null;
    }

    public int getMyLevel() {
        return currentData != null ? currentData.activePlayer.level : 1;
    }

    /** Returns true if a live session is active and resolved. */
    public boolean hasValidSession() {
        return currentData != null && activeChampionName != null && myTeam != null && myStaticChampion != null;
    }
}