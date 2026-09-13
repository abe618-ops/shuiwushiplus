package com.abe618.meihuafootball;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "meihua_football_blindtest";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_FROZEN_DIGITS = "frozen_digits";
    private static final String KEY_FROZEN_ID = "frozen_id";

    private final int ink = Color.rgb(31, 41, 55);
    private final int muted = Color.rgb(107, 114, 128);
    private final int accent = Color.rgb(15, 118, 110);
    private final int accent2 = Color.rgb(124, 58, 237);
    private final int card = Color.rgb(248, 250, 252);
    private final int border = Color.rgb(226, 232, 240);

    private SharedPreferences prefs;
    private EditText homeTeam;
    private EditText awayTeam;
    private EditText digitsInput;
    private EditText homeScore;
    private EditText awayScore;
    private TextView predictionView;
    private TextView reviewView;
    private TextView historyView;
    private TextView freezeStatus;
    private MeihuaEngine.Prediction latest;
    private MeihuaEngine.Prediction frozen;
    private String frozenId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(28));
        root.setBackgroundColor(Color.rgb(241, 245, 249));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("梅花足球盲测", 28, Typeface.BOLD, ink);
        root.addView(title);
        TextView sub = text("随机数起卦 · 本卦/互卦/变卦 · A轨结构 + B轨主客极性 · 事前冻结/赛后复盘", 14, Typeface.NORMAL, muted);
        sub.setPadding(0, dp(4), 0, dp(12));
        root.addView(sub);

        TextView warning = text("实验说明：本工具用于可复算的随机盲测研究，不代表科学预测能力，也不提供投注保证。规则升级只从下一场生效，历史结果不回改。", 13, Typeface.NORMAL, Color.rgb(120, 53, 15));
        warning.setPadding(dp(12), dp(10), dp(12), dp(10));
        warning.setBackground(roundRect(Color.rgb(255, 247, 237), Color.rgb(253, 186, 116), 12));
        root.addView(warning, marginBottom(14));

        LinearLayout teamsCard = card();
        teamsCard.addView(sectionTitle("1. 比赛标记（不参与随机计算）"));
        homeTeam = edit("主队", InputType.TYPE_CLASS_TEXT);
        awayTeam = edit("客队", InputType.TYPE_CLASS_TEXT);
        homeTeam.setText("主队");
        awayTeam.setText("客队");
        teamsCard.addView(homeTeam, fieldLp());
        teamsCard.addView(awayTeam, fieldLp());
        root.addView(teamsCard, marginBottom(12));

        LinearLayout randomCard = card();
        randomCard.addView(sectionTitle("2. 三位随机数起盘"));
        digitsInput = edit("000-999", InputType.TYPE_CLASS_NUMBER);
        digitsInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
        randomCard.addView(digitsInput, fieldLp());

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button randomBtn = button("完全随机", accent);
        Button calcBtn = button("按此数起盘", accent2);
        actions.addView(randomBtn, weightButtonLp());
        actions.addView(calcBtn, weightButtonLp());
        randomCard.addView(actions);

        freezeStatus = text("尚未冻结", 13, Typeface.BOLD, muted);
        freezeStatus.setPadding(0, dp(10), 0, 0);
        randomCard.addView(freezeStatus);
        root.addView(randomCard, marginBottom(12));

        LinearLayout predCard = card();
        predCard.addView(sectionTitle("3. v1.3 双轨预测"));
        predictionView = text("点击“完全随机”开始第一场。", 15, Typeface.NORMAL, ink);
        predictionView.setLineSpacing(dp(3), 1f);
        predCard.addView(predictionView);

        LinearLayout predActions = new LinearLayout(this);
        predActions.setOrientation(LinearLayout.HORIZONTAL);
        Button freezeBtn = button("冻结本场", accent);
        Button copyBtn = button("复制预测", Color.rgb(71, 85, 105));
        predActions.addView(freezeBtn, weightButtonLp());
        predActions.addView(copyBtn, weightButtonLp());
        predCard.addView(predActions, marginTop(12));
        root.addView(predCard, marginBottom(12));

        LinearLayout reviewCard = card();
        reviewCard.addView(sectionTitle("4. 录入赛果并复盘"));
        LinearLayout scores = new LinearLayout(this);
        scores.setOrientation(LinearLayout.HORIZONTAL);
        scores.setGravity(Gravity.CENTER_VERTICAL);
        homeScore = edit("主队进球", InputType.TYPE_CLASS_NUMBER);
        awayScore = edit("客队进球", InputType.TYPE_CLASS_NUMBER);
        scores.addView(homeScore, weightFieldLp());
        TextView colon = text("：", 24, Typeface.BOLD, ink);
        colon.setGravity(Gravity.CENTER);
        scores.addView(colon, new LinearLayout.LayoutParams(dp(36), dp(52)));
        scores.addView(awayScore, weightFieldLp());
        reviewCard.addView(scores);
        Button reviewBtn = button("按冻结结果复盘", accent2);
        reviewCard.addView(reviewBtn, fullButtonLp());
        reviewView = text("复盘时严格按事前冻结结果记账；镜像比分只算“结构命中”，不算精确比分。", 14, Typeface.NORMAL, muted);
        reviewView.setPadding(0, dp(10), 0, 0);
        reviewCard.addView(reviewView);
        root.addView(reviewCard, marginBottom(12));

        LinearLayout historyCard = card();
        historyCard.addView(sectionTitle("5. 本机盲测记录"));
        historyView = text("暂无记录", 13, Typeface.NORMAL, ink);
        historyView.setLineSpacing(dp(2), 1f);
        historyCard.addView(historyView);
        LinearLayout historyActions = new LinearLayout(this);
        historyActions.setOrientation(LinearLayout.HORIZONTAL);
        Button refreshBtn = button("刷新历史", Color.rgb(71, 85, 105));
        Button clearBtn = button("清空历史", Color.rgb(185, 28, 28));
        historyActions.addView(refreshBtn, weightButtonLp());
        historyActions.addView(clearBtn, weightButtonLp());
        historyCard.addView(historyActions, marginTop(10));
        root.addView(historyCard);

        randomBtn.setOnClickListener(v -> {
            String digits = MeihuaEngine.randomDigits();
            digitsInput.setText(digits);
            runPrediction(digits);
        });
        calcBtn.setOnClickListener(v -> runPrediction(digitsInput.getText().toString().trim()));
        freezeBtn.setOnClickListener(v -> freezeCurrent());
        copyBtn.setOnClickListener(v -> copyPrediction());
        reviewBtn.setOnClickListener(v -> reviewFrozen());
        refreshBtn.setOnClickListener(v -> loadHistory());
        clearBtn.setOnClickListener(v -> {
            prefs.edit().remove(KEY_HISTORY).apply();
            loadHistory();
            Toast.makeText(this, "历史已清空", Toast.LENGTH_SHORT).show();
        });

        restoreFrozen();
        loadHistory();
        setContentView(scroll);
    }

    private void runPrediction(String digits) {
        try {
            latest = MeihuaEngine.analyze(digits);
            predictionView.setText(renderPrediction(latest));
            freezeStatus.setText("当前盘未冻结：" + digits);
            freezeStatus.setTextColor(Color.rgb(180, 83, 9));
            reviewView.setText("请先冻结本场，再录入实际比分。");
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String renderPrediction(MeihuaEngine.Prediction p) {
        StringBuilder sb = new StringBuilder();
        sb.append("随机数：").append(p.digits).append('\n');
        sb.append("上卦：").append(p.upper.name).append(p.upper.element)
                .append("　下卦：").append(p.lower.name).append(p.lower.element).append('\n');
        sb.append("本卦：").append(p.originalHex).append("　")
                .append("互卦：").append(p.mutualHex).append("　")
                .append("变卦：").append(p.changedHex).append('\n');
        sb.append("动爻：第").append(p.movingLine).append("爻\n\n");

        sb.append("【三阶段路径】\n");
        sb.append("本：").append(p.originalStage.relation).append("（").append(p.originalStage.label()).append("）\n");
        sb.append("互：").append(p.mutualStage.relation).append("（").append(p.mutualStage.label()).append("）\n");
        sb.append("变：").append(p.changedStage.relation).append("（").append(p.changedStage.label()).append("）\n");
        sb.append("路径：").append(p.path).append("｜").append(p.pathClass).append('\n');
        sb.append("结构分：").append(String.format(Locale.CHINA, "%.2f", p.structureScore)).append("（正=上方结构，负=下方结构）\n\n");

        sb.append("【A轨：无方向比赛形态】\n");
        sb.append("胜负形态：").append(p.winShape).append('\n');
        sb.append("平局倾向：").append(p.drawTendency).append('\n');
        sb.append("大小：").append(p.sizeSignal).append('\n');
        sb.append("单双：").append(p.oddEvenSignal).append('\n');
        sb.append("总进球：").append(p.totalGoalsSignal).append("球信号\n");
        sb.append(p.bttsSignal).append("｜").append(p.cleanSheetSignal).append('\n');
        sb.append("净胜球：").append(p.goalDiffSignal == 0 ? "平局结构" : p.goalDiffSignal + "球差倾向").append('\n');
        sb.append("比分形态：").append(p.scoreShape).append('\n');
        sb.append("提示：").append(p.structureNote).append("\n\n");

        sb.append("【B轨：主客极性】\n");
        sb.append("B1 固定位置（下=主，上=客）：").append(p.b1).append('\n');
        sb.append("B2 动爻定体用：").append(p.b2).append('\n');
        sb.append("极性状态：").append(p.polarityStatus).append('\n');
        sb.append("胜平负：").append(p.finalDirection).append('\n');
        sb.append("不败：").append(p.unbeaten).append('\n');
        sb.append("半全场：").append(p.halfFull).append('\n');
        sb.append("定向比分：").append(p.orientedScore).append('\n');
        sb.append("方向置信度：").append(p.confidence).append("\n\n");

        sb.append("【礼法代理层】\n");
        sb.append("本版纯随机盲测未取得时间当令/藏象节律/现场外应等完整信息，自动记为：礼法模糊 G=0，不参与修正。\n\n");
        sb.append("最终冻结原则：先看A轨结构；B1/B2冲突时不强行选边。");
        return sb.toString();
    }

    private void freezeCurrent() {
        if (latest == null) {
            Toast.makeText(this, "请先起盘", Toast.LENGTH_SHORT).show();
            return;
        }
        frozen = latest;
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
        frozenId = sha12(frozen.digits + "|" + now + "|" + frozen.compactSummary());
        String home = safeTeam(homeTeam.getText().toString(), "主队");
        String away = safeTeam(awayTeam.getText().toString(), "客队");
        String block = "[冻结 " + now + "] " + frozenId + "\n" + home + " vs " + away + "\n" + frozen.compactSummary();
        appendHistory(block);
        prefs.edit().putString(KEY_FROZEN_DIGITS, frozen.digits).putString(KEY_FROZEN_ID, frozenId).apply();
        freezeStatus.setText("已冻结：" + frozen.digits + "｜" + frozenId);
        freezeStatus.setTextColor(accent);
        Toast.makeText(this, "本场已冻结，赛果出来后只复盘不改判", Toast.LENGTH_LONG).show();
        loadHistory();
    }

    private void reviewFrozen() {
        if (frozen == null) {
            Toast.makeText(this, "没有可复盘的冻结盘", Toast.LENGTH_SHORT).show();
            return;
        }
        String hs = homeScore.getText().toString().trim();
        String as = awayScore.getText().toString().trim();
        if (hs.isEmpty() || as.isEmpty()) {
            Toast.makeText(this, "请输入双方最终进球数", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int h = Integer.parseInt(hs);
            int a = Integer.parseInt(as);
            if (h < 0 || a < 0 || h > 30 || a > 30) throw new NumberFormatException();
            String result = MeihuaEngine.review(frozen, h, a);
            reviewView.setText("冻结标记：" + frozenId + "\n" + result);
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
            appendHistory("[复盘 " + now + "] " + frozenId + "\n" + result.trim());
            loadHistory();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "比分输入无效", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyPrediction() {
        if (latest == null) {
            Toast.makeText(this, "暂无预测可复制", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("梅花足球盲测", predictionView.getText()));
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }

    private void restoreFrozen() {
        String d = prefs.getString(KEY_FROZEN_DIGITS, "");
        frozenId = prefs.getString(KEY_FROZEN_ID, "");
        if (d != null && d.matches("\\d{3}")) {
            frozen = MeihuaEngine.analyze(d);
            freezeStatus.setText("已恢复上次冻结：" + d + (frozenId.isEmpty() ? "" : "｜" + frozenId));
            freezeStatus.setTextColor(accent);
        }
    }

    private void appendHistory(String block) {
        String old = prefs.getString(KEY_HISTORY, "");
        String next = block + (old == null || old.isEmpty() ? "" : "\n\n────────────\n\n" + old);
        if (next.length() > 60000) next = next.substring(0, 60000);
        prefs.edit().putString(KEY_HISTORY, next).apply();
    }

    private void loadHistory() {
        String history = prefs.getString(KEY_HISTORY, "");
        historyView.setText(history == null || history.isEmpty() ? "暂无记录" : history);
    }

    private String sha12(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format(Locale.US, "%02x", b));
            return sb.substring(0, 12);
        } catch (Exception e) {
            return Long.toHexString(System.currentTimeMillis());
        }
    }

    private String safeTeam(String s, String fallback) {
        s = s == null ? "" : s.trim();
        return s.isEmpty() ? fallback : s;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(14), dp(14), dp(14), dp(14));
        l.setBackground(roundRect(card, border, 16));
        return l;
    }

    private TextView sectionTitle(String s) {
        TextView v = text(s, 18, Typeface.BOLD, ink);
        v.setPadding(0, 0, 0, dp(10));
        return v;
    }

    private EditText edit(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setTextColor(ink);
        e.setHintTextColor(Color.rgb(148, 163, 184));
        e.setInputType(type);
        e.setSingleLine(true);
        e.setPadding(dp(12), 0, dp(12), 0);
        e.setBackground(roundRect(Color.WHITE, border, 12));
        return e;
    }

    private Button button(String s, int color) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(roundRect(color, color, 12));
        return b;
    }

    private TextView text(String s, int sp, int style, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style);
        return t;
    }

    private GradientDrawable roundRect(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams marginBottom(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, this.dp(dp));
        return lp;
    }

    private LinearLayout.LayoutParams marginTop(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, this.dp(dp), 0, 0);
        return lp;
    }

    private LinearLayout.LayoutParams fieldLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, 0, 0, dp(9));
        return lp;
    }

    private LinearLayout.LayoutParams weightFieldLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        return lp;
    }

    private LinearLayout.LayoutParams weightButtonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        return lp;
    }

    private LinearLayout.LayoutParams fullButtonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        lp.setMargins(0, dp(10), 0, 0);
        return lp;
    }
}
