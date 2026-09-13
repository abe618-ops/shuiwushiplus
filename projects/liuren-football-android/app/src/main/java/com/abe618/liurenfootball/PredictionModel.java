package com.abe618.liurenfootball;

import java.util.Locale;

final class PredictionModel {
    private static final int[] DAY_DE = {2,8,5,11,5,2,8,5,11,5};
    private static final int[] DAY_LU = {2,3,5,6,5,6,8,9,11,0};

    private PredictionModel() {}

    static Models.PredictionResult predict(Models.Chart chart, Models.InputMeta meta, String mode) {
        Models.PredictionResult r = new Models.PredictionResult();
        r.mode = mode;
        r.chart = chart;
        r.location = locate(chart, meta);
        r.homeScore = 0;
        r.drawScore = 3.0;
        r.awayScore = 0;

        boolean homeBranch = r.location.homeIsBranch;
        int homeUpper = homeBranch ? chart.courses[2].upperBranch : chart.courses[0].upperBranch;
        int awayUpper = homeBranch ? chart.courses[0].upperBranch : chart.courses[2].upperBranch;
        int homeBaseEl = homeBranch ? LiurenEngine.BRANCH_ELEMENT[chart.dayBranch] : LiurenEngine.STEM_ELEMENT[chart.dayStem];
        int awayBaseEl = homeBranch ? LiurenEngine.STEM_ELEMENT[chart.dayStem] : LiurenEngine.BRANCH_ELEMENT[chart.dayBranch];

        double hSeason = seasonScore(chart.monthBuild, homeUpper);
        double aSeason = seasonScore(chart.monthBuild, awayUpper);
        add(r, "月令旺衰", meta.homeName + "上神" + LiurenEngine.BRANCHES[homeUpper] + seasonName(chart.monthBuild, homeUpper)
                        + "；" + meta.awayName + "上神" + LiurenEngine.BRANCHES[awayUpper] + seasonName(chart.monthBuild, awayUpper),
                hSeason, 0, aSeason);

        int hEl = LiurenEngine.BRANCH_ELEMENT[homeUpper];
        int aEl = LiurenEngine.BRANCH_ELEMENT[awayUpper];
        if (LiurenEngine.controls(aEl, hEl)) add(r, "上神互克", "客上克主上", -2, 0, 4);
        else if (LiurenEngine.controls(hEl, aEl)) add(r, "上神互克", "主上克客上", 4, 0, -2);
        else if (LiurenEngine.generates(aEl, hEl)) add(r, "上神相生", "客上生主上", 1.5, 0, -0.5);
        else if (LiurenEngine.generates(hEl, aEl)) add(r, "上神相生", "主上生客上", -0.5, 0, 1.5);
        else if (hEl == aEl) add(r, "上神比和", "双方上神同五行", 0, 1.5, 0);

        if (LiurenEngine.controls(hEl, awayBaseEl)) add(r, "克对方本位", "主上神克客位", 2, 0, 0);
        if (LiurenEngine.controls(aEl, homeBaseEl)) add(r, "克对方本位", "客上神克主位", 0, 0, 2);

        int initEl = LiurenEngine.BRANCH_ELEMENT[chart.initial];
        int lastEl = LiurenEngine.BRANCH_ELEMENT[chart.finalTransmission];
        if (LiurenEngine.controls(initEl, lastEl)) add(r, "三传初末", "初传(客)克末传(主)", -1, 0, 3);
        else if (LiurenEngine.controls(lastEl, initEl)) add(r, "三传初末", "末传(主)克初传(客)", 3, 0, -1);
        else if (LiurenEngine.generates(initEl, lastEl)) add(r, "三传初末", "初传生末传", 1.5, 0, 0);
        else if (LiurenEngine.generates(lastEl, initEl)) add(r, "三传初末", "末传生初传", 0, 0, 1.5);
        else if (initEl == lastEl) add(r, "三传初末", "初末比和", 0, 1, 0);

        int initGen = chart.generalOnSky[chart.initial];
        int lastGen = chart.generalOnSky[chart.finalTransmission];
        add(r, "传神乘将", "初传乘" + LiurenEngine.GENERALS[initGen] + "；末传乘" + LiurenEngine.GENERALS[lastGen],
                generalDelta(lastGen), 0, generalDelta(initGen));

        int hGen = chart.generalOnSky[homeUpper];
        int aGen = chart.generalOnSky[awayUpper];
        add(r, "上神乘将", meta.homeName + "上神乘" + LiurenEngine.GENERALS[hGen] + "；" + meta.awayName + "上神乘" + LiurenEngine.GENERALS[aGen],
                2 * generalDelta(hGen), 0, 2 * generalDelta(aGen));

        int gouBranch = branchRiddenByGeneral(chart, 4);
        int xuanBranch = branchRiddenByGeneral(chart, 9);
        double gouSeason = smallSeasonScore(chart.monthBuild, gouBranch);
        double xuanSeason = smallSeasonScore(chart.monthBuild, xuanBranch);
        add(r, "勾陈玄武旺衰", "勾陈乘" + LiurenEngine.BRANCHES[gouBranch] + "；玄武乘" + LiurenEngine.BRANCHES[xuanBranch],
                gouSeason, 0, xuanSeason);
        int gouEl = LiurenEngine.BRANCH_ELEMENT[gouBranch];
        int xuanEl = LiurenEngine.BRANCH_ELEMENT[xuanBranch];
        if (LiurenEngine.controls(gouEl, xuanEl)) add(r, "勾玄生克", "勾陈所乘克玄武所乘", 2, 0, 0);
        else if (LiurenEngine.controls(xuanEl, gouEl)) add(r, "勾玄生克", "玄武所乘克勾陈所乘", 0, 0, 2);

        double hLuDe = 0, aLuDe = 0;
        if (homeUpper == DAY_LU[chart.dayStem]) hLuDe += 1.5;
        if (homeUpper == DAY_DE[chart.dayStem]) hLuDe += 1.5;
        if (awayUpper == DAY_LU[chart.dayStem]) aLuDe += 1.5;
        if (awayUpper == DAY_DE[chart.dayStem]) aLuDe += 1.5;
        if (hLuDe != 0 || aLuDe != 0) add(r, "禄德", "日禄/日德临双方上神检查", hLuDe, 0, aLuDe);

        int horse = travelHorse(chart.dayBranch);
        double hHorse = homeUpper == horse ? 0.5 : 0;
        double aHorse = awayUpper == horse ? 0.5 : 0;
        if (hHorse != 0 || aHorse != 0) add(r, "驿马", "驿马=" + LiurenEngine.BRANCHES[horse], hHorse, 0, aHorse);

        int moveEarth = movingBranch(chart);
        int moveGod = chart.skyAt[moveEarth];
        int moveEl = LiurenEngine.BRANCH_ELEMENT[moveGod];
        double hMove = movementDelta(moveEl, homeBaseEl);
        double aMove = movementDelta(moveEl, awayBaseEl);
        add(r, "运移法", "运辰" + LiurenEngine.BRANCHES[moveEarth] + "，运神" + LiurenEngine.BRANCHES[moveGod], hMove, 0, aMove);

        String p = chart.pattern;
        if (p.contains("元首")) add(r, "课体", p + "利主", 1, 0, 0);
        else if (p.contains("重审")) add(r, "课体", p + "客先动", 0, 0, 1);
        else if (p.contains("蒿矢")) add(r, "课体", p, 0, 0, 0.5);
        else if (p.contains("弹射")) add(r, "课体", p, 0.5, 0, 0);
        else if (p.contains("伏吟")) add(r, "课体", p + "偏僵持", 0, 1.5, 0);
        else if (p.contains("返吟")) add(r, "课体", p + "偏反复", 0, 1, 0);
        else if (p.contains("知一") || p.contains("涉害") || p.contains("昴星") || p.contains("别责") || p.contains("八专"))
            add(r, "课体", p, 0, 0.5, 0);

        int stemUpper = chart.courses[0].upperBranch;
        int branchUpper = chart.courses[2].upperBranch;
        if (stemUpper == branchUpper || LiurenEngine.SIX_COMBINE[stemUpper] == branchUpper) {
            add(r, "合冲", "干上与支上相合/相同", 0, 2, 0);
        } else if (LiurenEngine.CLASH[stemUpper] == branchUpper) {
            double hBonus = 0, aBonus = 0;
            double stemS = seasonScore(chart.monthBuild, stemUpper);
            double branchS = seasonScore(chart.monthBuild, branchUpper);
            if (stemS > branchS) {
                if (homeBranch) aBonus = 1; else hBonus = 1;
            } else if (branchS > stemS) {
                if (homeBranch) hBonus = 1; else aBonus = 1;
            }
            add(r, "合冲", "干支上神相冲，旺者加权", hBonus, 0.5, aBonus);
        }

        double[] ps = softmax(r.homeScore, r.drawScore, r.awayScore, 3.0);
        r.pHome = ps[0]; r.pDraw = ps[1]; r.pAway = ps[2];
        if (r.pHome >= r.pDraw && r.pHome >= r.pAway) r.verdict = "主胜";
        else if (r.pAway >= r.pDraw) r.verdict = "客胜";
        else r.verdict = "平局";
        r.extendedSummary = extended(r);
        return r;
    }

