package com.shuiwushi.cezi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.SplittableRandom;

public final class CeziEngine {
    private CeziEngine() {}

    public enum Element {
        WOOD("木"), FIRE("火"), EARTH("土"), METAL("金"), WATER("水");
        public final String zh;
        Element(String zh) { this.zh = zh; }
    }

    private static final String[] OMENS = {"动", "静", "明", "暗", "开", "合", "升", "降"};
    private static final String[] TRIGRAMS = {"乾", "兑", "离", "震", "巽", "坎", "艮", "坤"};
    private static final Element[] TRIGRAM_ELEMENTS = {
            Element.METAL, Element.METAL, Element.FIRE, Element.WOOD,
            Element.WOOD, Element.WATER, Element.EARTH, Element.EARTH
    };

    private static final String[] MANSIONS = {
            "角木蛟","亢金龙","氐土貉","房日兔","心月狐","尾火虎","箕水豹",
            "斗木獬","牛金牛","女土蝠","虚日鼠","危月燕","室火猪","壁水貐",
            "奎木狼","娄金狗","胃土雉","昴日鸡","毕月乌","觜火猴","参水猿",
            "井木犴","鬼金羊","柳土獐","星日马","张月鹿","翼火蛇","轸水蚓"
    };
    private static final Element[] MANSION_ELEMENTS = {
            Element.WOOD,Element.METAL,Element.EARTH,Element.FIRE,Element.WATER,Element.FIRE,Element.WATER,
            Element.WOOD,Element.METAL,Element.EARTH,Element.FIRE,Element.WATER,Element.FIRE,Element.WATER,
            Element.WOOD,Element.METAL,Element.EARTH,Element.FIRE,Element.WATER,Element.FIRE,Element.WATER,
            Element.WOOD,Element.METAL,Element.EARTH,Element.FIRE,Element.WATER,Element.FIRE,Element.WATER
    };

    public static final class Result {
        public long seed;
        public CharRepository.Item item;
        public String omen;
        public GlyphAnalyzer.Metrics glyph;

        public int leftNumber, rightNumber;
        public Element leftElement, rightElement;
        public int hostMansion, guestMansion;
        public int upperTrigram, lowerTrigram, movingLine;
        public int shapeVote, fiveVote, yanqinVote, yiVote;
        public int totalScore, halfScore;

        public String winDrawLoss;
        public String strength;
        public String halfFull;
        public int[] goalVotes;
        public int goalBase, goalTotal;
        public String overUnder, oddEven, fourWay;
        public int homeGoals, awayGoals;
        public String score;
        public String voteSpread;
        public String seal;

        public String compactText() {
            return String.format(Locale.CHINA,
                    "测字演禽足球 V1\n种子：%016X\n测字：%s（第%d字，%d画，部首%s，%s，%s）\n" +
                    "外应：%s\n胜平负：%s · %s\n半全场：%s\n进球数：%d（基础%d）\n比分：%s\n大小：%s｜单双：%s｜组合：%s\n" +
                    "五数：%s｜离散：%s\n字形票：%+d｜五行票：%+d｜演禽票：%+d｜周易票：%+d｜总势：%+d\n" +
                    "演禽：主%s / 客%s\n周易：上%s%s / 下%s%s，动%d爻\n密封：%s",
                    seed, item.ch, item.index, item.strokes, item.radical,
                    CharRepository.structureName(item.structure), CharRepository.frequencyName(item.frequency),
                    omen, winDrawLoss, strength, halfFull, goalTotal, goalBase, score,
                    overUnder, oddEven, fourWay, Arrays.toString(goalVotes), voteSpread,
                    shapeVote, fiveVote, yanqinVote, yiVote, totalScore,
                    MANSIONS[hostMansion], MANSIONS[guestMansion],
                    TRIGRAMS[upperTrigram], TRIGRAM_ELEMENTS[upperTrigram].zh,
                    TRIGRAMS[lowerTrigram], TRIGRAM_ELEMENTS[lowerTrigram].zh,
                    movingLine, seal);
        }
    }

