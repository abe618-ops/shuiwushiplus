package com.shuiwushi.fengjiao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class FootballPrediction {
    private static final int[] STEM_TX = {9,8,7,6,5,9,8,7,6,5};
    private static final int[] BRANCH_TX = {9,8,7,6,5,4,9,8,7,6,5,4};
    private static final String[] TRIGRAMS = {"乾","兑","离","震","巽","坎","艮","坤"};

    public String fullResult;
    public String strength;
    public String halfFull;
    public int momentum;
    public int taiXuanVote;
    public int fiveElementVote;
    public int dayanVote;
    public int zhouyiVote;
    public int rawGoal;
    public int goalTotal;
    public String goalCoreRange;
    public String goalAgreement;
    public String primaryScore;
    public String alternateScores;
    public String relationDetail;
    public String numberDetail;
    public String seal;

    public static FootballPrediction from(FengJiaoEngine.Reading r) {
        FootballPrediction p = new FootballPrediction();
        p.calculateOutcome(r);
        p.calculateNumbers(r);
        p.calculateScores();
        p.seal = shortHash(String.format(Locale.ROOT,
                "%016X|%s|%s|%d|%s|%d,%d,%d,%d",
                r.seed, p.fullResult, p.halfFull, p.goalTotal, p.primaryScore,
                p.taiXuanVote, p.fiveElementVote, p.dayanVote, p.zhouyiVote));
        return p;
    }

    private void calculateOutcome(FengJiaoEngine.Reading r) {
        FengJiaoEngine.Element guest = r.dayNaYin.element;
        int time = relationScore(r.hourNaYin.element, guest);
        int wind = relationScore(r.wind.element, guest);
        int synergy = 0;
        if (r.hourNaYin.element == r.wind.element) {
            if (time > 0 && wind > 0) synergy = 1;
            if (time < 0 && wind < 0) synergy = -1;
        }
        momentum = time + wind + synergy;
        if (momentum >= 2) fullResult = "主胜";
        else if (momentum <= -2) fullResult = "客胜";
        else fullResult = "平";

        int abs = Math.abs(momentum);
        if ("平".equals(fullResult)) strength = abs == 0 ? "强胶着" : "偏胶着";
        else strength = abs >= 4 ? "强" : "中";

        String half;
        if (time >= 2) half = "胜";
        else if (time <= -2) half = "负";
        else half = "平";
        halfFull = half + "/" + shortResult(fullResult);

        relationDetail = "时: " + relationText(r.hourNaYin.element, guest) +
                "；风: " + relationText(r.wind.element, guest) +
                (synergy == 0 ? "" : "；时风同气修正" + (synergy > 0 ? "+1" : "-1"));
    }

    private void calculateNumbers(FengJiaoEngine.Reading r) {
        int ds = r.dayIndex % 10;
        int db = r.dayIndex % 12;
        int hs = r.hourIndex % 10;
        int hb = r.hourIndex % 12;
        int wb = r.wind.branchIndex;

        int dayTx = STEM_TX[ds] + BRANCH_TX[db];
        int hourTx = STEM_TX[hs] + BRANCH_TX[hb];
        int windTx = BRANCH_TX[wb];

        taiXuanVote = Math.floorMod(dayTx + hourTx + windTx, 7);

        int fiveRaw = riverNumber(r.dayNaYin.element) + riverNumber(r.hourNaYin.element) + riverNumber(r.wind.element);
        fiveElementVote = Math.floorMod(fiveRaw, 7);

        int remaining = 49 - (dayTx + hourTx);
        while (remaining <= 0) remaining += 49;
        dayanVote = Math.floorMod(remaining + windTx, 7);

        int upper = 1 + Math.floorMod(dayTx - 1, 8);
        int lower = 1 + Math.floorMod(hourTx + windTx - 1, 8);
        int moving = 1 + Math.floorMod(dayTx + hourTx + windTx - 1, 6);
        zhouyiVote = Math.floorMod(upper + lower + moving, 7);

        int[] votes = votes();
        int[] sorted = votes.clone();
        Arrays.sort(sorted);
        rawGoal = (sorted[1] + sorted[2]) / 2;
        goalCoreRange = sorted[1] == sorted[2] ? String.valueOf(sorted[1]) : sorted[1] + "–" + sorted[2];
        int centerGap = sorted[2] - sorted[1];
        int spread = sorted[3] - sorted[0];
        if (centerGap == 0 && spread <= 2) goalAgreement = "高度集中";
        else if (centerGap <= 1) goalAgreement = "较集中";
        else goalAgreement = "分歧较大";
        goalTotal = normalizeTotal(rawGoal, votes);

        numberDetail = String.format(Locale.CHINA,
                "太玄 %d+%d+%d→%d；五行 %d+%d+%d→%d；大衍 49-(%d+%d)+%d→%d；周易 %s%d/%s%d/动%d→%d",
                dayTx, hourTx, windTx, taiXuanVote,
                riverNumber(r.dayNaYin.element), riverNumber(r.hourNaYin.element), riverNumber(r.wind.element), fiveElementVote,
                dayTx, hourTx, windTx, dayanVote,
                TRIGRAMS[upper-1], upper, TRIGRAMS[lower-1], lower, moving, zhouyiVote);
    }

    private void calculateScores() {
        primaryScore = scoreFor(goalTotal);
        List<String> alt = new ArrayList<>();
        int[] offsets = {-1,1,-2,2,-3,3};
        for (int off : offsets) {
            int t = goalTotal + off;
            if (t < 0 || t > 6) continue;
            t = normalizeTotal(t, votes());
            String s = scoreFor(t);
            if (!s.equals(primaryScore) && !alt.contains(s)) alt.add(s);
            if (alt.size() == 2) break;
        }
        alternateScores = alt.isEmpty() ? "—" : (alt.size() == 1 ? alt.get(0) : alt.get(0) + " / " + alt.get(1));
    }

    private int[] votes() {
        return new int[]{taiXuanVote, fiveElementVote, dayanVote, zhouyiVote};
    }

    private int normalizeTotal(int total, int[] votes) {
        int t = Math.max(0, Math.min(6, total));
        if ("平".equals(fullResult)) {
            if ((t & 1) == 1) {
                int low = t - 1;
                int high = t + 1;
                if (high > 6) return low;
                if (low < 0) return high;
                return deviation(votes, low) <= deviation(votes, high) ? low : high;
            }
            return t;
        }
        return t == 0 ? 1 : t;
    }

    private String scoreFor(int total) {
        if ("平".equals(fullResult)) {
            int t = (total & 1) == 0 ? total : Math.max(0, total - 1);
            return (t/2) + ":" + (t/2);
        }
        int t = Math.max(1, total);
        int targetMargin = Math.abs(momentum) >= 5 ? 3 : (Math.abs(momentum) >= 4 ? 2 : 1);
        int best = -1;
        int dist = Integer.MAX_VALUE;
        for (int m=1; m<=Math.min(3,t); m++) {
            if (((t-m)&1) != 0) continue;
            int d = Math.abs(m-targetMargin);
            if (d < dist) { dist=d; best=m; }
        }
        if (best < 0) best = (t&1)==1 ? 1 : Math.min(2,t);
        int win = (t+best)/2;
        int lose = (t-best)/2;
        return "主胜".equals(fullResult) ? win+":"+lose : lose+":"+win;
    }

    private static int deviation(int[] votes, int target) {
        int d=0;
        for (int v:votes) d += Math.abs(v-target);
        return d;
    }

    private static int riverNumber(FengJiaoEngine.Element e) {
        switch (e) {
            case WATER: return 1;
            case FIRE: return 2;
            case WOOD: return 3;
            case METAL: return 4;
            case EARTH: return 5;
            default: return 0;
        }
    }

    private static int relationScore(FengJiaoEngine.Element host, FengJiaoEngine.Element guest) {
        if (host == guest) return 0;
        if (overcomes(host,guest)) return 2;
        if (overcomes(guest,host)) return -2;
        if (generates(guest,host)) return 1;
        if (generates(host,guest)) return -1;
        return 0;
    }

    private static String relationText(FengJiaoEngine.Element host, FengJiaoEngine.Element guest) {
        if (host == guest) return host.zh+"与"+guest.zh+"同气(0)";
        if (overcomes(host,guest)) return host.zh+"克"+guest.zh+"，主得势(+2)";
        if (overcomes(guest,host)) return guest.zh+"克"+host.zh+"，客得势(-2)";
        if (generates(guest,host)) return guest.zh+"生"+host.zh+"，主受生(+1)";
        return host.zh+"生"+guest.zh+"，主泄(-1)";
    }

    private static boolean generates(FengJiaoEngine.Element a, FengJiaoEngine.Element b) {
        return (a==FengJiaoEngine.Element.WOOD && b==FengJiaoEngine.Element.FIRE) ||
                (a==FengJiaoEngine.Element.FIRE && b==FengJiaoEngine.Element.EARTH) ||
                (a==FengJiaoEngine.Element.EARTH && b==FengJiaoEngine.Element.METAL) ||
                (a==FengJiaoEngine.Element.METAL && b==FengJiaoEngine.Element.WATER) ||
                (a==FengJiaoEngine.Element.WATER && b==FengJiaoEngine.Element.WOOD);
    }

    private static boolean overcomes(FengJiaoEngine.Element a, FengJiaoEngine.Element b) {
        return (a==FengJiaoEngine.Element.WOOD && b==FengJiaoEngine.Element.EARTH) ||
                (a==FengJiaoEngine.Element.EARTH && b==FengJiaoEngine.Element.WATER) ||
                (a==FengJiaoEngine.Element.WATER && b==FengJiaoEngine.Element.FIRE) ||
                (a==FengJiaoEngine.Element.FIRE && b==FengJiaoEngine.Element.METAL) ||
                (a==FengJiaoEngine.Element.METAL && b==FengJiaoEngine.Element.WOOD);
    }

    private static String shortResult(String r) {
        return "主胜".equals(r) ? "胜" : ("客胜".equals(r) ? "负" : "平");
    }

    private static String shortHash(String s) {
        try {
            byte[] b = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (int i=0;i<8;i++) out.append(String.format(Locale.ROOT,"%02x",b[i]));
            return out.toString();
        } catch (Exception e) {
            return "hash-error";
        }
    }
}