    static Models.LocationResult locate(Models.Chart c, Models.InputMeta meta) {
        Models.LocationResult out = new Models.LocationResult();
        vote(out, "D0 常规兵占", 1.0, true, "干=客、支=主");
        applyYearVote(out, c, meta.homeFoundedYear, true, 0.8, "D1 主队成立年支");
        applyYearVote(out, c, meta.awayFoundedYear, false, 0.8, "D2 客队成立年支");
        applyYearVote(out, c, meta.homeCoachBirthYear, true, 0.6, "D3 主教练生年支");
        applyYearVote(out, c, meta.awayCoachBirthYear, false, 0.6, "D4 客教练生年支");
        out.homeIsBranch = out.branchVotes >= out.stemVotes;
        double total = out.branchVotes + out.stemVotes;
        out.consistency = total > 0 ? Math.max(out.branchVotes, out.stemVotes) / total : 1;
        return out;
    }

    private static void applyYearVote(Models.LocationResult out, Models.Chart c, Integer year, boolean isHomeEntity, double weight, String source) {
        if (year == null) {
            out.votes.add(new Models.LocationVote(source, weight, "弃权", "未提供年份"));
            return;
        }
        int branch = LiurenEngine.branchOfYear(year);
        boolean onStem = c.courses[0].upperBranch == branch || c.courses[1].upperBranch == branch;
        boolean onBranch = c.courses[2].upperBranch == branch || c.courses[3].upperBranch == branch;
        String z = year + " " + LiurenEngine.ZODIAC[branch] + "(" + LiurenEngine.BRANCHES[branch] + ")";
        if (onStem && onBranch) {
            out.votes.add(new Models.LocationVote(source, weight, "弃权", z + " 同现干支课，冲突"));
            return;
        }
        if (!onStem && !onBranch) {
            out.votes.add(new Models.LocationVote(source, weight, "弃权", z + " 不上四课"));
            return;
        }
        boolean homeIsBranch;
        if (isHomeEntity) homeIsBranch = onBranch;
        else homeIsBranch = onStem;
        vote(out, source, weight, homeIsBranch, z + (onStem ? " 落干上" : " 落支上"));
    }

