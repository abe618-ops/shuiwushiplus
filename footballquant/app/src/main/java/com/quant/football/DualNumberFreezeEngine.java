package com.quant.football;

import java.security.SecureRandom;

/** Manual + automatic 3-digit axis. Keeps leading zeroes and never rerolls implicitly. */
public final class DualNumberFreezeEngine {
    private static final SecureRandom RNG = new SecureRandom();
    private DualNumberFreezeEngine() {}

    public static String generateAutoNumber() {
        return String.format(java.util.Locale.US, "%03d", RNG.nextInt(1000));
    }

    public static String normalizeManualNumber(String raw) {
        if (raw == null) throw new IllegalArgumentException("manual number missing");
        String s = raw.trim();
        if (!s.matches("\\d{1,3}")) throw new IllegalArgumentException("manual number must be 0-999");
        int value = Integer.parseInt(s);
        return String.format(java.util.Locale.US, "%03d", value);
    }

    public static MeihuaChart deriveLegacyMeihua(String threeDigits) {
        String n = normalizeManualNumber(threeDigits);
        int a = n.charAt(0) - '0';
        int b = n.charAt(1) - '0';
        int c = n.charAt(2) - '0';
        int upper = trigramIndex(a);
        int lower = trigramIndex(b);
        int moving = (a + b + c) % 6;
        if (moving == 0) moving = 6;
        return new MeihuaChart(n, upper, lower, moving);
    }

    private static int trigramIndex(int digit) {
        if (digit == 0) return 8; // Kun
        if (digit == 9) return 1; // Qian
        return digit;             // 1..8 innate-number mapping
    }

    public static final class MeihuaChart {
        public final String number;
        public final int upperTrigram;
        public final int lowerTrigram;
        public final int movingLine;
        public MeihuaChart(String number, int upperTrigram, int lowerTrigram, int movingLine) {
            this.number = number;
            this.upperTrigram = upperTrigram;
            this.lowerTrigram = lowerTrigram;
            this.movingLine = movingLine;
        }
    }
}
