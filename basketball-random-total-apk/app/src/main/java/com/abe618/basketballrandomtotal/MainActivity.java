package com.abe618.basketballrandomtotal;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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

import java.security.SecureRandom;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "basketball_random_total";
    private final SecureRandom random = new SecureRandom();

    private EditText numberInput;
    private EditText baselineInput;
    private EditText marketLineInput;
    private LinearLayout resultCard;
    private TextView directionText;
    private TextView weightText;
    private TextView centerText;
    private TextView lineDecisionText;
    private TextView reasonsText;
    private TextView historyText;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildUi());
        renderHistory();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 247, 248));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("篮球随机数大小盘", 28, Color.rgb(24, 24, 27), Typeface.BOLD);
        root.addView(title);
        TextView subtitle = text("随机取数 → 多维逻辑合参 → 判断全场总分大小", 14,
                Color.rgb(99, 99, 102), Typeface.NORMAL);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        root.addView(subtitle);

        LinearLayout inputCard = card();
        root.addView(inputCard, matchWrap(0, 0, 0, 14));

        inputCard.addView(label("随机数"));
        numberInput = input("例如 096 / 764 / 277 / 609", InputType.TYPE_CLASS_NUMBER);
        inputCard.addView(numberInput, matchWrap(0, 6, 0, 12));

        LinearLayout two = new LinearLayout(this);
        two.setOrientation(LinearLayout.HORIZONTAL);
        inputCard.addView(two, matchWrap(0, 0, 0, 12));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        two.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        left.addView(label("基准总分"));
        baselineInput = input("221.0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        baselineInput.setText("221.0");
        left.addView(baselineInput, matchWrap(0, 6, 6, 0));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        two.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        right.addView(label("当前盘口（可选）"));
        marketLineInput = input("如 223.5", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        right.addView(marketLineInput, matchWrap(6, 6, 0, 0));

        Button randomButton = button("一键随机起数并判断", Color.rgb(24, 24, 27), Color.WHITE);
        randomButton.setOnClickListener(v -> {
            numberInput.setText(String.format(Locale.US, "%03d", random.nextInt(1000)));
            analyzeAndRender();
        });
        inputCard.addView(randomButton, matchWrap(0, 2, 0, 10));

        Button analyzeButton = button("分析当前数字", Color.rgb(238, 238, 240), Color.rgb(24, 24, 27));
        analyzeButton.setOnClickListener(v -> analyzeAndRender());
        inputCard.addView(analyzeButton);

        resultCard = card();
        resultCard.setVisibility(View.GONE);
        root.addView(resultCard, matchWrap(0, 0, 0, 14));

        TextView resultLabel = label("合参结果");
        resultCard.addView(resultLabel);
        directionText = text("", 34, Color.rgb(24, 24, 27), Typeface.BOLD);
        directionText.setGravity(Gravity.CENTER_HORIZONTAL);
        resultCard.addView(directionText, matchWrap(0, 8, 0, 4));
        weightText = text("", 15, Color.rgb(82, 82, 91), Typeface.NORMAL);
        weightText.setGravity(Gravity.CENTER_HORIZONTAL);
        resultCard.addView(weightText);
        centerText = text("", 18, Color.rgb(39, 39, 42), Typeface.BOLD);
        centerText.setPadding(0, dp(18), 0, dp(8));
        resultCard.addView(centerText);
        lineDecisionText = text("", 17, Color.rgb(39, 39, 42), Typeface.BOLD);
        resultCard.addView(lineDecisionText);
        reasonsText = text("", 14, Color.rgb(63, 63, 70), Typeface.NORMAL);
        reasonsText.setLineSpacing(0f, 1.18f);
        reasonsText.setPadding(0, dp(14), 0, 0);
        resultCard.addView(reasonsText);

        LinearLayout historyCard = card();
        root.addView(historyCard);
        LinearLayout historyHeader = new LinearLayout(this);
        historyHeader.setOrientation(LinearLayout.HORIZONTAL);
        historyHeader.setGravity(Gravity.CENTER_VERTICAL);
        historyCard.addView(historyHeader);
        TextView historyLabel = label("最近记录");
        historyHeader.addView(historyLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button clear = button("清空", Color.TRANSPARENT, Color.rgb(113, 113, 122));
        clear.setPadding(dp(12), dp(6), dp(12), dp(6));
        clear.setOnClickListener(v -> {
            prefs.edit().remove("history").apply();
            renderHistory();
        });
        historyHeader.addView(clear);
        historyText = text("暂无记录", 14, Color.rgb(82, 82, 91), Typeface.NORMAL);
        historyText.setLineSpacing(0f, 1.2f);
        historyText.setPadding(0, dp(10), 0, 0);
        historyCard.addView(historyText);

        TextView disclaimer = text("说明：本工具复刻当前随机数实验规则，仅供娱乐与方法实验，不代表统计学真实胜率或投注建议。",
                12, Color.rgb(113, 113, 122), Typeface.NORMAL);
        disclaimer.setPadding(dp(4), dp(16), dp(4), 0);
        root.addView(disclaimer);

        return scroll;
    }

    private void analyzeAndRender() {
        String raw = numberInput.getText().toString().trim();
        if (raw.isEmpty()) {
            Toast.makeText(this, "请先输入数字，或点击一键随机起数", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            NumberEngine.Result r = NumberEngine.analyze(raw);
            double baseline = parseDoubleOr(baselineInput.getText().toString(), 221.0);
            double center = r.center(baseline);
            String verdict = r.over ? "大分" : "小分";

            resultCard.setVisibility(View.VISIBLE);
            directionText.setText(verdict);
            directionText.setTextColor(r.over ? Color.rgb(185, 28, 28) : Color.rgb(29, 78, 216));
            weightText.setText(String.format(Locale.US, "内部权重：大 %d%%  ·  小 %d%%", r.overWeight, r.underWeight));
            centerText.setText(String.format(Locale.US,
                    "实验总分中枢 %.1f   ｜   核心区间 %s", center, r.range(baseline)));

            String lineRaw = marketLineInput.getText().toString().trim();
            if (!lineRaw.isEmpty()) {
                double line = Double.parseDouble(lineRaw);
                double diff = center - line;
                String lineVerdict;
                if (Math.abs(diff) < 1.0) {
                    lineVerdict = "临界盘：接近五五开";
                } else if (diff > 0) {
                    lineVerdict = "盘口判断：偏大分";
                } else {
                    lineVerdict = "盘口判断：偏小分";
                }
                lineDecisionText.setText(String.format(Locale.US, "%s（中枢相对盘口 %+.1f）", lineVerdict, diff));
            } else {
                lineDecisionText.setText("未输入盘口：当前仅给随机数方向与实验中枢");
            }

            StringBuilder details = new StringBuilder();
            for (int i = 0; i < r.reasons.size(); i++) {
                details.append("• ").append(r.reasons.get(i));
                if (i < r.reasons.size() - 1) details.append('\n');
            }
            reasonsText.setText(details.toString());

            saveHistory(String.format(Locale.US, "%s  →  %s  %d:%d  中枢%.1f",
                    r.normalized, verdict, r.overWeight, r.underWeight, center));
            renderHistory();
        } catch (Exception e) {
            Toast.makeText(this, "输入有误：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveHistory(String line) {
        String old = prefs.getString("history", "");
        String combined = line + (old.isEmpty() ? "" : "\n" + old);
        String[] rows = combined.split("\n");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(rows.length, 12); i++) {
            if (i > 0) out.append('\n');
            out.append(rows[i]);
        }
        prefs.edit().putString("history", out.toString()).apply();
    }

    private void renderHistory() {
        if (historyText == null) return;
        String h = prefs.getString("history", "");
        historyText.setText(h.isEmpty() ? "暂无记录" : h);
    }

    private double parseDoubleOr(String s, double fallback) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private LinearLayout card() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.rgb(228, 228, 231));
        v.setBackground(bg);
        return v;
    }

    private TextView label(String s) {
        return text(s, 13, Color.rgb(82, 82, 91), Typeface.BOLD);
    }

    private EditText input(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setTextSize(17);
        e.setTextColor(Color.rgb(24, 24, 27));
        e.setHintTextColor(Color.rgb(161, 161, 170));
        e.setHint(hint);
        e.setInputType(inputType);
        e.setSingleLine(true);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(250, 250, 250));
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), Color.rgb(212, 212, 216));
        e.setBackground(bg);
        return e;
    }

    private Button button(String s, int bgColor, int textColor) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(15);
        b.setTextColor(textColor);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(13));
        b.setBackground(bg);
        return b;
    }

    private TextView text(String s, int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style);
        return t;
    }

    private LinearLayout.LayoutParams matchWrap(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
