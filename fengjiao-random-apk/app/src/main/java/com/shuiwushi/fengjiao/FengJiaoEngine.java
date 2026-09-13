package com.shuiwushi.fengjiao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.SplittableRandom;

public final class FengJiaoEngine {
    private FengJiaoEngine() {}

    public static final String[] STEMS = {"甲","乙","丙","丁","戊","己","庚","辛","壬","癸"};
    public static final String[] BRANCHES = {"子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥"};

    private static final String[] NAYIN_NAMES = {
            "海中金","炉中火","大林木","路旁土","剑锋金","山头火","涧下水","城头土","白蜡金","杨柳木",
            "泉中水","屋上土","霹雳火","松柏木","长流水","沙中金","山下火","平地木","壁上土","金箔金",
            "覆灯火","天河水","大驿土","钗钏金","桑柘木","大溪水","沙中土","天上火","石榴木","大海水"
    };
    private static final Element[] NAYIN_ELEMENTS = {
            Element.METAL,Element.FIRE,Element.WOOD,Element.EARTH,Element.METAL,Element.FIRE,Element.WATER,Element.EARTH,Element.METAL,Element.WOOD,
            Element.WATER,Element.EARTH,Element.FIRE,Element.WOOD,Element.WATER,Element.METAL,Element.FIRE,Element.WOOD,Element.EARTH,Element.METAL,
            Element.FIRE,Element.WATER,Element.EARTH,Element.METAL,Element.WOOD,Element.WATER,Element.EARTH,Element.FIRE,Element.WOOD,Element.WATER
    };

    public enum Element {
        WOOD("木","角"), FIRE("火","徵"), EARTH("土","宫"), METAL("金","商"), WATER("水","羽");
        public final String zh;
        public final String tone;
        Element(String zh, String tone) { this.zh = zh; this.tone = tone; }
    }

    public static final class NaYin {
        public final String name;
        public final Element element;
        public NaYin(String name, Element element) { this.name = name; this.element = element; }
    }

    public static final class Wind {
        public final int degrees;
        public final int branchIndex;
        public final String branch;
        public final String direction;
        public final Element element;
        public Wind(int degrees, int branchIndex, String branch, String direction, Element element) {
            this.degrees = degrees; this.branchIndex = branchIndex; this.branch = branch;
            this.direction = direction; this.element = element;
        }
    }

    public static final class Reading {
        public long seed;
        public LocalDateTime dateTime;
        public int dayIndex;
        public String dayGanzhi;
        public NaYin dayNaYin;
        public int hourIndex;
        public String hourGanzhi;
        public NaYin hourNaYin;
        public Wind wind;
        public String result;
        public String strength;
        public String reason;
        public String seal;

        public String compactText() {
            return String.format(Locale.CHINA,
                    "风角随机起盘\n种子：%016X\n随机时刻：%s\n客（日）：%s · %s（%s）\n主①（时）：%s · %s（%s）\n主②（风）：%d° %s%s · %s%s\n判定：%s%s\n理由：%s\n密封：%s",
                    seed, dateTime.toString().replace('T',' '), dayGanzhi, dayNaYin.name, dayNaYin.element.zh,
                    hourGanzhi, hourNaYin.name, hourNaYin.element.zh,
                    wind.degrees, wind.direction, wind.branch, wind.element.tone, wind.element.zh,
                    result, strength.isEmpty()?"":" · "+strength, reason, seal);
        }
    }

    public static Reading draw(long seed) {
        SplittableRandom r = new SplittableRandom(seed);
        LocalDate start = LocalDate.of(1900, 1, 1);
        LocalDate end = LocalDate.of(2099, 12, 31);
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate date = start.plusDays(r.nextLong(days));
        int minuteOfDay = r.nextInt(24 * 60);
        int hour = minuteOfDay / 60;
        int minute = minuteOfDay % 60;
        int degrees = r.nextInt(360);

        Reading out = new Reading();
        out.seed = seed;
        out.dateTime = LocalDateTime.of(date, LocalTime.of(hour, minute));
        out.dayIndex = daySexagenaryIndex(date);
        out.dayGanzhi = ganzhi(out.dayIndex);
        out.dayNaYin = nayin(out.dayIndex);

        int hourBranch = hourBranchIndex(hour);
        int dayStem = out.dayIndex % 10;
        int hourStem = ((dayStem % 5) * 2 + hourBranch) % 10;
        out.hourIndex = sexagenaryIndexForStemBranch(hourStem, hourBranch);
        out.hourGanzhi = STEMS[hourStem] + BRANCHES[hourBranch];
        out.hourNaYin = nayin(out.hourIndex);
        out.wind = wind(degrees);

        judge(out);
        out.seal = sha256Short(String.format(Locale.ROOT, "%016X|%s|%s|%s|%d|%s",
                seed, out.dateTime, out.dayGanzhi, out.hourGanzhi, degrees, out.result));
        return out;
    }

