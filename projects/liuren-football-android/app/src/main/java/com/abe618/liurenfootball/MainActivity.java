package com.abe618.liurenfootball;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private LinearLayout root;
    private final int pad = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void base(String title, String subtitle) {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.rgb(247, 245, 239));
        scroll.addView(root);
        setContentView(scroll);

        TextView h = text(title, 25, true);
        h.setTextColor(Color.rgb(49, 40, 28));
        root.addView(h);
        TextView sub = text(subtitle, 13, false);
        sub.setTextColor(Color.DKGRAY);
        root.addView(sub);
        rowNav();
    }

    private void rowNav() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 14, 0, 14);
        Button a = smallButton("联网比赛");
        Button b = smallButton("手工双盘");
        Button c = smallButton("年命独立");
        a.setOnClickListener(v -> showMatches(LocalDate.now()));
        b.setOnClickListener(v -> showPredict(new Models.MatchItem()));
        c.setOnClickListener(v -> showYearOnly());
        row.addView(a, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(c, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(row);
    }

    private void showHome() {
        base("六壬足球双盘", "740局随机日期盘 × 今日活时盘｜主客干支定位｜球队/教练生肖投票｜联网赛程");
        cardTitle("两套盘并行");
        root.addView(text("A. 740局随机日期盘：固定种子生成740个可复盘日期/时辰样本，每次预测随机抽一局并冻结编号。\n\nB. 今日活时盘：固定当天日干支，以手机当前时辰起课；首次计算后结果固定在本次页面。\n\n两盘都使用同一套四课、九宗门、十二天将、主客定位和三池评分，便于观察同向/分歧。", 16, false));
        cardTitle("主客定位");
        root.addView(text("D0：干=客、支=主。可叠加主队成立年支、客队成立年支、主教练生年支、客教练生年支。年支不上四课或同时落干支两侧时自动弃权，不强行补票。", 15, false));
        cardTitle("使用方式");
        root.addView(text("优先从“联网比赛”选择场次；球队成立年可尝试联网补全，教练出生年手工补录。也可完全手工输入。独立年命模块不需要球队名称，只按“前主后客”输入两方年命。", 15, false));
        TextView note = text("说明：这是规则复盘/研究工具，不承诺真实比赛预测能力，不构成投注建议。", 13, false);
        note.setTextColor(Color.rgb(120, 65, 35));
        root.addView(note);
    }

    private void showMatches(LocalDate date) {
        base("联网比赛", "日期赛程来自公开网络数据源；网络源缺失时仍可切到手工双盘。");
        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        Button prev = smallButton("前一天");
        Button today = smallButton("今天");
        Button next = smallButton("后一天");
        dateRow.addView(prev, new LinearLayout.LayoutParams(0, -2, 1));
        dateRow.addView(today, new LinearLayout.LayoutParams(0, -2, 1));
        dateRow.addView(next, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(dateRow);
        TextView day = text("日期：" + date, 18, true);
        root.addView(day);
        ProgressBar pb = new ProgressBar(this);
        root.addView(pb);
        TextView status = text("正在联网读取足球赛程……", 14, false);
        root.addView(status);

        prev.setOnClickListener(v -> showMatches(date.minusDays(1)));
        today.setOnClickListener(v -> showMatches(LocalDate.now()));
        next.setOnClickListener(v -> showMatches(date.plusDays(1)));

        io.submit(() -> {
            try {
                List<Models.MatchItem> matches = MatchService.fetchMatches(date);
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    status.setText(matches.isEmpty() ? "当天网络源没有返回比赛。可切换日期或手工录入。" : "共读取 " + matches.size() + " 场；点击任一场进入双盘。\n时间字段可能为数据源UTC，排盘不采用开球时间，因此不影响本软件两种起课法。");
                    for (Models.MatchItem m : matches) addMatchCard(m);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pb.setVisibility(View.GONE);
                    status.setText("联网失败：" + e.getMessage() + "\n可直接使用“手工双盘”。");
                });
            }
        });
    }

    private void addMatchCard(Models.MatchItem m) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setText((m.league == null ? "" : m.league) + "\n" + m.home + "  vs  " + m.away + (m.time == null || m.time.isEmpty() ? "" : "\n" + m.time));
        b.setPadding(18, 12, 18, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 6, 0, 6);
        root.addView(b, lp);
        b.setOnClickListener(v -> showPredict(m));
    }

    private void showPredict(Models.MatchItem match) {
        base("双盘预测", "同一主客资料同时进入两套排盘；随机盘与活时盘各自独立计算，不互相改写。默认左/前为主队，右/后为客队。");

        EditText homeName = edit("主队名称", match.home == null ? "主队" : match.home, false);
        EditText awayName = edit("客队名称", match.away == null ? "客队" : match.away, false);
        root.addView(homeName); root.addView(awayName);

        EditText homeYear = edit("主队成立年（可空，如 1902）", match.homeFoundedYear == null ? "" : String.valueOf(match.homeFoundedYear), true);
        EditText awayYear = edit("客队成立年（可空）", match.awayFoundedYear == null ? "" : String.valueOf(match.awayFoundedYear), true);
        EditText homeCoach = edit("主教练出生年（可空）", "", true);
        EditText awayCoach = edit("客教练出生年（可空）", "", true);
        root.addView(homeYear); root.addView(awayYear); root.addView(homeCoach); root.addView(awayCoach);

        EditText poolIndex = edit("740局编号（0-739；空=每次随机抽取）", "", true);
        root.addView(poolIndex);
        EditText monthOverride = edit("今日盘月将覆写（可空；可填 子/丑/.../亥，临界节气时使用）", "", false);
        root.addView(monthOverride);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button autoYears = smallButton("联网补球队年份");
        Button run = smallButton("双盘预测");
        actions.addView(autoYears, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(run, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(actions);

        TextView result = text("", 14, false);
        result.setTextIsSelectable(true);
        result.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.setMargins(0, 14, 0, 20);
        root.addView(result, rlp);

        autoYears.setOnClickListener(v -> {
            autoYears.setEnabled(false);
            Toast.makeText(this, "正在查询球队成立年…", Toast.LENGTH_SHORT).show();
            String hn = homeName.getText().toString().trim();
            String an = awayName.getText().toString().trim();
            io.submit(() -> {
                Models.TeamMeta hm = new Models.TeamMeta();
                Models.TeamMeta am = new Models.TeamMeta();
                try { hm = MatchService.searchTeamMeta(hn); } catch (Exception ignored) {}
                try { am = MatchService.searchTeamMeta(an); } catch (Exception ignored) {}
                Models.TeamMeta fHm = hm, fAm = am;
                runOnUiThread(() -> {
                    if (fHm.foundedYear != null) homeYear.setText(String.valueOf(fHm.foundedYear));
                    if (fAm.foundedYear != null) awayYear.setText(String.valueOf(fAm.foundedYear));
                    autoYears.setEnabled(true);
                    Toast.makeText(this, "补全完成；未查到的年份保持为空。", Toast.LENGTH_SHORT).show();
                });
            });
        });

        run.setOnClickListener(v -> {
            Models.InputMeta meta = new Models.InputMeta();
            meta.homeName = nonEmpty(homeName.getText().toString(), "主队");
            meta.awayName = nonEmpty(awayName.getText().toString(), "客队");
            meta.homeFoundedYear = intOrNull(homeYear.getText().toString());
            meta.awayFoundedYear = intOrNull(awayYear.getText().toString());
            meta.homeCoachBirthYear = intOrNull(homeCoach.getText().toString());
            meta.awayCoachBirthYear = intOrNull(awayCoach.getText().toString());
            Integer idx = intOrNull(poolIndex.getText().toString());
            if (idx != null && (idx < 0 || idx >= 740)) {
                Toast.makeText(this, "740局编号应为 0-739；已按随机处理。", Toast.LENGTH_SHORT).show();
                idx = null;
            }
            Integer mg = parseBranch(monthOverride.getText().toString());

            Models.Chart randomChart = LiurenEngine.build(RandomCharts.randomPoolSeed(idx));
            Models.Chart liveChart = LiurenEngine.build(RandomCharts.liveSeedWithManualMonthGeneral(mg));
            Models.PredictionResult a = PredictionModel.predict(randomChart, meta, "A｜740局随机日期盘");
            Models.PredictionResult b = PredictionModel.predict(liveChart, meta, "B｜今日活时盘");
            result.setText(compareHeader(a, b) + "\n\n" + render(a, meta) + "\n\n" + repeat('═', 44) + "\n\n" + render(b, meta));
        });
    }

    private void showYearOnly() {
        base("年命独立占测", "不指定比赛名称。一个输入框内严格按“前面主方、后面客方”解析；支持公历年份或生肖/地支。");
        TextView order = text("输入顺序：主方在前，客方在后。例：1984 1990；或 鼠 马；或 子 午。", 15, true);
        root.addView(order);
        EditText pair = edit("主方年命  客方年命", "", false);
        root.addView(pair);
        EditText fixedIndex = edit("740局编号（可空，0-739）", "", true);
        root.addView(fixedIndex);
        Button run = new Button(this);
        run.setText("两盘并行判断");
        run.setAllCaps(false);
        root.addView(run);
        TextView result = text("", 14, false);
        result.setTypeface(Typeface.MONOSPACE);
        result.setTextIsSelectable(true);
        root.addView(result);

        run.setOnClickListener(v -> {
            String raw = pair.getText().toString().trim();
            String[] parts = raw.split("[\\s,，;；/\\|]+", 3);
            if (parts.length < 2) {
                Toast.makeText(this, "请输入两方年命：前主后客。", Toast.LENGTH_SHORT).show();
                return;
            }
            Integer hb = LiurenEngine.branchFromYearOrZodiacToken(parts[0]);
            Integer ab = LiurenEngine.branchFromYearOrZodiacToken(parts[1]);
            if (hb == null || ab == null) {
                Toast.makeText(this, "年命无法识别，可输入年份或生肖/地支。", Toast.LENGTH_SHORT).show();
                return;
            }
            Models.InputMeta meta = new Models.InputMeta();
            meta.homeName = "主方";
            meta.awayName = "客方";
            meta.homeFoundedYear = 1984 + hb;
            meta.awayFoundedYear = 1984 + ab;
            Integer idx = intOrNull(fixedIndex.getText().toString());
            if (idx != null && (idx < 0 || idx >= 740)) idx = null;

            Models.PredictionResult a = PredictionModel.predict(LiurenEngine.build(RandomCharts.randomPoolSeed(idx)), meta, "A｜740局随机日期盘");
            Models.PredictionResult b = PredictionModel.predict(LiurenEngine.build(RandomCharts.liveSeed()), meta, "B｜今日活时盘");
            String normalized = "主方年命：" + LiurenEngine.BRANCHES[hb] + "(" + LiurenEngine.ZODIAC[hb] + ")；客方年命：" + LiurenEngine.BRANCHES[ab] + "(" + LiurenEngine.ZODIAC[ab] + ")\n\n";
            result.setText(normalized + compareHeader(a, b) + "\n\n" + render(a, meta) + "\n\n" + repeat('═', 44) + "\n\n" + render(b, meta));
        });
    }

    private String compareHeader(Models.PredictionResult a, Models.PredictionResult b) {
        String relation = a.verdict.equals(b.verdict) ? "【两盘同向】" : "【两盘分歧】";
        return relation + "  随机盘=" + a.verdict + "  /  活时盘=" + b.verdict
                + "\n随机盘概率 主/平/客=" + pct(a.pHome) + "/" + pct(a.pDraw) + "/" + pct(a.pAway)
                + "\n活时盘概率 主/平/客=" + pct(b.pHome) + "/" + pct(b.pDraw) + "/" + pct(b.pAway);
    }

    private String render(Models.PredictionResult r, Models.InputMeta meta) {
        Models.Chart c = r.chart;
        StringBuilder sb = new StringBuilder();
        sb.append(r.mode).append('\n');
        sb.append(repeat('─', 36)).append('\n');
        sb.append("冻结日期/时辰：").append(c.seed.date).append(' ')
                .append(LiurenEngine.BRANCHES[c.seed.hourBranch]).append("时");
        if (c.seed.poolIndex >= 0) sb.append("  局号#").append(c.seed.poolIndex);
        sb.append('\n');
        sb.append("日柱：").append(LiurenEngine.dayName(c))
                .append("  月将：").append(LiurenEngine.BRANCHES[c.seed.monthGeneral])
                .append("  月建：").append(LiurenEngine.BRANCHES[c.monthBuild]).append('\n');
        sb.append("四课：").append(LiurenEngine.fourCoursesText(c)).append('\n');
        sb.append("课体：").append(c.pattern).append("  三传：").append(LiurenEngine.transmissionName(c)).append('\n');
        sb.append("贵人：").append(LiurenEngine.BRANCHES[c.nobleBranch])
                .append(c.nobleForward ? " 顺布" : " 逆布").append('\n');
        sb.append("定位：主=").append(r.location.homeIsBranch ? "支" : "干")
                .append("，客=").append(r.location.homeIsBranch ? "干" : "支")
                .append("；一致性 ").append(pct(r.location.consistency)).append('\n');
        for (Models.LocationVote v : r.location.votes) {
            sb.append("  · ").append(v.source).append("[").append(v.weight).append("] ")
                    .append(v.vote).append("：").append(v.detail).append('\n');
        }
        sb.append("评分 主/平/客：")
                .append(fmt(r.homeScore)).append(" / ").append(fmt(r.drawScore)).append(" / ").append(fmt(r.awayScore)).append('\n');
        sb.append("概率 主/平/客：")
                .append(pct(r.pHome)).append(" / ").append(pct(r.pDraw)).append(" / ").append(pct(r.pAway)).append('\n');
        sb.append("主判：").append(r.verdict).append("  ｜ ").append(r.extendedSummary).append('\n');
        sb.append("评分明细：\n");
        for (Models.ScoreLine line : r.scoreLines) {
            sb.append("  ").append(line.item).append("：").append(line.detail)
                    .append(" [主").append(signed(line.homeDelta))
                    .append(" 平").append(signed(line.drawDelta))
                    .append(" 客").append(signed(line.awayDelta)).append("]\n");
        }
        sb.append("\n注：初传=客、末传=主以及勾陈/玄武是独立阅读维度；定位投票倒置时不会同步倒置这两个维度。");
        return sb.toString();
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.rgb(42, 42, 42));
        t.setPadding(4, 6, 4, 6);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void cardTitle(String s) {
        TextView t = text(s, 18, true);
        t.setPadding(0, 18, 0, 4);
        root.addView(t);
    }

    private Button smallButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(13);
        return b;
    }

    private EditText edit(String hint, String value, boolean numeric) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value == null ? "" : value);
        e.setTextSize(15);
        e.setSingleLine(true);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 4, 0, 4);
        e.setLayoutParams(lp);
        return e;
    }

    private Integer intOrNull(String s) {
        try {
            String x = s == null ? "" : s.trim();
            if (x.isEmpty()) return null;
            return Integer.parseInt(x);
        } catch (Exception e) { return null; }
    }

    private Integer parseBranch(String s) {
        if (s == null) return null;
        String x = s.trim();
        if (x.isEmpty()) return null;
        for (int i = 0; i < LiurenEngine.BRANCHES.length; i++) if (x.contains(LiurenEngine.BRANCHES[i])) return i;
        Integer n = intOrNull(x);
        return n != null && n >= 0 && n < 12 ? n : null;
    }

    private static String nonEmpty(String x, String fallback) {
        return x == null || x.trim().isEmpty() ? fallback : x.trim();
    }

    private static String pct(double x) { return String.format(Locale.US, "%.1f%%", x * 100); }
    private static String fmt(double x) { return String.format(Locale.US, "%.1f", x); }
    private static String signed(double x) { return String.format(Locale.US, "%+.1f", x); }
    private static String repeat(char c, int n) { StringBuilder s = new StringBuilder(); for (int i=0;i<n;i++) s.append(c); return s.toString(); }
}