    private static void vote(Models.LocationResult out, String source, double weight, boolean homeIsBranch, String detail) {
        if (homeIsBranch) out.branchVotes += weight; else out.stemVotes += weight;
        out.votes.add(new Models.LocationVote(source, weight, homeIsBranch ? "主=支" : "主=干", detail));
    }

    private static void add(Models.PredictionResult r, String item, String detail, double h, double d, double a) {
        r.homeScore += h; r.drawScore += d; r.awayScore += a;
        r.scoreLines.add(new Models.ScoreLine(item, detail, h, d, a));
    }

    private static double seasonScore(int monthBuild, int branch) {
        int m = LiurenEngine.BRANCH_ELEMENT[monthBuild];
        int e = LiurenEngine.BRANCH_ELEMENT[branch];
        if (e == m) return 3;
        if (LiurenEngine.generates(m, e)) return 1.5;
        if (LiurenEngine.generates(e, m)) return 0;
        if (LiurenEngine.controls(e, m)) return -1.5;
        if (LiurenEngine.controls(m, e)) return -3;
        return 0;
    }

    private static String seasonName(int monthBuild, int branch) {
        double s = seasonScore(monthBuild, branch);
        if (s == 3) return "旺";
        if (s == 1.5) return "相";
        if (s == 0) return "休";
        if (s == -1.5) return "囚";
        return "死";
    }

