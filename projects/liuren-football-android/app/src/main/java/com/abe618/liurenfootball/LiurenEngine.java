package com.abe618.liurenfootball;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class LiurenEngine {
    static final String[] STEMS = {"甲","乙","丙","丁","戊","己","庚","辛","壬","癸"};
    static final String[] BRANCHES = {"子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥"};
    static final String[] ZODIAC = {"鼠","牛","虎","兔","龙","蛇","马","羊","猴","鸡","狗","猪"};
    static final String[] GENERALS = {"贵人","螣蛇","朱雀","六合","勾陈","青龙","天空","白虎","太常","玄武","太阴","天后"};
    static final String[] ELEMENTS = {"木","火","土","金","水"};

    static final int[] STEM_ELEMENT = {0,0,1,1,2,2,3,3,4,4};
    static final int[] BRANCH_ELEMENT = {4,2,0,0,2,1,1,2,3,3,2,4};
    static final int[] STEM_LODGE = {2,4,5,7,5,7,8,10,11,1};
    static final int[] SIX_COMBINE = {1,0,11,10,9,8,7,6,5,4,3,2};
    static final int[] CLASH = {6,7,8,9,10,11,0,1,2,3,4,5};
    static final int[] PUNISH = {3,10,5,0,4,8,6,1,2,9,7,11};
    private static final LocalDate JIA_ZI_BASE = LocalDate.of(2000, 1, 7);

    private LiurenEngine() {}

    static Models.Chart build(Models.ChartSeed seed) {
        Models.Chart c = new Models.Chart();
        c.seed = seed;
        c.dayStem = Math.floorMod(seed.dayIndex, 10);
        c.dayBranch = Math.floorMod(seed.dayIndex, 12);
        c.monthBuild = SIX_COMBINE[seed.monthGeneral];

        for (int p = 0; p < 12; p++) {
            c.skyAt[p] = Math.floorMod(seed.monthGeneral - seed.hourBranch + p, 12);
        }

        int firstLowerBranch = STEM_LODGE[c.dayStem];
        int firstUpper = c.skyAt[firstLowerBranch];
        int secondUpper = c.skyAt[firstUpper];
        int thirdUpper = c.skyAt[c.dayBranch];
        int fourthUpper = c.skyAt[thirdUpper];
        c.courses[0] = new Models.Course(0, firstLowerBranch, c.dayStem, firstUpper, STEMS[c.dayStem]);
        c.courses[1] = new Models.Course(1, firstUpper, null, secondUpper, BRANCHES[firstUpper]);
        c.courses[2] = new Models.Course(2, c.dayBranch, null, thirdUpper, BRANCHES[c.dayBranch]);
        c.courses[3] = new Models.Course(3, thirdUpper, null, fourthUpper, BRANCHES[thirdUpper]);

        boolean day = isDayHour(seed.hourBranch);
        c.nobleBranch = nobleBranch(c.dayStem, day);
        int nobleEarth = inverseSky(c.skyAt, c.nobleBranch);
        c.nobleForward = nobleEarth >= 3 && nobleEarth <= 8;
        for (int p = 0; p < 12; p++) {
            int dist = c.nobleForward ? Math.floorMod(p - nobleEarth, 12) : Math.floorMod(nobleEarth - p, 12);
            int sky = c.skyAt[p];
            c.generalOnSky[sky] = dist;
        }

        boolean fuyin = seed.monthGeneral == seed.hourBranch;
        boolean fanyin = seed.monthGeneral == Math.floorMod(seed.hourBranch + 6, 12);
        if (fuyin) buildFuYin(c);
        else if (fanyin) buildFanYin(c);
        else buildOrdinary(c);
        return c;
    }

    private static void buildOrdinary(Models.Chart c) {
        CandidateResult cr = findThiefControl(c);
        if (cr.found) {
            c.initial = cr.initial;
            c.pattern = cr.pattern;
            yinGodTransmissions(c);
            return;
        }

        cr = findRemoteControl(c);
        if (cr.found) {
            c.initial = cr.initial;
            c.pattern = cr.pattern;
            yinGodTransmissions(c);
            return;
        }

        if (isBaZhuanDay(c.dayStem, c.dayBranch) && distinctUpperCount(c) <= 2) {
            boolean yang = isYangStem(c.dayStem);
            c.initial = yang ? Math.floorMod(c.courses[0].upperBranch + 2, 12)
                    : Math.floorMod(c.courses[3].upperBranch - 2, 12);
            c.pattern = "八专";
            yinGodTransmissions(c);
            return;
        }

        if (isBieZe(c)) {
            if (isYangStem(c.dayStem)) {
                int partnerStem = stemCombinePartner(c.dayStem);
                c.initial = c.skyAt[STEM_LODGE[partnerStem]];
            } else {
                c.initial = Math.floorMod(c.dayBranch - 4, 12);
            }
            c.middle = c.courses[0].upperBranch;
            c.finalTransmission = c.courses[0].upperBranch;
            c.pattern = "别责";
            return;
        }

        if (isYangStem(c.dayStem)) {
            c.initial = c.skyAt[9];
            c.middle = c.courses[2].upperBranch;
            c.finalTransmission = c.courses[0].upperBranch;
            c.pattern = "昴星·虎视";
        } else {
            c.initial = inverseSky(c.skyAt, 9);
            c.middle = c.courses[0].upperBranch;
            c.finalTransmission = c.courses[2].upperBranch;
            c.pattern = "昴星·冬蛇掩目";
        }
    }

    private static void buildFuYin(Models.Chart c) {
        CandidateResult cr = findThiefControl(c);
        if (cr.found) {
            c.initial = cr.initial;
            c.pattern = "伏吟·" + cr.pattern;
        } else {
            c.initial = isYangStem(c.dayStem) ? c.courses[0].upperBranch : c.courses[2].upperBranch;
            c.pattern = "伏吟";
        }

        int next = PUNISH[c.initial];
        if (next == c.initial) c.middle = isYangStem(c.dayStem) ? c.courses[2].upperBranch : c.courses[0].upperBranch;
        else c.middle = next;
        int last = PUNISH[c.middle];
        c.finalTransmission = (last == c.middle) ? CLASH[c.middle] : last;
    }

    private static void buildFanYin(Models.Chart c) {
        CandidateResult cr = findThiefControl(c);
        if (cr.found) {
            c.initial = cr.initial;
            c.pattern = "返吟·" + cr.pattern;
            yinGodTransmissions(c);
            return;
        }
        c.initial = c.courses[2].upperBranch;
        c.middle = CLASH[c.initial];
        c.finalTransmission = CLASH[c.middle];
        c.pattern = "返吟·无克";
    }

    private static void yinGodTransmissions(Models.Chart c) {
        c.middle = c.skyAt[c.initial];
        c.finalTransmission = c.skyAt[c.middle];
    }

    private static CandidateResult findThiefControl(Models.Chart c) {
        List<Models.Course> thief = new ArrayList<>();
        List<Models.Course> control = new ArrayList<>();
        for (Models.Course course : c.courses) {
            int lowerEl = lowerElement(course);
            int upperEl = BRANCH_ELEMENT[course.upperBranch];
            if (controls(lowerEl, upperEl)) thief.add(course);
            else if (controls(upperEl, lowerEl)) control.add(course);
        }
        List<Models.Course> candidates = !thief.isEmpty() ? thief : control;
        if (candidates.isEmpty()) return CandidateResult.none();
        String basePattern = !thief.isEmpty() ? "重审" : "元首";
        Models.Course chosen = chooseBiYongSheHai(c, candidates);
        String pattern = candidates.size() == 1 ? basePattern : (chosenChoiceWasBiYong(c, candidates) ? "知一" : "涉害");
        return new CandidateResult(true, chosen.upperBranch, pattern);
    }

    private static boolean chosenChoiceWasBiYong(Models.Chart c, List<Models.Course> candidates) {
        int parity = c.dayStem % 2;
        int count = 0;
        for (Models.Course course : candidates) if (course.upperBranch % 2 == parity) count++;
        return count == 1;
    }

    private static Models.Course chooseBiYongSheHai(Models.Chart c, List<Models.Course> candidates) {
        if (candidates.size() == 1) return candidates.get(0);
        int parity = c.dayStem % 2;
        List<Models.Course> biyong = new ArrayList<>();
        for (Models.Course course : candidates) if (course.upperBranch % 2 == parity) biyong.add(course);
        if (biyong.size() == 1) return biyong.get(0);
        List<Models.Course> use = biyong.isEmpty() ? candidates : biyong;

        int max = Integer.MIN_VALUE;
        List<Models.Course> deepest = new ArrayList<>();
        for (Models.Course course : use) {
            int d = harmDepth(c, course);
            if (d > max) {
                max = d;
                deepest.clear();
                deepest.add(course);
            } else if (d == max) deepest.add(course);
        }
        if (deepest.size() == 1) return deepest.get(0);

        List<Models.Course> meng = new ArrayList<>();
        for (Models.Course course : deepest) if (isMeng(course.lowerBranch)) meng.add(course);
        if (meng.size() == 1) return meng.get(0);
        List<Models.Course> tie = meng.isEmpty() ? deepest : meng;

        boolean yang = isYangStem(c.dayStem);
        for (Models.Course course : tie) {
            if (yang && course.index <= 1) return course;
            if (!yang && course.index >= 2) return course;
        }
        return tie.get(0);
    }

    private static int harmDepth(Models.Chart c, Models.Course course) {
        int candidateEl = BRANCH_ELEMENT[course.upperBranch];
        int p = course.lowerBranch;
        int depth = 0;
        for (int steps = 0; steps < 12; steps++) {
            int earthEl = BRANCH_ELEMENT[p];
            int skyEl = BRANCH_ELEMENT[c.skyAt[p]];
            if (controls(earthEl, candidateEl)) depth++;
            if (controls(skyEl, candidateEl)) depth++;
            if (p == course.upperBranch) break;
            p = (p + 1) % 12;
        }
        return depth;
    }

    private static CandidateResult findRemoteControl(Models.Chart c) {
        int stemEl = STEM_ELEMENT[c.dayStem];
        List<Models.Course> hao = new ArrayList<>();
        List<Models.Course> dan = new ArrayList<>();
        for (Models.Course course : c.courses) {
            int upEl = BRANCH_ELEMENT[course.upperBranch];
            if (controls(upEl, stemEl)) hao.add(course);
            else if (controls(stemEl, upEl)) dan.add(course);
        }
        if (!hao.isEmpty()) return new CandidateResult(true, chooseByParityThenOrder(c, hao).upperBranch, "蒿矢");
        if (!dan.isEmpty()) return new CandidateResult(true, chooseByParityThenOrder(c, dan).upperBranch, "弹射");
        return CandidateResult.none();
    }

    private static Models.Course chooseByParityThenOrder(Models.Chart c, List<Models.Course> list) {
        int parity = c.dayStem % 2;
        for (Models.Course course : list) if (course.upperBranch % 2 == parity) return course;
        return list.get(0);
    }

    static int dayIndex(LocalDate date) {
        long days = ChronoUnit.DAYS.between(JIA_ZI_BASE, date);
        return Math.floorMod((int)(days % 60), 60);
    }

    static int actualHourBranch(int hour24) {
        return Math.floorMod((hour24 + 1) / 2, 12);
    }

    static int monthGeneralForDate(LocalDate d) {
        int m = d.getMonthValue();
        int day = d.getDayOfMonth();
        if ((m == 1 && day >= 20) || (m == 2 && day < 18)) return 0;
        if ((m == 2 && day >= 18) || (m == 3 && day < 20)) return 11;
        if ((m == 3 && day >= 20) || (m == 4 && day < 20)) return 10;
        if ((m == 4 && day >= 20) || (m == 5 && day < 21)) return 9;
        if ((m == 5 && day >= 21) || (m == 6 && day < 21)) return 8;
        if ((m == 6 && day >= 21) || (m == 7 && day < 23)) return 7;
        if ((m == 7 && day >= 23) || (m == 8 && day < 23)) return 6;
        if ((m == 8 && day >= 23) || (m == 9 && day < 23)) return 5;
        if ((m == 9 && day >= 23) || (m == 10 && day < 23)) return 4;
        if ((m == 10 && day >= 23) || (m == 11 && day < 22)) return 3;
        if ((m == 11 && day >= 22) || (m == 12 && day < 22)) return 2;
        return 1;
    }

    static int branchOfYear(int year) {
        return Math.floorMod(year - 1984, 12);
    }

    static String zodiacOfYear(int year) {
        return ZODIAC[branchOfYear(year)];
    }

    static Integer branchFromYearOrZodiacToken(String token) {
        if (token == null) return null;
        token = token.trim();
        if (token.isEmpty()) return null;
        try {
            int y = Integer.parseInt(token.replaceAll("[^0-9-]", ""));
            if (y >= 1000 && y <= 3000) return branchOfYear(y);
        } catch (Exception ignored) {}
        for (int i = 0; i < ZODIAC.length; i++) {
            if (token.contains(ZODIAC[i]) || token.contains(BRANCHES[i])) return i;
        }
        return null;
    }

    static String dayName(Models.Chart c) {
        return STEMS[c.dayStem] + BRANCHES[c.dayBranch];
    }

    static String transmissionName(Models.Chart c) {
        return BRANCHES[c.initial] + " → " + BRANCHES[c.middle] + " → " + BRANCHES[c.finalTransmission];
    }

    static String fourCoursesText(Models.Chart c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            Models.Course q = c.courses[i];
            if (i > 0) sb.append("  ");
            sb.append(i + 1).append("课 ").append(q.lowerLabel).append("→").append(BRANCHES[q.upperBranch]);
        }
        return sb.toString();
    }

    static String generalNameOnBranch(Models.Chart c, int branch) {
        return GENERALS[c.generalOnSky[branch]];
    }

    static boolean controls(int a, int b) {
        return (a == 0 && b == 2) || (a == 2 && b == 4) || (a == 4 && b == 1)
                || (a == 1 && b == 3) || (a == 3 && b == 0);
    }

    static boolean generates(int a, int b) {
        return (a == 0 && b == 1) || (a == 1 && b == 2) || (a == 2 && b == 3)
                || (a == 3 && b == 4) || (a == 4 && b == 0);
    }

    static int inverseSky(int[] skyAt, int skyBranch) {
        for (int p = 0; p < 12; p++) if (skyAt[p] == skyBranch) return p;
        return 0;
    }

    static boolean isYangStem(int stem) { return stem % 2 == 0; }
    static boolean isDayHour(int branch) { return branch >= 3 && branch <= 8; }

    private static int lowerElement(Models.Course q) {
        return q.lowerStem != null ? STEM_ELEMENT[q.lowerStem] : BRANCH_ELEMENT[q.lowerBranch];
    }

    private static boolean isMeng(int branch) {
        return branch == 2 || branch == 5 || branch == 8 || branch == 11;
    }

    private static int distinctUpperCount(Models.Chart c) {
        Set<Integer> set = new HashSet<>();
        for (Models.Course q : c.courses) set.add(q.upperBranch);
        return set.size();
    }

    private static boolean isBieZe(Models.Chart c) {
        return distinctUpperCount(c) == 3 || c.courses[0].upperBranch == c.dayBranch;
    }

    private static boolean isBaZhuanDay(int stem, int branch) {
        String x = STEMS[stem] + BRANCHES[branch];
        return x.equals("甲寅") || x.equals("庚申") || x.equals("丁未") || x.equals("己未");
    }

    private static int stemCombinePartner(int stem) {
        switch (stem) {
            case 0: return 5;
            case 5: return 0;
            case 1: return 6;
            case 6: return 1;
            case 2: return 7;
            case 7: return 2;
            case 3: return 8;
            case 8: return 3;
            case 4: return 9;
            case 9: return 4;
            default: return 0;
        }
    }

    private static int nobleBranch(int stem, boolean day) {
        switch (stem) {
            case 0: case 4: case 6: return day ? 1 : 7;
            case 1: case 5: return day ? 0 : 8;
            case 2: case 3: return day ? 11 : 9;
            case 8: case 9: return day ? 3 : 5;
            case 7: return day ? 6 : 2;
            default: return 1;
        }
    }

    private static final class CandidateResult {
        final boolean found;
        final int initial;
        final String pattern;

        CandidateResult(boolean found, int initial, String pattern) {
            this.found = found;
            this.initial = initial;
            this.pattern = pattern;
        }

        static CandidateResult none() { return new CandidateResult(false, 0, ""); }
    }
}
