package com.shuiwushi.cezi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private final SecureRandom secureRandom = new SecureRandom();
    private CharRepository repo;
    private Spinner omenSpinner;
    private EditText seedInput;
    private TextView charView, charMeta, wdl, halfFull, goals, score, markets;
    private TextView splitDetail, fiveDetail, yanqinDetail, yiDetail, goalDetail, seal;
    private CeziEngine.Result current;

    private final int bg = Color.rgb(246, 243, 235);
    private final int ink = Color.rgb(42, 38, 32);
    private final int accent = Color.rgb(137, 48, 42);
    private final int dark = Color.rgb(74, 68, 58);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(55, 50, 44));
        getWindow().setNavigationBarColor(bg);
        try {
            repo = CharRepository.load(this);
            setContentView(buildUi());
            drawRandom();
        } catch (Exception e) {
            TextView error = new TextView(this);
            error.setText("字库加载失败：\n" + e.getMessage());
            error.setTextSize(16);
            error.setPadding(dp(24), dp(40), dp(24), dp(24));
            setContentView(error);
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        LinearLayout root = col();
        root.setPadding(dp(16), dp(18), dp(16), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("测字演禽足球", 28, true, ink);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView sub = text("7000字随机库 · 拆字形 · 五行 · 演禽 · 周易｜盲测 V1", 13, false, Color.DKGRAY);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, dp(14));
        root.addView(sub);

        LinearLayout control = card();
        root.addView(control, margin(-1, -2, 0, 0, 0, 12));
        control.addView(text("外应（请在起字前选择）", 14, true, ink));
        String[] omens = {"随机外应", "动", "静", "明", "暗", "开", "合", "升", "降"};
        omenSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, omens);
        omenSpinner.setAdapter(adapter);
        control.addView(omenSpinner, margin(-1, dp(48), 0, 2, 0, 6));

        control.addView(text("随机种子", 14, true, ink));
        seedInput = new EditText(this);
        seedInput.setSingleLine(true);
        seedInput.setTextSize(16);
        seedInput.setInputType(InputType.TYPE_CLASS_TEXT);
        seedInput.setHint("16位十六进制种子");
        control.addView(seedInput, margin(-1, dp(48), 0, 2, 0, 6));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button random = button("随机起字", accent);
        Button reproduce = button("按种子复现", dark);
        row.addView(random, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, dp(48), 1);
        p2.setMargins(dp(10), 0, 0, 0);
        row.addView(reproduce, p2);
        control.addView(row);

        LinearLayout charCard = card();
        root.addView(charCard, margin(-1, -2, 0, 0, 0, 12));
        charView = text("字", 86, true, accent);
        charView.setGravity(Gravity.CENTER);
        charCard.addView(charView);
        charMeta = text("", 14, false, Color.DKGRAY);
        charMeta.setGravity(Gravity.CENTER);
        charMeta.setPadding(0, 0, 0, dp(10));
        charCard.addView(charMeta);

        wdl = outcomeBlock(); halfFull = outcomeBlock(); goals = outcomeBlock(); score = outcomeBlock(); markets = outcomeBlock();
        charCard.addView(wdl); charCard.addView(halfFull); charCard.addView(goals); charCard.addView(score); charCard.addView(markets);

        LinearLayout detail = card();
        root.addView(detail, margin(-1, -2, 0, 0, 0, 12));
        detail.addView(text("推演拆解", 16, true, ink));
        splitDetail = detailText(); fiveDetail = detailText(); yanqinDetail = detailText(); yiDetail = detailText(); goalDetail = detailText();
        detail.addView(splitDetail); detail.addView(fiveDetail); detail.addView(yanqinDetail); detail.addView(yiDetail); detail.addView(goalDetail);
        seal = text("", 13, false, Color.GRAY);
        seal.setPadding(0, dp(10), 0, 0);
        detail.addView(seal);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button copy = button("复制本盘", dark);
        Button history = button("最近20盘", dark);
        actions.addView(copy, new LinearLayout.LayoutParams(0, dp(46), 1));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, dp(46), 1);
        hp.setMargins(dp(10), 0, 0, 0);
        actions.addView(history, hp);
        root.addView(actions, margin(-1, -2, 0, 0, 0, 12));

        LinearLayout note = card();
        note.addView(text("冻结规则 / 研究说明", 15, true, ink));
        TextView noteText = text(
                "随机池从《通用规范汉字表》8105字中按原顺序筛选，过滤非BMP扩展字、20画以上复杂字及缺可靠元数据字后固定取7000字；最近64次随机抽取做防重复，但“按种子复现”始终严格按种子取同一字。" +
                "字形拆分以手机实际字体渲染后的左右、上下墨量为程序化量化；左作主、右作客，上部辅助半场。" +
                "演禽层把字形主客数映射到二十八宿，再以五行生克比较；日宿按火、月宿按水是本软件的实验性统一映射。" +
                "周易层以上下字形数取八卦及动爻。外应只调整比赛节奏/进球数，不改变胜平负，防止赛后随意改方向。" +
                "这些方法属于传统文化与算法盲测实验，未经科学验证，不构成投注建议。",
                13, false, Color.DKGRAY);
        noteText.setLineSpacing(dp(3), 1f);
        noteText.setPadding(0, dp(6), 0, 0);
        note.addView(noteText);
        root.addView(note);

        random.setOnClickListener(v -> drawRandom());
        reproduce.setOnClickListener(v -> reproduce());
        copy.setOnClickListener(v -> copyCurrent());
        history.setOnClickListener(v -> showHistory());
        return scroll;
    }

    private void drawRandom() {
        Set<String> recent = recentChars();
        long seed = secureRandom.nextLong();
        CharRepository.Item item = repo.bySeed(seed);
        int guard = 0;
        while (recent.contains(item.ch) && guard++ < 128) {
            seed = secureRandom.nextLong();
            item = repo.bySeed(seed);
        }
        seedInput.setText(String.format(Locale.ROOT, "%016X", seed));
        render(seed, item, true);
    }

    private void reproduce() {
        String s = seedInput.getText().toString().trim().replace("0x", "").replace("0X", "");
        try {
            if (s.isEmpty() || s.length() > 16) throw new NumberFormatException();
            long seed = Long.parseUnsignedLong(s, 16);
            render(seed, repo.bySeed(seed), false);
        } catch (Exception e) {
            Toast.makeText(this, "请输入1～16位十六进制种子", Toast.LENGTH_SHORT).show();
        }
    }

    private void render(long seed, CharRepository.Item item, boolean save) {
        GlyphAnalyzer.Metrics metrics = GlyphAnalyzer.analyze(item.ch);
        String omenChoice = String.valueOf(omenSpinner.getSelectedItem());
        current = CeziEngine.calculate(seed, item, metrics, omenChoice);

        charView.setText(item.ch);
        charMeta.setText(String.format(Locale.CHINA, "字库第 %d / %d ｜ %d画 ｜ 部首 %s ｜ %s ｜ %s",
                item.index, repo.size(), item.strokes, item.radical,
                CharRepository.structureName(item.structure), CharRepository.frequencyName(item.frequency)));
        wdl.setText("胜平负　" + current.winDrawLoss + " · " + current.strength + "（总势 " + signed(current.totalScore) + "）");
        halfFull.setText("半全场　" + current.halfFull + "（半场势 " + signed(current.halfScore) + "）");
        goals.setText("进球数　" + current.goalTotal + " 球　｜　五法基础 " + current.goalBase + " 球");
        score.setText("比分　　" + current.score);
        markets.setText("盘口衍生　" + current.overUnder + "　｜　" + current.oddEven + "　｜　" + current.fourWay);

        double lr = metrics.total == 0 ? 0 : metrics.left * 100.0 / metrics.total;
        double rr = metrics.total == 0 ? 0 : metrics.right * 100.0 / metrics.total;
        double tp = metrics.total == 0 ? 0 : metrics.top * 100.0 / metrics.total;
        double bp = metrics.total == 0 ? 0 : metrics.bottom * 100.0 / metrics.total;
        splitDetail.setText(String.format(Locale.CHINA,
                "拆字形｜左主 %.1f%% / 右客 %.1f%%；上 %.1f%% / 下 %.1f%%；密度 %.3f；字形票 %s",
                lr, rr, tp, bp, metrics.density, signed(current.shapeVote)));
        fiveDetail.setText("五行数｜主部 " + current.leftNumber + current.leftElement.zh +
                " / 客部 " + current.rightNumber + current.rightElement.zh + "；生克票 " + signed(current.fiveVote));
        yanqinDetail.setText("拆字演禽｜主 " + CeziEngine.mansionName(current.hostMansion) +
                "（" + CeziEngine.mansionElement(current.hostMansion).zh + "） / 客 " +
                CeziEngine.mansionName(current.guestMansion) + "（" + CeziEngine.mansionElement(current.guestMansion).zh +
                "）；票 " + signed(current.yanqinVote));
        yiDetail.setText("周易｜上卦 " + CeziEngine.trigramName(current.upperTrigram) + CeziEngine.trigramElement(current.upperTrigram).zh +
                " / 下卦 " + CeziEngine.trigramName(current.lowerTrigram) + CeziEngine.trigramElement(current.lowerTrigram).zh +
                " / 动" + current.movingLine + "爻；票 " + signed(current.yiVote));
        goalDetail.setText("进球五数｜笔画 " + current.goalVotes[0] + " · 字形 " + current.goalVotes[1] +
                " · 五行 " + current.goalVotes[2] + " · 演禽 " + current.goalVotes[3] + " · 周易 " + current.goalVotes[4] +
                "；中位数 " + current.goalBase + "；外应“" + current.omen + "”后 " + current.goalTotal + "；" + current.voteSpread);
        seal.setText("种子 " + String.format(Locale.ROOT, "%016X", seed) + "　｜　密封哈希 " + current.seal);

        if (save) {
            saveRecentChar(item.ch);
            saveHistory(current.compactText());
        }
    }

    private String signed(int x) { return x > 0 ? "+" + x : String.valueOf(x); }

    private Set<String> recentChars() {
        String old = getSharedPreferences("cezi", 0).getString("recent_chars", "");
        Set<String> set = new HashSet<>();
        if (!old.isEmpty()) {
            String[] parts = old.split("\\u001F", -1);
            for (String s : parts) if (!s.isEmpty()) set.add(s);
        }
        return set;
    }

    private void saveRecentChar(String ch) {
        String old = getSharedPreferences("cezi", 0).getString("recent_chars", "");
        String[] parts = old.isEmpty() ? new String[0] : old.split("\\u001F", -1);
        StringBuilder b = new StringBuilder(ch);
        int count = 1;
        for (String s : parts) {
            if (s.isEmpty() || s.equals(ch)) continue;
            if (count++ >= 64) break;
            b.append('\u001F').append(s);
        }
        getSharedPreferences("cezi", 0).edit().putString("recent_chars", b.toString()).apply();
    }

    private void saveHistory(String value) {
        String old = getSharedPreferences("cezi", 0).getString("history", "");
        String[] parts = old.isEmpty() ? new String[0] : old.split("\\u001E", -1);
        StringBuilder b = new StringBuilder(value);
        for (int i = 0; i < parts.length && i < 19; i++) {
            if (!parts[i].isEmpty()) b.append('\u001E').append(parts[i]);
        }
        getSharedPreferences("cezi", 0).edit().putString("history", b.toString()).apply();
    }

    private void copyCurrent() {
        if (current == null) return;
        ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("测字演禽足球", current.compactText()));
        Toast.makeText(this, "本盘已复制", Toast.LENGTH_SHORT).show();
    }

    private void showHistory() {
        String old = getSharedPreferences("cezi", 0).getString("history", "");
        if (old.isEmpty()) {
            Toast.makeText(this, "暂无历史", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] parts = old.split("\\u001E", -1);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) b.append("\n\n────────────\n\n");
            b.append("#").append(i + 1).append("\n").append(parts[i]);
        }
        TextView t = text(b.toString(), 13, false, ink);
        t.setPadding(dp(18), dp(8), dp(18), dp(8));
        t.setTextIsSelectable(true);
        ScrollView s = new ScrollView(this);
        s.addView(t);
        new AlertDialog.Builder(this)
                .setTitle("最近20盘")
                .setView(s)
                .setPositiveButton("关闭", null)
                .setNeutralButton("清空", (d, w) -> getSharedPreferences("cezi", 0).edit().remove("history").apply())
                .show();
    }

    private TextView outcomeBlock() {
        TextView v = text("", 17, true, ink);
        v.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.rgb(249, 248, 244));
        g.setCornerRadius(dp(11));
        g.setStroke(dp(1), Color.rgb(229, 224, 214));
        v.setBackground(g);
        v.setLayoutParams(margin(-1, -2, 0, 4, 0, 4));
        return v;
    }

    private TextView detailText() {
        TextView v = text("", 14, false, ink);
        v.setLineSpacing(dp(3), 1f);
        v.setPadding(0, dp(9), 0, 0);
        return v;
    }

    private LinearLayout card() {
        LinearLayout l = col();
        l.setPadding(dp(15), dp(15), dp(15), dp(15));
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.WHITE);
        g.setCornerRadius(dp(18));
        g.setStroke(dp(1), Color.rgb(232, 227, 217));
        l.setBackground(g);
        return l;
    }

    private LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView text(String s, float sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private Button button(String s, int color) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setAllCaps(false);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(12));
        b.setBackground(g);
        return b;
    }

    private LinearLayout.LayoutParams margin(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int x) {
        return (int)(x * getResources().getDisplayMetrics().density + 0.5f);
    }
}