    public static Result calculate(long seed, CharRepository.Item item,
                                   GlyphAnalyzer.Metrics glyph, String omenChoice) {
        Result r = new Result();
        r.seed = seed;
        r.item = item;
        r.glyph = glyph;
        r.omen = resolveOmen(seed, omenChoice);

        r.leftNumber = 1 + Math.floorMod((int)((glyph.left / 255L) + item.strokes + item.index), 10);
        r.rightNumber = 1 + Math.floorMod((int)((glyph.right / 255L) + item.strokes * 2L + item.index), 10);
        r.leftElement = elementFromHetu(r.leftNumber);
        r.rightElement = elementFromHetu(r.rightNumber);

        r.shapeVote = shapeVote(glyph);
        r.fiveVote = relationVote(r.leftElement, r.rightElement);

        r.hostMansion = Math.floorMod((int)((glyph.left / 255L) + item.strokes * 17L + item.index), 28);
        r.guestMansion = Math.floorMod((int)((glyph.right / 255L) + item.strokes * 29L + item.index * 3L), 28);
        r.yanqinVote = relationVote(MANSION_ELEMENTS[r.hostMansion], MANSION_ELEMENTS[r.guestMansion]);

        r.upperTrigram = Math.floorMod((int)((glyph.top / 255L) + item.strokes + item.index), 8);
        r.lowerTrigram = Math.floorMod((int)((glyph.bottom / 255L) + item.strokes * 2L + item.index), 8);
        r.movingLine = 1 + Math.floorMod(item.index + item.strokes + (int)(glyph.total % 6), 6);
        r.yiVote = relationVote(TRIGRAM_ELEMENTS[r.upperTrigram], TRIGRAM_ELEMENTS[r.lowerTrigram]);

        r.totalScore = r.shapeVote + r.fiveVote + r.yanqinVote + r.yiVote;
        judgeFull(r);
        judgeHalf(r);
        judgeGoals(r);
        allocateScore(r);
        r.seal = seal(r);
        return r;
    }

    private static void judgeFull(Result r) {
        if (r.totalScore >= 2) r.winDrawLoss = "主胜";
        else if (r.totalScore <= -2) r.winDrawLoss = "客胜";
        else r.winDrawLoss = "平/胶着";

        int a = Math.abs(r.totalScore);
        if (a <= 1) r.strength = "高胶着";
        else if (a <= 3) r.strength = "偏势";
        else if (a <= 5) r.strength = "较强";
        else r.strength = "强势";
    }

    private static void judgeHalf(Result r) {
        int topShape = balanceVote(r.glyph.tl, r.glyph.tr, r.glyph.tl + r.glyph.tr);
        int topLeftNum = 1 + Math.floorMod((int)((r.glyph.tl / 255L) + r.item.strokes), 10);
        int topRightNum = 1 + Math.floorMod((int)((r.glyph.tr / 255L) + r.item.index), 10);
        int topElement = relationVote(elementFromHetu(topLeftNum), elementFromHetu(topRightNum));
        r.halfScore = topShape + topElement;
        String half = r.halfScore >= 2 ? "胜" : r.halfScore <= -2 ? "负" : "平";
        String full = "主胜".equals(r.winDrawLoss) ? "胜" : "客胜".equals(r.winDrawLoss) ? "负" : "平";
        r.halfFull = half + "/" + full;
    }

    private static void judgeGoals(Result r) {
        int strokeGoal = Math.floorMod(r.item.strokes, 7);
        int shapeGoal = Math.floorMod((int)Math.round(r.glyph.density * 1000.0) + r.item.structure.hashCode(), 7);
        int fiveGoal = Math.floorMod(r.leftNumber + r.rightNumber + r.item.strokes, 7);
        int yanqinGoal = Math.floorMod(r.hostMansion + r.guestMansion + 2, 7);
        int yiGoal = Math.floorMod((r.upperTrigram + 1) + (r.lowerTrigram + 1) + r.movingLine, 7);
        r.goalVotes = new int[]{strokeGoal, shapeGoal, fiveGoal, yanqinGoal, yiGoal};
        int[] sorted = r.goalVotes.clone();
        Arrays.sort(sorted);
        r.goalBase = sorted[2];
        r.goalTotal = clamp(r.goalBase + omenDelta(r.omen), 0, 6);
        int spread = sorted[4] - sorted[0];
        r.voteSpread = spread <= 2 ? "集中" : spread <= 4 ? "中等分歧" : "分歧较大";
        r.overUnder = r.goalTotal >= 3 ? "大2.5" : "小2.5";
        r.oddEven = (r.goalTotal & 1) == 1 ? "单" : "双";
        r.fourWay = (r.goalTotal >= 3 ? "大" : "小") + r.oddEven;
    }