    private static double smallSeasonScore(int monthBuild, int branch) {
        double s = seasonScore(monthBuild, branch);
        if (s == 3) return 1;
        if (s == 1.5) return 0.5;
        if (s < 0) return -1;
        return 0;
    }

    private static double generalDelta(int generalIndex) {
        return (generalIndex == 0 || generalIndex == 3 || generalIndex == 5 || generalIndex == 8 || generalIndex == 10 || generalIndex == 11) ? 1 : -1;
    }

    private static int branchRiddenByGeneral(Models.Chart c, int generalIndex) {
        for (int branch = 0; branch < 12; branch++) if (c.generalOnSky[branch] == generalIndex) return branch;
        return 0;
    }

    private static int travelHorse(int dayBranch) {
        if (dayBranch == 8 || dayBranch == 0 || dayBranch == 4) return 2;
        if (dayBranch == 2 || dayBranch == 6 || dayBranch == 10) return 8;
        if (dayBranch == 5 || dayBranch == 9 || dayBranch == 1) return 11;
        return 5;
    }

    private static int movingBranch(Models.Chart c) {
        int a = c.initial % 2, b = c.middle % 2, d = c.finalTransmission % 2;
        int base;
        int dir;
        if (a == 1 && b == 0 && d == 1) { base = c.initial; dir = -1; }
        else if (a == 0 && b == 0 && d == 0) { base = c.middle; dir = -1; }
        else if (a == 0 && b == 1 && d == 0) { base = c.finalTransmission; dir = 1; }
        else if (a == 1 && b == 1 && d == 1) { base = c.middle; dir = 1; }
        else {
            base = c.middle;
            boolean meng = base == 2 || base == 5 || base == 8 || base == 11;
            boolean zhong = base == 0 || base == 3 || base == 6 || base == 9;
            dir = (meng || zhong) ? -1 : 1;
        }
        return Math.floorMod(base + dir * 4, 12);
    }

    private static double movementDelta(int moveEl, int baseEl) {
        if (LiurenEngine.controls(moveEl, baseEl)) return -1.5;
        if (LiurenEngine.generates(moveEl, baseEl)) return 1;
        return 0;
    }

    private static double[] softmax(double h, double d, double a, double tau) {
        double mh = h / tau, md = d / tau, ma = a / tau;
        double max = Math.max(mh, Math.max(md, ma));
        double eh = Math.exp(mh - max), ed = Math.exp(md - max), ea = Math.exp(ma - max);
        double sum = eh + ed + ea;
        return new double[]{eh / sum, ed / sum, ea / sum};
    }

    private static String extended(Models.PredictionResult r) {
        double max = Math.max(r.pHome, Math.max(r.pDraw, r.pAway));
        String risk = max >= 0.62 ? "方向较集中" : (max >= 0.48 ? "中等分歧" : "高分歧，宜防反向");
        String side = r.location.consistency < 0.60 ? "主客定位存疑" : "主客定位较稳";
        return String.format(Locale.US, "%s；%s；定位一致性 %.0f%%", risk, side, r.location.consistency * 100);
    }
}
