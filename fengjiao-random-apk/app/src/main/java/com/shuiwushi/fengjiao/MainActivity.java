package com.shuiwushi.fengjiao;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.Locale;

public class MainActivity extends Activity {
    private final SecureRandom secureRandom = new SecureRandom();
    private EditText seedInput;
    private TextView resultTitle, meta, guest, hostTime, hostWind, reason, seal;
    private FengJiaoEngine.Reading current;

    private final int bg = Color.rgb(247,244,236);
    private final int ink = Color.rgb(42,38,32);
    private final int red = Color.rgb(151,55,45);
    private final int card = Color.WHITE;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(61,56,48));
        getWindow().setNavigationBarColor(bg);
        setContentView(buildUi());
        drawRandom();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        LinearLayout root = col();
        root.setPadding(dp(18),dp(20),dp(18),dp(34));
        scroll.addView(root, new ScrollView.LayoutParams(-1,-2));

        TextView title = text("风角随机起盘", 28, true, ink);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);
        TextView sub = text("随机日期 · 随机时刻 · 随机风来方｜可复现盲测版 V1", 13, false, Color.DKGRAY);
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        sub.setPadding(0,dp(4),0,dp(16));
        root.addView(sub);

        LinearLayout seedCard = card();
        root.addView(seedCard, margin(-1,-2,0,0,0,12));
        seedCard.addView(text("随机种子",14,true,ink));
        seedInput = new EditText(this);
        seedInput.setSingleLine(true);
        seedInput.setTextSize(16);
        seedInput.setInputType(InputType.TYPE_CLASS_TEXT);
        seedInput.setHint("16位十六进制，例如 7FA91234ABCD5678");
        seedCard.addView(seedInput, margin(-1,dp(48),0,6,0,6));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button random = button("随机起盘", red);
        Button reproduce = button("按种子复现", Color.rgb(70,76,70));
        row.addView(random, new LinearLayout.LayoutParams(0,dp(48),1));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0,dp(48),1); rp.setMargins(dp(10),0,0,0);
        row.addView(reproduce, rp);
        seedCard.addView(row);

        LinearLayout resultCard = card();
        root.addView(resultCard, margin(-1,-2,0,0,0,12));
        resultTitle = text("—",26,true,red); resultTitle.setGravity(Gravity.CENTER);
        resultCard.addView(resultTitle);
        meta = text("",14,false,ink); meta.setGravity(Gravity.CENTER); meta.setPadding(0,dp(4),0,dp(12));
        resultCard.addView(meta);

        guest = block(); hostTime = block(); hostWind = block();
        resultCard.addView(guest); resultCard.addView(hostTime); resultCard.addView(hostWind);
        reason = text("",15,false,ink); reason.setLineSpacing(dp(3),1f); reason.setPadding(0,dp(12),0,0);
        resultCard.addView(reason);
        seal = text("",13,false,Color.GRAY); seal.setPadding(0,dp(10),0,0);
        resultCard.addView(seal);

        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        Button copy = button("复制本盘", Color.rgb(82,74,62));
        Button history = button("最近10盘", Color.rgb(82,74,62));
        actions.addView(copy, new LinearLayout.LayoutParams(0,dp(46),1));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0,dp(46),1); hp.setMargins(dp(10),0,0,0);
        actions.addView(history,hp); root.addView(actions, margin(-1,-2,0,0,0,12));

        LinearLayout note = card();
        note.addView(text("冻结规则",15,true,ink));
        TextView noteText = text("日期范围固定为 1900-01-01～2099-12-31；随机分钟等概率；风向 0～359° 等概率。以日纳音为“客”，时纳音与随机风方为两个“主”信号；主克客偏主，客克主偏客，相生或同气取和。此版用于传统术数实验与盲测，不代表科学预测或投注建议。",13,false,Color.DKGRAY);
        noteText.setLineSpacing(dp(3),1f); noteText.setPadding(0,dp(6),0,0); note.addView(noteText);
        root.addView(note);

        random.setOnClickListener(v -> drawRandom());
        reproduce.setOnClickListener(v -> reproduce());
        copy.setOnClickListener(v -> copyCurrent());
        history.setOnClickListener(v -> showHistory());
        return scroll;
    }

    private void drawRandom() {
        long seed = secureRandom.nextLong();
        seedInput.setText(String.format(Locale.ROOT,"%016X", seed));
        render(FengJiaoEngine.draw(seed), true);
    }

    private void reproduce() {
        String s = seedInput.getText().toString().trim().replace("0x","").replace("0X","");
        try {
            if (s.isEmpty() || s.length()>16) throw new NumberFormatException();
            long seed = Long.parseUnsignedLong(s,16);
            render(FengJiaoEngine.draw(seed), false);
        } catch (Exception e) {
            Toast.makeText(this,"请输入1～16位十六进制种子",Toast.LENGTH_SHORT).show();
        }
    }

    private void render(FengJiaoEngine.Reading r, boolean save) {
        current = r;
        resultTitle.setText(r.result + (r.strength.isEmpty()?"":" · "+r.strength));
        meta.setText(r.dateTime.toString().replace('T',' ') + "   ｜   种子 " + String.format(Locale.ROOT,"%016X",r.seed));
        guest.setText("客（日）\n" + r.dayGanzhi + " · " + r.dayNaYin.name + " · " + r.dayNaYin.element.tone + r.dayNaYin.element.zh);
        hostTime.setText("主①（时）\n" + r.hourGanzhi + " · " + r.hourNaYin.name + " · " + r.hourNaYin.element.tone + r.hourNaYin.element.zh);
        hostWind.setText("主②（风）\n" + r.wind.degrees + "° · " + r.wind.direction + r.wind.branch + " · " + r.wind.element.tone + r.wind.element.zh);
        reason.setText("合参：" + r.reason);
        seal.setText("密封哈希：" + r.seal);
        if (save) saveHistory(r.compactText());
    }

    private TextView block() {
        TextView v = text("",16,true,ink); v.setPadding(dp(14),dp(12),dp(14),dp(12));
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.rgb(248,247,243)); g.setCornerRadius(dp(12)); g.setStroke(dp(1),Color.rgb(225,220,211));
        v.setBackground(g); v.setLayoutParams(margin(-1,-2,0,5,0,5)); return v;
    }

    private LinearLayout card() {
        LinearLayout l = col(); l.setPadding(dp(16),dp(16),dp(16),dp(16));
        GradientDrawable g = new GradientDrawable(); g.setColor(card); g.setCornerRadius(dp(18)); g.setStroke(dp(1),Color.rgb(232,227,217)); l.setBackground(g); return l;
    }

    private LinearLayout col() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private TextView text(String s,float sp,boolean bold,int color) { TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return v; }
    private Button button(String s,int color) { Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setAllCaps(false); GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(12)); b.setBackground(g); return b; }
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(dp(l),dp(t),dp(r),dp(b)); return p; }
    private int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density+0.5f); }

    private void copyCurrent() {
        if (current==null) return;
        ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("风角随机起盘",current.compactText()));
        Toast.makeText(this,"已复制",Toast.LENGTH_SHORT).show();
    }

    private void saveHistory(String text) {
        String old=getSharedPreferences("fj",0).getString("history","");
        String[] items=old.isEmpty()?new String[0]:old.split("\\u001E",-1);
        StringBuilder b=new StringBuilder(text);
        for(int i=0;i<items.length && i<9;i++) if(!items[i].isEmpty()) b.append('\u001E').append(items[i]);
        getSharedPreferences("fj",0).edit().putString("history",b.toString()).apply();
    }

    private void showHistory() {
        String old=getSharedPreferences("fj",0).getString("history","");
        if(old.isEmpty()){ Toast.makeText(this,"暂无历史",Toast.LENGTH_SHORT).show(); return; }
        String[] items=old.split("\\u001E",-1);
        StringBuilder b=new StringBuilder();
        for(int i=0;i<items.length;i++) {
            if(i>0)b.append("\n\n────────────\n\n");
            b.append("#").append(i+1).append("\n").append(items[i]);
        }
        TextView t=text(b.toString(),13,false,ink); t.setPadding(dp(18),dp(8),dp(18),dp(8)); t.setTextIsSelectable(true);
        ScrollView s=new ScrollView(this); s.addView(t);
        new AlertDialog.Builder(this).setTitle("最近10盘").setView(s).setPositiveButton("关闭",null).setNeutralButton("清空",(d,w)->getSharedPreferences("fj",0).edit().remove("history").apply()).show();
    }
}