    private static void allocateScore(Result r) {
        int total = r.goalTotal;
        if ("平/胶着".equals(r.winDrawLoss)) {
            if ((total & 1) == 1) {
                if (r.movingLine >= 4 && total < 6) total++;
                else total--;
            }
            total = clamp(total, 0, 6);
            r.homeGoals = r.awayGoals = total / 2;
        } else {
            if (total == 0) total = 1;
            int strength = Math.abs(r.totalScore);
            int diff;
            if ((total & 1) == 0) {
                if (total < 2) total = 2;
                diff = 2;
                if (strength >= 6 && total >= 4) diff = 4;
            } else {
                diff = (strength >= 5 && total >= 3) ? 3 : 1;
            }
            if (diff > total) diff = total;
            int winner = (total + diff) / 2;
            int loser = (total - diff) / 2;
            if ("主胜".equals(r.winDrawLoss)) {
                r.homeGoals = winner; r.awayGoals = loser;
            } else {
                r.homeGoals = loser; r.awayGoals = winner;
            }
        }
        r.goalTotal = r.homeGoals + r.awayGoals;
        r.score = r.homeGoals + ":" + r.awayGoals;
        r.overUnder = r.goalTotal >= 3 ? "大2.5" : "小2.5";
        r.oddEven = (r.goalTotal & 1) == 1 ? "单" : "双";
        r.fourWay = (r.goalTotal >= 3 ? "大" : "小") + r.oddEven;
    }

    private static int shapeVote(GlyphAnalyzer.Metrics m) {
        return balanceVote(m.left, m.right, m.total);
    }

    private static int balanceVote(long a, long b, long total) {
        if (total <= 0) return 0;
        double d = (a - b) / (double) total;
        if (d > 0.035) return 1;
        if (d < -0.035) return -1;
        return 0;
    }

    private static Element elementFromHetu(int n) {
        int d = Math.floorMod(n, 10);
        if (d == 1 || d == 6) return Element.WATER;
        if (d == 2 || d == 7) return Element.FIRE;
        if (d == 3 || d == 8) return Element.WOOD;
        if (d == 4 || d == 9) return Element.METAL;
        return Element.EARTH;
    }

    private static int relationVote(Element host, Element guest) {
        if (host == guest) return 0;
        if (overcomes(host, guest)) return 2;
        if (overcomes(guest, host)) return -2;
        if (generates(guest, host)) return 1;
        if (generates(host, guest)) return -1;
        return 0;
    }

    private static boolean generates(Element a, Element b) {
        return (a == Element.WOOD && b == Element.FIRE) ||
                (a == Element.FIRE && b == Element.EARTH) ||
                (a == Element.EARTH && b == Element.METAL) ||
                (a == Element.METAL && b == Element.WATER) ||
                (a == Element.WATER && b == Element.WOOD);
    }

    private static boolean overcomes(Element a, Element b) {
        return (a == Element.WOOD && b == Element.EARTH) ||
                (a == Element.EARTH && b == Element.WATER) ||
                (a == Element.WATER && b == Element.FIRE) ||
                (a == Element.FIRE && b == Element.METAL) ||
                (a == Element.METAL && b == Element.WOOD);
    }

    private static String resolveOmen(long seed, String choice) {
        if (choice != null && !choice.startsWith("随机")) return choice;
        SplittableRandom random = new SplittableRandom(seed ^ 0x6E624EB7A51D39C3L);
        return OMENS[random.nextInt(OMENS.length)];
    }

    private static int omenDelta(String omen) {
        if ("动".equals(omen) || "明".equals(omen) || "开".equals(omen) || "升".equals(omen)) return 1;
        if ("静".equals(omen) || "暗".equals(omen) || "合".equals(omen) || "降".equals(omen)) return -1;
        return 0;
    }

    private static int clamp(int x, int min, int max) {
        return Math.max(min, Math.min(max, x));
    }

    private static String seal(Result r) {
        String raw = String.format(Locale.ROOT,
                "%016X|%s|%d|%d|%s|%d|%d|%d|%d|%d|%s|%s|%s",
                r.seed, r.item.ch, r.item.index, r.item.strokes, r.omen,
                r.shapeVote, r.fiveVote, r.yanqinVote, r.yiVote,
                r.goalTotal, r.winDrawLoss, r.halfFull, r.score);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 8; i++) b.append(String.format(Locale.ROOT, "%02x", bytes[i]));
            return b.toString();
        } catch (Exception e) {
            return "hash-error";
        }
    }

    public static String mansionName(int i) { return MANSIONS[Math.floorMod(i, 28)]; }
    public static String trigramName(int i) { return TRIGRAMS[Math.floorMod(i, 8)]; }
    public static Element mansionElement(int i) { return MANSION_ELEMENTS[Math.floorMod(i, 28)]; }
    public static Element trigramElement(int i) { return TRIGRAM_ELEMENTS[Math.floorMod(i, 8)]; }
}
