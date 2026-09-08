package com.quant.football;

/**
 * Football Fusion V3 frozen configuration.
 * Experimental divination modules are NEVER allowed to alter calibrated formal probabilities
 * until a forward validation version explicitly promotes them.
 */
public final class FusionV3Config {
    private FusionV3Config() {}

    public static final String SYSTEM_VERSION = "football-fusion-v3.0-apk";
    public static final String PRIORITY_BOOKMAKER = "12BET";
    public static final String SETTLEMENT = "REGULAR_TIME_PLUS_STOPPAGE";
    public static final String LEGACY_NUMBER_RULESET = "dual_meihua_v1_sum_mod6";

    public static final boolean REROLL_SAME_EVENT = false;
    public static final double FORMAL_DIVINATION_WEIGHT = 0.0;

    public static final int SCORE_GRID_MAX = 12;
    public static final double TAIL_EPSILON = 1e-8;

    public static final String[] SNAPSHOT_NODES = {
            "OPEN", "T-24H", "T-6H", "T-60M", "T-15M", "FINAL_PRE_KICKOFF"
    };
}
