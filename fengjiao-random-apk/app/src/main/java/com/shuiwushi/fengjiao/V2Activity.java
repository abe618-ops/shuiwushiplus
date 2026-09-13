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

public class V2Activity extends Activity {
    private final SecureRandom random = new SecureRandom();
    private EditText seedInput, hostName, guestName;
    private TextView verdict, halfFull, goals, score, meta, core, numbers, detail, seal;
    private FengJiaoEngine.Reading reading;
    private FootballPrediction prediction;

    private final int bg=Color.rgb(246,243,235), ink=Color.rgb(40,37,32), red=Color.rgb(143,48,40);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(57,52,45));
        getWindow().setNavigationBarColor(bg);
        setContentView(buildUi());
        drawRandom();
    }

    private View buildUi() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(bg);
        LinearLayout root=col(); root.setPadding(dp(16),dp(18),dp(16),dp(32)); scroll.addView(root);

        TextView title=text("风角数理足球",28,true,ink); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub=text("随机日期时刻 × 风角主客 × 太玄/五行/大衍/周易｜V2",13,false,Color.DKGRAY);
        sub.setGravity(Gravity.CENTER); sub.setPadding(0,dp(4),0,dp(14)); root.addView(sub);

        LinearLayout input=card(); root.addView(input,margin(-1,-2,0,0,0,12));
        LinearLayout teams=new LinearLayout(this); teams.setOrientation(LinearLayout.HORIZONTAL);
        hostName=inputBox("主队（可选）"); guestName=inputBox("客队（可选）");
        teams.addView(hostName,new LinearLayout.LayoutParams(0,dp(48),1));
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(0,dp(48),1); gp.setMargins(dp(8),0,0,0); teams.addView(guestName,gp);
        input.addView(teams);
        seedInput=inputBox("16位十六进制种子"); input.addView(seedInput,margin(-1,dp(48),0,8,0,8));
        LinearLayout buttons=new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button draw=button("随机起盘",red), replay=button("按种子复现",Color.rgb(68,75,69));
        buttons.addView(draw,new LinearLayout.LayoutParams(0,dp(48),1));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(48),1); bp.setMargins(dp(8),0,0,0); buttons.addView(replay,bp); input.addView(buttons);

        LinearLayout result=card(); root.addView(result,margin(-1,-2,0,0,0,12));
        verdict=bigResult(); halfFull=bigResult(); goals=bigResult(); score=bigResult();
        result.addView(verdict); result.addView(halfFull); result.addView(goals); result.addView(score);
        meta=text("",13,false,Color.DKGRAY); meta.setGravity(Gravity.CENTER); meta.setPadding(0,dp(8),0,0); result.addView(meta);

        LinearLayout signal=card(); root.addView(signal,margin(-1,-2,0,0,0,12));
        signal.addView(text("风角主客",16,true,ink));
        core=text("",14,false,ink); core.setLineSpacing(dp(3),1f); core.setPadding(0,dp(8),0,0); signal.addView(core);
        detail=text("",14,false,ink); detail.setLineSpacing(dp(3),1f); detail.setPadding(0,dp(8),0,0); signal.addView(detail);

        LinearLayout num=card(); root.addView(num,margin(-1,-2,0,0,0,12));
        num.addView(text("四法取数",16,true,ink));
        numbers=text("",16,true,red); numbers.setPadding(0,dp(8),0,0); num.addView(numbers);
        TextView rule=text("总进球统一映射为0～6球：太玄票、五行票、大衍票、周易票独立计算；四票排序后取中间两票平均并向下取整。若全场判平而总进球为奇数，自动选四票总偏差更小的相邻偶数。",13,false,Color.DKGRAY);
        rule.setLineSpacing(dp(3),1f); rule.setPadding(0,dp(8),0,0); num.addView(rule);

        LinearLayout action=new LinearLayout(this); action.setOrientation(LinearLayout.HORIZONTAL);
        Button copy=button("复制本盘",Color.rgb(78,70,59)), history=button("最近20盘",Color.rgb(78,70,59));
        action.addView(copy,new LinearLayout.LayoutParams(0,dp(46),1));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,dp(46),1); hp.setMargins(dp(8),0,0,0); action.addView(history,hp); root.addView(action,margin(-1,-2,0,0,0,12));

        LinearLayout note=card(); note.addView(text("冻结说明",15,true,ink));
        TextView nt=text("胜平负以日纳音为客、时与风为主：主克客+2、客生主+1、同气0、主生客-1、客克主-2；时风同气同向再±1。势差≥2主胜，≤-2客胜，-1～1平/胶着。半场只取时主直接相克信号，全场再叠加风主。因此可以自然形成胜/平、平/胜、负/胜等半全场。传统术数实验不等于科学预测。",13,false,Color.DKGRAY);
        nt.setLineSpacing(dp(3),1f); nt.setPadding(0,dp(6),0,0); note.addView(nt); root.addView(note);

        seal=text("",12,false,Color.GRAY); seal.setPadding(0,dp(12),0,0); root.addView(seal);

        draw.setOnClickListener(v->drawRandom()); replay.setOnClickListener(v->reproduce());
        copy.setOnClickListener(v->copyCurrent()); history.setOnClickListener(v->showHistory());
        return scroll;
    }

    private void drawRandom() {
        long seed=random.nextLong(); seedInput.setText(String.format(Locale.ROOT,"%016X",seed)); render(FengJiaoEngine.draw(seed),true);
    }

    private void reproduce() {
        String s=seedInput.getText().toString().trim().replace("0x","").replace("0X","");
        try {
            if(s.isEmpty()||s.length()>16) throw new NumberFormatException();
            render(FengJiaoEngine.draw(Long.parseUnsignedLong(s,16)),false);
        } catch(Exception e) { Toast.makeText(this,"请输入1～16位十六进制种子",Toast.LENGTH_SHORT).show(); }
    }

    private void render(FengJiaoEngine.Reading r, boolean save) {
        reading=r; prediction=FootballPrediction.from(r);
        verdict.setText("胜平负　"+prediction.fullResult+" · "+prediction.strength+"（势差 "+String.format(Locale.CHINA,"%+d",prediction.momentum)+"）");
        halfFull.setText("半全场　"+prediction.halfFull);
        goals.setText("进球数　"+prediction.goalTotal+"球　〔"+prediction.goalAgreement+"〕");
        score.setText("比分　"+prediction.primaryScore+"　备 "+prediction.alternateScores);
        meta.setText(r.dateTime.toString().replace('T',' ')+"　｜　种子 "+String.format(Locale.ROOT,"%016X",r.seed));
        core.setText("客（日）  "+r.dayGanzhi+" · "+r.dayNaYin.name+" · "+r.dayNaYin.element.tone+r.dayNaYin.element.zh+"\n"+
                "主①（时） "+r.hourGanzhi+" · "+r.hourNaYin.name+" · "+r.hourNaYin.element.tone+r.hourNaYin.element.zh+"\n"+
                "主②（风） "+r.wind.degrees+"° · "+r.wind.direction+r.wind.branch+" · "+r.wind.element.tone+r.wind.element.zh);
        detail.setText("生克："+prediction.relationDetail+"\n取数："+prediction.numberDetail);
        numbers.setText("太玄 "+prediction.taiXuanVote+"　五行 "+prediction.fiveElementVote+"　大衍 "+prediction.dayanVote+"　周易 "+prediction.zhouyiVote+"\n核心区间 "+prediction.goalCoreRange+" → 主数 "+prediction.goalTotal);
        seal.setText("V2密封哈希："+prediction.seal+"　｜　V1底盘："+r.seal);
        if(save) saveHistory(currentText());
    }

    private String currentText() {
        if(reading==null||prediction==null) return "";
        String hn=hostName.getText().toString().trim(), gn=guestName.getText().toString().trim();
        String match=(hn.isEmpty()&&gn.isEmpty())?"":("比赛："+(hn.isEmpty()?"主队":hn)+" vs "+(gn.isEmpty()?"客队":gn)+"\n");
        return match+"风角数理足球 V2\n种子："+String.format(Locale.ROOT,"%016X",reading.seed)+"\n随机时刻："+reading.dateTime.toString().replace('T',' ')+
                "\n胜平负："+prediction.fullResult+"（"+prediction.strength+"，势差 "+prediction.momentum+"）\n半全场："+prediction.halfFull+
                "\n进球数："+prediction.goalTotal+"球（四票 "+prediction.taiXuanVote+"/"+prediction.fiveElementVote+"/"+prediction.dayanVote+"/"+prediction.zhouyiVote+"）"+
                "\n比分："+prediction.primaryScore+"；备："+prediction.alternateScores+"\n"+prediction.relationDetail+"\n"+prediction.numberDetail+"\n密封："+prediction.seal;
    }

    private TextView bigResult() { TextView v=text("—",18,true,red); v.setGravity(Gravity.CENTER); v.setPadding(dp(8),dp(9),dp(8),dp(9)); return v; }
    private EditText inputBox(String hint) { EditText e=new EditText(this); e.setHint(hint); e.setSingleLine(true); e.setTextSize(15); e.setInputType(InputType.TYPE_CLASS_TEXT); return e; }
    private LinearLayout col(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout card(){ LinearLayout l=col(); l.setPadding(dp(15),dp(15),dp(15),dp(15)); GradientDrawable g=new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp(16)); g.setStroke(dp(1),Color.rgb(229,224,214)); l.setBackground(g); return l; }
    private TextView text(String s,float sp,boolean bold,int color){ TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return v; }
    private Button button(String s,int color){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setAllCaps(false); GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(12)); b.setBackground(g); return b; }
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(dp(l),dp(t),dp(r),dp(b)); return p; }
    private int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density+0.5f); }

    private void copyCurrent(){ if(reading==null)return; ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE); cm.setPrimaryClip(ClipData.newPlainText("风角数理足球V2",currentText())); Toast.makeText(this,"已复制",Toast.LENGTH_SHORT).show(); }
    private void saveHistory(String s){ String old=getSharedPreferences("fjv2",0).getString("history",""); String[] a=old.isEmpty()?new String[0]:old.split("\\u001E",-1); StringBuilder b=new StringBuilder(s); for(int i=0;i<a.length&&i<19;i++) if(!a[i].isEmpty()) b.append('\u001E').append(a[i]); getSharedPreferences("fjv2",0).edit().putString("history",b.toString()).apply(); }
    private void showHistory(){ String old=getSharedPreferences("fjv2",0).getString("history",""); if(old.isEmpty()){Toast.makeText(this,"暂无历史",Toast.LENGTH_SHORT).show();return;} String[] a=old.split("\\u001E",-1); StringBuilder b=new StringBuilder(); for(int i=0;i<a.length;i++){if(i>0)b.append("\n\n────────\n\n");b.append("#").append(i+1).append("\n").append(a[i]);} TextView t=text(b.toString(),13,false,ink); t.setPadding(dp(16),dp(8),dp(16),dp(8)); t.setTextIsSelectable(true); ScrollView s=new ScrollView(this); s.addView(t); new AlertDialog.Builder(this).setTitle("最近20盘").setView(s).setPositiveButton("关闭",null).setNeutralButton("清空",(d,w)->getSharedPreferences("fjv2",0).edit().remove("history").apply()).show(); }
}
