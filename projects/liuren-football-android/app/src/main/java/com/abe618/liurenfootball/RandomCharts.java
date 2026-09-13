package com.abe618.liurenfootball;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class RandomCharts {
    static final int POOL_SIZE = 740;
    static final long POOL_SEED = 740L;
    private static final LocalDate START = LocalDate.of(2020, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);
    private static final List<Models.ChartSeed> POOL = buildPool();
    private static final SecureRandom SECURE = new SecureRandom();

    private RandomCharts() {}

    static Models.ChartSeed randomPoolSeed(Integer fixedIndex) {
        int idx = fixedIndex == null ? SECURE.nextInt(POOL_SIZE) : Math.floorMod(fixedIndex, POOL_SIZE);
        Models.ChartSeed src = POOL.get(idx);
        Models.ChartSeed out = copy(src);
        out.modeName = "740局随机日期盘";
        out.poolIndex = idx;
        out.frozenAtMillis = System.currentTimeMillis();
        return out;
    }

    static Models.ChartSeed liveSeed() {
        LocalDateTime now = LocalDateTime.now();
        Models.ChartSeed out = new Models.ChartSeed();
        out.modeName = "今日活时盘";
        out.poolIndex = -1;
        out.date = now.toLocalDate();
        out.dayIndex = LiurenEngine.dayIndex(out.date);
        out.hourBranch = LiurenEngine.actualHourBranch(now.getHour());
        out.monthGeneral = LiurenEngine.monthGeneralForDate(out.date);
        out.frozenAtMillis = System.currentTimeMillis();
        return out;
    }

    static Models.ChartSeed liveSeedWithManualMonthGeneral(Integer monthGeneralOverride) {
        Models.ChartSeed out = liveSeed();
        if (monthGeneralOverride != null && monthGeneralOverride >= 0 && monthGeneralOverride < 12) {
            out.monthGeneral = monthGeneralOverride;
        }
        return out;
    }

    static List<Models.ChartSeed> previewPool() {
        return new ArrayList<>(POOL);
    }

    private static List<Models.ChartSeed> buildPool() {
        Random rnd = new Random(POOL_SEED);
        long days = ChronoUnit.DAYS.between(START, END) + 1;
        List<Models.ChartSeed> list = new ArrayList<>(POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            LocalDate d = START.plusDays(rnd.nextInt((int) days));
            Models.ChartSeed x = new Models.ChartSeed();
            x.modeName = "740局随机日期盘";
            x.poolIndex = i;
            x.date = d;
            x.dayIndex = LiurenEngine.dayIndex(d);
            x.hourBranch = rnd.nextInt(12);
            x.monthGeneral = LiurenEngine.monthGeneralForDate(d);
            x.frozenAtMillis = 0L;
            list.add(x);
        }
        return list;
    }

    private static Models.ChartSeed copy(Models.ChartSeed x) {
        Models.ChartSeed y = new Models.ChartSeed();
        y.modeName = x.modeName;
        y.poolIndex = x.poolIndex;
        y.date = x.date;
        y.dayIndex = x.dayIndex;
        y.hourBranch = x.hourBranch;
        y.monthGeneral = x.monthGeneral;
        y.frozenAtMillis = x.frozenAtMillis;
        return y;
    }
}
