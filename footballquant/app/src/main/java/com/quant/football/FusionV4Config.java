package com.quant.football;

/** Frozen configuration for the V4 multi-consensus experiment. */
public final class FusionV4Config {
    private FusionV4Config() {}

    public static final String SYSTEM_VERSION = "football-fusion-v4.0-multiconsensus";
    public static final String PRIORITY_BOOKMAKER = "12BET";
    public static final String SETTLEMENT = "REGULAR_TIME_PLUS_STOPPAGE";
    public static final String LEGACY_NUMBER_RULESET = "dual_meihua_v1_sum_mod6";
    public static final String TAIXUAN_RULESET = "taixuan_81_state_mod81_v1";
    public static final String STEM_BRANCH_RULESET = "day_ganzhi_epoch_2000-01-07_jiazi_v1";
    public static final String ZHONGLV_RULESET = "branch_to_12lu_v1_experimental";

    /** Traditional/number families remain shadow features until forward validation promotes them. */
    public static final double FORMAL_TRADITION_WEIGHT = 0.0;

    /** Experimental-only coefficients. They produce labels, never calibrated probabilities. */
    public static final double EXP_MEIHUA_WEIGHT = 1.00;
    public static final double EXP_FIVE_ELEMENT_WEIGHT = 0.55;
    public static final double EXP_TAIXUAN_WEIGHT = 0.35;
    public static final double EXP_STEM_BRANCH_WEIGHT = 0.15;

    public static final double FIRST_HALF_GOAL_SHARE = 0.45;
    public static final int SCORE_GRID_MAX = 12;
    public static final boolean REROLL_SAME_EVENT = false;
}
