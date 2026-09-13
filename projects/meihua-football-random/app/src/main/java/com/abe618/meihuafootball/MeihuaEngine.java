package com.abe618.meihuafootball;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MeihuaEngine {
    private MeihuaEngine() {}

    public static final class Trigram {
        public final int index;
        public final String name;
        public final String element;
        public final int[] lines; // bottom -> top

        Trigram(int index, String name, String element, int[] lines) {
            this.index = index;
            this.name = name;
            this.element = element;
            this.lines = lines;
        }
    }

    public static final class Stage {
        public final String relation;
        public final int score; // +2 upper advantage, -2 lower advantage, 0 neutral

        Stage(String relation, int score) {
            this.relation = relation;
            this.score = score;
        }

        public String label() {
            if (score > 0) return "上优";
            if (score < 0) return "下优";
            return "比和";
        }
    }

    public static final class Prediction {
        public String digits;
        public Trigram upper;
        public Trigram lower;
        public Trigram mutualUpper;
        public Trigram mutualLower;
        public Trigram changedUpper;
        public Trigram changedLower;
        public int movingLine;
        public String originalHex;
        public String mutualHex;
        public String changedHex;
        public Stage originalStage;
        public Stage mutualStage;
        public Stage changedStage;
        public double structureScore;
        public String path;
        public String pathClass;

        // A track: direction-free structure
        public String winShape;
        public String drawTendency;
        public String sizeSignal;
        public String oddEvenSignal;
        public int totalGoalsSignal;
        public String bttsSignal;
        public String cleanSheetSignal;
        public int goalDiffSignal;
        public String scoreShape;
        public String structureNote;

        // B track: home/away polarity
        public String b1;
        public String b2;
        public String polarityStatus;
        public String finalDirection;
        public String unbeaten;
        public String halfFull;
        public String orientedScore;
        public String confidence;

        public String compactSummary() {
            return "随机数 " + digits + "｜" + originalHex + " → " + mutualHex + " → " + changedHex
                    + "｜" + movingLine + "爻动\n"
                    + "A轨：" + winShape + "；" + sizeSignal + "；" + oddEvenSignal + "；总进球信号 " + totalGoalsSignal
                    + "；" + bttsSignal + "；比分形态 " + scoreShape + "\n"
                    + "B轨：" + finalDirection + "；" + unbeaten + "；半全场 " + halfFull
                    + "；比分 " + orientedScore + "；置信度 " + confidence;
        }
    }

    private static final Trigram[] TRIGRAMS = new Trigram[9];
    private static final Map<String, String> HEX = new HashMap<>();
    private static final SecureRandom RNG = new SecureRandom();

    static {
        TRIGRAMS[1] = new Trigram(1, "乾", "金", new int[]{1,1,1});
        TRIGRAMS[2] = new Trigram(2, "兑", "金", new int[]{1,1,0});
        TRIGRAMS[3] = new Trigram(3, "离", "火", new int[]{1,0,1});
        TRIGRAMS[4] = new Trigram(4, "震", "木", new int[]{1,0,0});
        TRIGRAMS[5] = new Trigram(5, "巽", "木", new int[]{0,1,1});
        TRIGRAMS[6] = new Trigram(6, "坎", "水", new int[]{0,1,0});
        TRIGRAMS[7] = new Trigram(7, "艮", "土", new int[]{0,0,1});
        TRIGRAMS[8] = new Trigram(8, "坤", "土", new int[]{0,0,0});

        put("乾","乾","乾为天"); put("坤","坤","坤为地");
        put("坎","震","水雷屯"); put("艮","坎","山水蒙");
        put("坎","乾","水天需"); put("乾","坎","天水讼");
        put("坤","坎","地水师"); put("坎","坤","水地比");
        put("巽","乾","风天小畜"); put("乾","兑","天泽履");
        put("坤","乾","地天泰"); put("乾","坤","天地否");
        put("乾","离","天火同人"); put("离","乾","火天大有");
        put("坤","艮","地山谦"); put("震","坤","雷地豫");
        put("兑","震","泽雷随"); put("艮","巽","山风蛊");
        put("坤","兑","地泽临"); put("巽","坤","风地观");
        put("离","震","火雷噬嗑"); put("艮","离","山火贲");
        put("艮","坤","山地剥"); put("坤","震","地雷复");
        put("乾","震","天雷无妄"); put("艮","乾","山天大畜");
        put("艮","震","山雷颐"); put("兑","巽","泽风大过");
        put("坎","坎","坎为水"); put("离","离","离为火");
        put("兑","艮","泽山咸"); put("震","巽","雷风恒");
        put("乾","艮","天山遁"); put("震","乾","雷天大壮");
        put("离","坤","火地晋"); put("坤","离","地火明夷");
        put("巽","离","风火家人"); put("离","兑","火泽睽");
        put("坎","艮","水山蹇"); put("震","坎","雷水解");
        put("艮","兑","山泽损"); put("巽","震","风雷益");
        put("兑","乾","泽天夬"); put("乾","巽","天风姤");
        put("兑","坤","泽地萃"); put("坤","巽","地风升");
        put("兑","坎","泽水困"); put("坎","巽","水风井");
        put("兑","离","泽火革"); put("离","巽","火风鼎");
        put("震","震","震为雷"); put("艮","艮","艮为山");
        put("巽","艮","风山渐"); put("震","兑","雷泽归妹");
        put("震","离","雷火丰"); put("离","艮","火山旅");
        put("巽","巽","巽为风"); put("兑","兑","兑为泽");
        put("巽","坎","风水涣"); put("坎","兑","水泽节");
        put("巽","兑","风泽中孚"); put("震","艮","雷山小过");
        put("坎","离","水火既济"); put("离","坎","火水未济");
    }

    private static void put(String upper, String lower, String name) {
        HEX.put(upper + "-" + lower, name);
    }

    public static String randomDigits() {
        return String.format(Locale.US, "%03d", RNG.nextInt(1000));
    }

    public static Prediction analyze(String digits) {
        if (digits == null || !digits.matches("\\d{3}")) {
            throw new IllegalArgumentException("必须是三位数字，例如 293、055、934");
        }

        int a = digits.charAt(0) - '0';
        int b = digits.charAt(1) - '0';
        int c = digits.charAt(2) - '0';
        Trigram upper = fromDigit(a);
        Trigram lower = fromDigit(b);
        int moving = (a + b + c) % 6;
        if (moving == 0) moving = 6;

        int[] lines = new int[]{
                lower.lines[0], lower.lines[1], lower.lines[2],
                upper.lines[0], upper.lines[1], upper.lines[2]
        };

        Trigram mutualLower = fromLines(lines[1], lines[2], lines[3]);
        Trigram mutualUpper = fromLines(lines[2], lines[3], lines[4]);

        int[] changed = lines.clone();
        changed[moving - 1] = 1 - changed[moving - 1];
        Trigram changedLower = fromLines(changed[0], changed[1], changed[2]);
        Trigram changedUpper = fromLines(changed[3], changed[4], changed[5]);

        Prediction p = new Prediction();
        p.digits = digits;
        p.upper = upper;
        p.lower = lower;
        p.mutualUpper = mutualUpper;
        p.mutualLower = mutualLower;
        p.changedUpper = changedUpper;
        p.changedLower = changedLower;
        p.movingLine = moving;
        p.originalHex = hexName(upper, lower);
        p.mutualHex = hexName(mutualUpper, mutualLower);
        p.changedHex = hexName(changedUpper, changedLower);
        p.originalStage = relation(upper.element, lower.element);
        p.mutualStage = relation(mutualUpper.element, mutualLower.element);
        p.changedStage = relation(changedUpper.element, changedLower.element);
        p.structureScore = 0.35 * p.originalStage.score + 0.25 * p.mutualStage.score + 0.40 * p.changedStage.score;
        p.path = p.originalStage.label() + " → " + p.mutualStage.label() + " → " + p.changedStage.label();
        p.pathClass = classifyPath(p.originalStage.score, p.mutualStage.score, p.changedStage.score);

        buildStructureTrack(p, a, b, c, lines, changed);
        buildPolarityTrack(p);
        return p;
    }

    private static void buildStructureTrack(Prediction p, int a, int b, int c, int[] lines, int[] changed) {
        int s1 = sign(p.originalStage.score);
        int s2 = sign(p.mutualStage.score);
        int s3 = sign(p.changedStage.score);
        boolean aba = s1 != 0 && s2 != 0 && s3 != 0 && s1 == s3 && s1 != s2;
        boolean weak = Math.abs(p.structureScore) < 0.60;
        boolean draw = weak || aba;

        if (draw) {
            p.winShape = "平局倾向 / 胜负不宜压死";
            p.drawTendency = aba ? "高（A→B→A来回结构）" : "中高（结构分接近0）";
        } else {
            p.winShape = "倾向分出胜负";
            p.drawTendency = Math.abs(p.structureScore) >= 1.30 ? "低" : "中";
        }

        int yangCount = 0;
        for (int x : lines) yangCount += x;
        for (int x : changed) yangCount += x;
        int goalRaw = (p.upper.index + p.lower.index + p.mutualUpper.index + p.mutualLower.index
                + p.changedUpper.index + p.changedLower.index + p.movingLine + c) % 6;
        if (!draw && goalRaw == 0) goalRaw = 1;
        p.totalGoalsSignal = goalRaw;

        int sizeSeed = (a * 7 + b * 5 + c * 3 + p.movingLine + yangCount) % 7;
        if (sizeSeed <= 2) p.sizeSignal = "小球倾向（2.5线）";
        else if (sizeSeed >= 4) p.sizeSignal = "大球倾向（2.5线）";
        else p.sizeSignal = "大小中性";

        int oddSeed = (a + 2 * b + 3 * c + p.movingLine + yangCount) & 1;
        p.oddEvenSignal = oddSeed == 1 ? "单数倾向" : "双数倾向";

        int bttsSeed = (p.upper.index + p.lower.index + p.changedUpper.index * 2
                + p.changedLower.index * 3 + c + p.movingLine) % 4;
        boolean btts = bttsSeed >= 2;
        p.bttsSignal = btts ? "双方进球：是" : "双方进球：否";
        p.cleanSheetSignal = btts ? "零封信号：低" : "零封信号：较高";

        if (draw) p.goalDiffSignal = 0;
        else p.goalDiffSignal = Math.abs(p.structureScore) >= 1.30 ? 2 : 1;

        p.scoreShape = scoreShape(draw, btts, goalRaw, p.goalDiffSignal);
        List<String> notes = new ArrayList<>();
        if (aba) notes.add("来回反转：降低直接胜负置信度，防平");
        if (s1 == s3 && s2 == 0 && s1 != 0) notes.add("A→0→A：中段比和不视为反转");
        if (s3 == 0 && s1 != 0 && s2 == s1) notes.add("A→A→0：末段比和只表示无新增反转力量");
        if ((goalRaw & 1) == 0 && p.oddEvenSignal.startsWith("单")) notes.add("总进球与单双独立信号冲突");
        if ((goalRaw & 1) == 1 && p.oddEvenSignal.startsWith("双")) notes.add("总进球与单双独立信号冲突");
        p.structureNote = notes.isEmpty() ? "结构信号一致" : join(notes, "；");
    }

    private static void buildPolarityTrack(Prediction p) {
        // B1: lower=home, upper=away. structureScore is upper-positive, so negate it for home-positive.
        double b1Home = -p.structureScore;
        // B2: moving 1-3 => upper=body/home; moving 4-6 => lower=body/home.
        double b2Home = p.movingLine <= 3 ? p.structureScore : -p.structureScore;
        p.b1 = resultFromHomeScore(b1Home);
        p.b2 = resultFromHomeScore(b2Home);

        if (p.b1.equals(p.b2)) {
            p.polarityStatus = "B1/B2同向";
            p.finalDirection = p.b1;
            p.confidence = Math.abs(b1Home) >= 1.30 ? "较高" : (Math.abs(b1Home) >= 0.60 ? "中" : "低");
        } else if ("平".equals(p.b1) || "平".equals(p.b2)) {
            String side = "平".equals(p.b1) ? p.b2 : p.b1;
            p.polarityStatus = "一轨判平，方向降级";
            p.finalDirection = side + "，重点防平";
            p.confidence = "低-中";
        } else {
            p.polarityStatus = "主客极性争议";
            p.finalDirection = "方向弃权，优先看A轨结构";
            p.confidence = "低";
        }

        if (p.finalDirection.startsWith("主胜")) p.unbeaten = "主队不败";
        else if (p.finalDirection.startsWith("客胜")) p.unbeaten = "客队不败";
        else if (p.finalDirection.startsWith("平")) p.unbeaten = "平局优先";
        else p.unbeaten = "不败方向不输出";

        if (p.polarityStatus.equals("B1/B2同向")) {
            double earlyHome = -((p.originalStage.score + p.mutualStage.score) / 2.0);
            p.halfFull = phaseResult(earlyHome) + "/" + phaseResult(b1Home);
        } else {
            p.halfFull = "不强行输出";
        }
        p.orientedScore = orientShape(p.scoreShape, p.finalDirection);
    }

    public static String review(Prediction p, int home, int away) {
        int total = home + away;
        String actualResult = home > away ? "主胜" : (home < away ? "客胜" : "平");
        String actualSize = total >= 3 ? "大球" : "小球";
        String actualOdd = (total & 1) == 1 ? "单数" : "双数";
        StringBuilder sb = new StringBuilder();
        sb.append("实际：").append(home).append(':').append(away).append("｜")
                .append(actualResult).append("｜").append(actualSize).append("｜").append(actualOdd).append('\n');

        boolean directionComparable = p.finalDirection.startsWith("主胜") || p.finalDirection.startsWith("客胜") || p.finalDirection.startsWith("平");
        if (directionComparable) {
            String predictedResult = p.finalDirection.startsWith("主胜") ? "主胜" : (p.finalDirection.startsWith("客胜") ? "客胜" : "平");
            sb.append("胜平负：").append(predictedResult.equals(actualResult) ? "✅" : "❌").append(' ')
                    .append(predictedResult).append(" → ").append(actualResult).append('\n');
        } else {
            sb.append("胜平负：未输出明确方向（不计）\n");
        }

        if (!p.sizeSignal.startsWith("大小中性")) {
            String predictedSize = p.sizeSignal.startsWith("大") ? "大球" : "小球";
            sb.append("大小：").append(predictedSize.equals(actualSize) ? "✅" : "❌").append(' ')
                    .append(predictedSize).append(" → ").append(actualSize).append('\n');
        } else {
            sb.append("大小：中性（不计）\n");
        }

        String predictedOdd = p.oddEvenSignal.startsWith("单") ? "单数" : "双数";
        sb.append("单双：").append(predictedOdd.equals(actualOdd) ? "✅" : "❌").append(' ')
                .append(predictedOdd).append(" → ").append(actualOdd).append('\n');
        sb.append("总进球：").append(p.totalGoalsSignal == total ? "✅" : "❌")
                .append(' ').append(p.totalGoalsSignal).append(" → ").append(total).append('\n');

        if (p.orientedScore.matches("\\d+:\\d+")) {
            String[] ps = p.orientedScore.split(":");
            int ph = Integer.parseInt(ps[0]);
            int pa = Integer.parseInt(ps[1]);
            boolean exact = ph == home && pa == away;
            boolean mirror = ph == away && pa == home && !exact;
            sb.append("比分：").append(exact ? "✅ 精确命中" : (mirror ? "⚠️ 镜像结构命中" : "❌"))
                    .append(' ').append(p.orientedScore).append(" → ").append(home).append(':').append(away).append('\n');
        } else {
            sb.append("比分：仅记录无方向形态 ").append(p.scoreShape).append('\n');
        }
        return sb.toString();
    }

    private static String scoreShape(boolean draw, boolean btts, int goals, int diff) {
        if (draw) {
            if (goals <= 0) return "0:0型";
            if (goals <= 2) return "1:1型";
            return "2:2型";
        }
        if (!btts) {
            if (goals <= 1) return "1:0型";
            if (goals == 2 || diff >= 2) return "2:0型";
            return "3:0型";
        }
        if (goals <= 2) return "1:1型（但结构倾向分胜负，信号冲突）";
        if (goals == 3) return "2:1型";
        if (goals == 4) return "3:1型";
        return "3:2型";
    }

    private static String orientShape(String shape, String direction) {
        if (shape == null) return "不输出";
        String base = shape.split("型")[0];
        if (!base.matches("\\d+:\\d+")) return "不输出";
        if (direction.startsWith("主胜")) return base;
        if (direction.startsWith("客胜")) {
            String[] p = base.split(":");
            return p[1] + ":" + p[0];
        }
        if (direction.startsWith("平")) {
            String[] p = base.split(":");
            if (p[0].equals(p[1])) return base;
        }
        return "无方向形态 " + base;
    }

    private static String classifyPath(int a, int b, int c) {
        int x = sign(a), y = sign(b), z = sign(c);
        if (x != 0 && x == y && y == z) return "A→A→A 连续同向";
        if (x != 0 && y == 0 && z == x) return "A→0→A 中段停顿";
        if (x != 0 && y != 0 && z == x && x != y) return "A→B→A 来回反转";
        if (x != 0 && y != 0 && y == z && x != y) return "A→B→B 真反转";
        if (x == 0 && y == 0 && z != 0) return "0→0→A 后程破平";
        if (x != 0 && y == x && z == 0) return "A→A→0 末段比和";
        return "混合路径";
    }

    private static String resultFromHomeScore(double homeScore) {
        if (homeScore >= 0.60) return "主胜";
        if (homeScore <= -0.60) return "客胜";
        return "平";
    }

    private static String phaseResult(double homeScore) {
        if (homeScore >= 0.50) return "主";
        if (homeScore <= -0.50) return "客";
        return "平";
    }

    private static Stage relation(String upper, String lower) {
        if (upper.equals(lower)) return new Stage("比和", 0);
        if (controls(upper).equals(lower)) return new Stage("上克下", 2);
        if (generates(lower).equals(upper)) return new Stage("下生上", 2);
        if (controls(lower).equals(upper)) return new Stage("下克上", -2);
        if (generates(upper).equals(lower)) return new Stage("上生下", -2);
        return new Stage("未定义", 0);
    }

    private static String generates(String e) {
        switch (e) {
            case "木": return "火";
            case "火": return "土";
            case "土": return "金";
            case "金": return "水";
            case "水": return "木";
            default: return "";
        }
    }

    private static String controls(String e) {
        switch (e) {
            case "木": return "土";
            case "土": return "水";
            case "水": return "火";
            case "火": return "金";
            case "金": return "木";
            default: return "";
        }
    }

    private static Trigram fromDigit(int digit) {
        int i = digit % 8;
        if (i == 0) i = 8;
        return TRIGRAMS[i];
    }

    private static Trigram fromLines(int a, int b, int c) {
        for (int i = 1; i <= 8; i++) {
            int[] l = TRIGRAMS[i].lines;
            if (l[0] == a && l[1] == b && l[2] == c) return TRIGRAMS[i];
        }
        throw new IllegalStateException("未知卦线");
    }

    private static String hexName(Trigram upper, Trigram lower) {
        String name = HEX.get(upper.name + "-" + lower.name);
        return name == null ? upper.name + lower.name : name;
    }

    private static int sign(int x) {
        return Integer.compare(x, 0);
    }

    private static String join(List<String> items, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(sep);
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