    public static int daySexagenaryIndex(LocalDate date) {
        long delta = ChronoUnit.DAYS.between(LocalDate.of(2000,1,7), date);
        return Math.floorMod((int)(delta % 60), 60);
    }

    public static String ganzhi(int index) {
        int i = Math.floorMod(index, 60);
        return STEMS[i % 10] + BRANCHES[i % 12];
    }

    public static NaYin nayin(int sexagenaryIndex) {
        int group = Math.floorMod(sexagenaryIndex, 60) / 2;
        return new NaYin(NAYIN_NAMES[group], NAYIN_ELEMENTS[group]);
    }

    public static int hourBranchIndex(int hour24) {
        return ((hour24 + 1) / 2) % 12;
    }

    private static int sexagenaryIndexForStemBranch(int stem, int branch) {
        for (int i = 0; i < 60; i++) {
            if (i % 10 == stem && i % 12 == branch) return i;
        }
        throw new IllegalArgumentException("invalid stem/branch parity");
    }

    public static Wind wind(int degrees) {
        int d = Math.floorMod(degrees, 360);
        int branch = ((d + 15) / 30) % 12;
        String[] dirs = {"北","东北偏北","东北偏东","东","东南偏东","东南偏南","南","西南偏南","西南偏西","西","西北偏西","西北偏北"};
        Element e;
        switch (branch) {
            case 0: case 6: e = Element.EARTH; break;
            case 1: case 2: case 7: case 8: e = Element.FIRE; break;
            case 3: case 9: e = Element.WATER; break;
            case 4: case 10: e = Element.METAL; break;
            case 5: case 11: e = Element.WOOD; break;
            default: throw new AssertionError();
        }
        return new Wind(d, branch, BRANCHES[branch], dirs[branch], e);
    }

    private static void judge(Reading x) {
        Element guest = x.dayNaYin.element;
        Element[] hosts = {x.hourNaYin.element, x.wind.element};
        int hostScore = 0, guestScore = 0;
        StringBuilder detail = new StringBuilder();
        String[] labels = {"时","风"};
        for (int i=0;i<hosts.length;i++) {
            Element h = hosts[i];
            String rel;
            if (overcomes(h, guest)) { hostScore++; rel = h.zh + "克" + guest.zh + "，主得势"; }
            else if (overcomes(guest, h)) { guestScore++; rel = guest.zh + "克" + h.zh + "，客得势"; }
            else if (generates(h, guest)) rel = h.zh + "生" + guest.zh + "，相生取和";
            else if (generates(guest, h)) rel = guest.zh + "生" + h.zh + "，相生取和";
            else rel = h.zh + "与" + guest.zh + "同气，取和";
            if (i>0) detail.append("；");
            detail.append(labels[i]).append(": ").append(rel);
        }

        if (hostScore == 2) { x.result = "主胜倾向"; x.strength = "强"; }
        else if (guestScore == 2) { x.result = "客胜倾向"; x.strength = "强"; }
        else if (hostScore > guestScore) { x.result = "主胜倾向"; x.strength = "偏"; }
        else if (guestScore > hostScore) { x.result = "客胜倾向"; x.strength = "偏"; }
        else { x.result = "和局/胶着倾向"; x.strength = (hostScore == 1 ? "主客分歧" : "相生同气"); }
        if (x.hourNaYin.element == x.wind.element && hostScore == 2) {
            x.strength = "双主同气·强";
        }
        x.reason = detail.toString();
    }

    private static boolean generates(Element a, Element b) {
        return (a==Element.WOOD && b==Element.FIRE) ||
                (a==Element.FIRE && b==Element.EARTH) ||
                (a==Element.EARTH && b==Element.METAL) ||
                (a==Element.METAL && b==Element.WATER) ||
                (a==Element.WATER && b==Element.WOOD);
    }

    private static boolean overcomes(Element a, Element b) {
        return (a==Element.WOOD && b==Element.EARTH) ||
                (a==Element.EARTH && b==Element.WATER) ||
                (a==Element.WATER && b==Element.FIRE) ||
                (a==Element.FIRE && b==Element.METAL) ||
                (a==Element.METAL && b==Element.WOOD);
    }

    private static String sha256Short(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i=0;i<8;i++) hex.append(String.format(Locale.ROOT, "%02x", b[i]));
            return hex.toString();
        } catch (Exception e) {
            return "hash-error";
        }
    }
}
