package com.quant.football;

/** Strict 1X2 odds / return-rate / relative Kelly calculations. */
public final class OddsKellyReturnEngine {
    private OddsKellyReturnEngine() {}

    public static final class Result {
        public final double returnRate;
        public final double margin;
        public final double[] fairProbability;
        public final double[] relativeIndex;
        public final double[] relativeSupport;

        Result(double returnRate, double margin, double[] fairProbability,
               double[] relativeIndex, double[] relativeSupport) {
            this.returnRate = returnRate;
            this.margin = margin;
            this.fairProbability = fairProbability;
            this.relativeIndex = relativeIndex;
            this.relativeSupport = relativeSupport;
        }
    }

    /**
     * @param odds complete decimal 1X2 odds [home, draw, away]
     * @param providerKelly provider-displayed Kelly indices, nullable. If supplied all 3 must be >0.
     */
    public static Result calculate(double[] odds, double[] providerKelly) {
        requireTriplet(odds, "odds");
        double sumInv = 0.0;
        for (double o : odds) {
            if (!(o > 1.0) || Double.isInfinite(o) || Double.isNaN(o)) {
                throw new IllegalArgumentException("Invalid decimal odds");
            }
            sumInv += 1.0 / o;
        }
        final double r = 1.0 / sumInv;
        final double[] p = new double[3];
        for (int i = 0; i < 3; i++) p[i] = (1.0 / odds[i]) / sumInv;

        final double[] j = new double[]{Double.NaN, Double.NaN, Double.NaN};
        final double[] e = new double[]{Double.NaN, Double.NaN, Double.NaN};
        if (providerKelly != null) {
            requireTriplet(providerKelly, "providerKelly");
            for (int i = 0; i < 3; i++) {
                if (!(providerKelly[i] > 0.0) || Double.isNaN(providerKelly[i])) {
                    throw new IllegalArgumentException("Invalid Kelly index");
                }
                j[i] = providerKelly[i] / r;       // K/R
                e[i] = -Math.log(j[i]);            // log(p_company/p_ref), if provider formula verified
            }
        }
        return new Result(r, sumInv - 1.0, p, j, e);
    }

    public static double probabilityPointMove(double current, double opening) {
        return (current - opening) * 100.0;
    }

    private static void requireTriplet(double[] x, String name) {
        if (x == null || x.length != 3) throw new IllegalArgumentException(name + " must contain H/D/A");
    }
}
