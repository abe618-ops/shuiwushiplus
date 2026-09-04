package com.abe618.basketballrandomtotal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NumberEngine {
    private NumberEngine() {}

    public static final class Result {
        public final String normalized;
        public final boolean over;
        public final int overWeight;
        public final int underWeight;
        public final double centerOffset;
        public final List<String> reasons;

        Result(String normalized, boolean over, int overWeight, double centerOffset, List<String> reasons) {
            this.normalized = normalized;
            this.over = over;
            this.overWeight = overWeight;
            this.underWeight = 100 - overWeight;
            this.centerOffset = centerOffset;
            this.reasons = reasons;
        }

        public double center(double baseline) {
            return baseline + centerOffset;
        }

        public String range(double baseline) {
            double c = center(baseline);
            return String.format(Locale.US, "%.1f～%.1f", c - 2.0, c + 2.0);
        }
    }

    public static Result analyze(String raw) {
        String normalized = raw == null ? "" : raw.replaceAll("[^0-9]", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("请输入至少1位数字");
        }
        if (normalized.length() > 18) {
            normalized = normalized.substring(0, 18);
        }

        int[] d = new int[normalized.length()];
        int odd = 0, even = 0, sum = 0;
        int score = 0;
        int rising = 0, falling = 0, equalHigh = 0;
        boolean has0 = false, has6 = false, has9 = false;
        int count7 = 0;
        List<String> reasons = new ArrayList<>();

        for (int i = 0; i < normalized.length(); i++) {
            int v = normalized.charAt(i) - '0';
            d[i] = v;
            sum += v;
            if ((v & 1) == 1) odd++; else even++;
            if (v == 0) has0 = true;
            if (v == 6) has6 = true;
            if (v == 9) has9 = true;
            if (v == 7) count7++;

            switch (v) {
                case 9: score += 5; break;
                case 8: score += 2; break;
                case 7: score += 3; break;
                case 6: score -= 1; break;
                case 4: score -= 2; break;
                case 2: score -= 1; break;
                default: break;
            }
        }

        int parityDiff = odd - even;
        score += parityDiff * 4;
        reasons.add(String.format(Locale.US, "奇偶结构：奇%d / 偶%d → %s", odd, even,
                parityDiff > 0 ? "偏放量" : parityDiff < 0 ? "偏收缩" : "中性"));

        if ((sum & 1) == 1) {
            score += 4;
            reasons.add("数字和=" + sum + "（奇）→ 增加大分动能");
        } else {
            score -= 2;
            reasons.add("数字和=" + sum + "（偶）→ 略作收束");
        }

        for (int i = 1; i < d.length; i++) {
            if (d[i] > d[i - 1]) {
                rising++;
                score += 2;
            } else if (d[i] < d[i - 1]) {
                falling++;
                score -= 2;
            } else if (d[i] >= 7) {
                equalHigh++;
                score += 3;
            } else {
                score -= 1;
            }
        }

        if (rising > falling) {
            reasons.add("数字走势：上升段较多 → 节奏倾向抬升");
        } else if (falling > rising) {
            reasons.add("数字走势：下降段较多 → 节奏倾向降速");
        } else {
            reasons.add("数字走势：升降相抵 → 看尾数与高位数决定");
        }
        if (equalHigh > 0) {
            reasons.add("高位重复 → 得分强度有延续性");
        }

        int last = d[d.length - 1];
        if (last == 9) {
            score += 6;
            reasons.add("尾9：高位收尾 → 后程放量权重明显提高");
        } else if (last == 7) {
            score += 4;
            reasons.add("尾7：高位不收 → 偏大");
        } else if (last == 4) {
            score -= 4;
            reasons.add("尾4：低位收口 → 偏小");
        } else if (last == 6) {
            score -= 2;
            reasons.add("尾6：有收束作用");
        }

        for (int i = 1; i < d.length; i++) {
            if ((d[i - 1] == 0 && d[i] == 9) || (d[i - 1] == 9 && d[i] == 0)) {
                score += 5;
                reasons.add("0/9相邻：波动后冲高 → 增加大分权重");
                break;
            }
        }

        if (has0 && has6 && has9) {
            score += 3;
            reasons.add("0·6·9共现：先收/断档/再放量的波动结构");
        }

        boolean strictlyDescending = d.length >= 3;
        for (int i = 1; i < d.length; i++) {
            if (d[i] >= d[i - 1]) {
                strictlyDescending = false;
                break;
            }
        }
        if (strictlyDescending) {
            score -= 5;
            reasons.add("连续下降：后程降速信号加强");
        }

        if (count7 >= 2) {
            score += 2;
            reasons.add("双7/多7：高位持续，不易快速熄火");
        }

        int overWeight = clamp(50 + score, 20, 80);
        boolean over = overWeight >= 50;

        double offset = (overWeight - 50) * 0.15;
        if (last == 9) offset += 3.0;
        if (count7 >= 2) offset += 2.0;
        if (strictlyDescending) offset -= 1.0;
        if (has0 && has6 && has9) offset += 0.5;

        return new Result(normalized, over, overWeight, offset, reasons);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
