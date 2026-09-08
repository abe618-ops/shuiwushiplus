package com.quant.football;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Versioned experimental adapter for the folk "0-goal SP positioning" method.
 * This module intentionally does NOT claim a verified public formula or calibrated probability.
 * It stores observable SP relationships and exposes deterministic diagnostics for forward testing.
 */
public final class ZeroGoalSpExperiment {
    private ZeroGoalSpExperiment() {}

    public static final String RULESET_ID = "zero-goal-sp-public-clues-v0.1";

    public static final class Input {
        public final double zeroGoalSp;
        public final double[] totalGoalSp; // index 0 means 0 goals, 1 means 1 goal, etc.
        public final double integerMultipleTolerance;
        public Input(double zeroGoalSp, double[] totalGoalSp, double integerMultipleTolerance) {
            this.zeroGoalSp = zeroGoalSp;
            this.totalGoalSp = totalGoalSp;
            this.integerMultipleTolerance = integerMultipleTolerance;
        }
    }

    public static final class Result {
        public final String rulesetId;
        public final String status;
        public final List<Integer> nearIntegerMultipleGoals;
        public final String note;
        Result(String rulesetId, String status, List<Integer> goals, String note) {
            this.rulesetId = rulesetId;
            this.status = status;
            this.nearIntegerMultipleGoals = Collections.unmodifiableList(goals);
            this.note = note;
        }
    }

    public static Result inspect(Input in) {
        if (in == null || !(in.zeroGoalSp > 0.0) || in.totalGoalSp == null || in.totalGoalSp.length == 0) {
            return new Result(RULESET_ID, "insufficient_input", new ArrayList<>(), "缺少完整总进球SP");
        }
        double tol = in.integerMultipleTolerance > 0 ? in.integerMultipleTolerance : 0.035;
        ArrayList<Integer> hits = new ArrayList<>();
        for (int goals = 0; goals < in.totalGoalSp.length; goals++) {
            double sp = in.totalGoalSp[goals];
            if (!(sp > 0.0)) continue;
            double ratio = sp / in.zeroGoalSp;
            double nearest = Math.rint(ratio);
            if (nearest >= 1.0 && Math.abs(ratio - nearest) <= tol) hits.add(goals);
        }
        return new Result(
                RULESET_ID,
                "experimental",
                hits,
                "仅检查0球SP与其他总进球SP的近整数倍关系；区间判定需在取得可核验完整规则后另建ruleset，当前不进入正式概率。"
        );
    }
}
