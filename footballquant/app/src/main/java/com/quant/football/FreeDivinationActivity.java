package com.quant.football;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Offline generic reading UI: no fixture list, no team data, no odds. */
public class FreeDivinationActivity extends Activity {
    private final FreeDivinationEngine engine=new FreeDivinationEngine();
    private final SecureRandom rng=new SecureRandom();
    private SharedPreferences sp;
    private EditText labelInput,dayInput,manualInput;
    private TextView frozenInfo,output;
    private FreeDivinationEngine.Result current;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);sp=getSharedPreferences("free_divination_v4",MODE_PRIVATE);build();restore();
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(20));root.setBackgroundColor(Color.rgb(245,247,248));
        scroll.addView(root);

        TextView title=tv("足球多重合参 · 自由占测 V4",23,true);title.setTextColor(Color.rgb(28,64,75));root.addView(title);
        TextView sub=tv("不绑定比赛 · 不读取赛程/球队/赔率 · 双三位数冻结 · 梅花V1 + 太玄81态 + 五行数 + 干支数 + 钟律 + Random-Control",11,false);sub.setPadding(0,dp(4),0,dp(10));root.addView(sub);

        TextView warn=tv("本版只有术数/数字实验层，没有市场与统计数据，因此输出是盲测结构，不是校准概率。用于事前冻结、长期复盘。",11,true);warn.setTextColor(Color.rgb(166,91,25));warn.setPadding(dp(10),dp(8),dp(10),dp(8));warn.setBackgroundColor(Color.rgb(255,247,231));root.addView(warn);

        labelInput=input("可选：本局备注/编号（例如 测试001）",false);root.addView(labelInput);
        dayInput=input("日期 yyyy-MM-dd（用于干支/钟律）",false);root.addView(dayInput);
        manualInput=input("手动三位数 000-999（可留空，仅系统随机轴）",true);root.addView(manualInput);

        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        Button run=btn("双轴生成并冻结");Button auto=btn("仅系统轴");row.addView(run,wgt());row.addView(auto,wgt());root.addView(row);
        LinearLayout row2=new LinearLayout(this);row2.setOrientation(LinearLayout.HORIZONTAL);
        Button fresh=btn("新一局");Button copy=btn("复制结果");row2.addView(fresh,wgt());row2.addView(copy,wgt());root.addView(row2);

        frozenInfo=tv("尚未冻结",11,true);frozenInfo.setPadding(0,dp(8),0,dp(6));root.addView(frozenInfo);
        output=tv("输入三位数后生成；也可以直接点“仅系统轴”。",13,false);output.setTextIsSelectable(true);output.setPadding(dp(10),dp(10),dp(10),dp(12));output.setBackgroundColor(Color.WHITE);root.addView(output);

        TextView rules=tv("冻结规则：同一局的系统种子、系统三位数、手动三位数一旦生成即固定；刷新或重新打开不会重抽。要重新占测请点“新一局”。\n\n输出顺序：胜平负 → 假设盘口方向 → 总进球/大小 → 单双 → 大小单双 → 比分Top3 → 半全场Top3 → 家族同向度 → Random-Control。",10,false);rules.setPadding(0,dp(12),0,0);root.addView(rules);

        run.setOnClickListener(v->generate(false));auto.setOnClickListener(v->generate(true));fresh.setOnClickListener(v->newRound());copy.setOnClickListener(v->copyResult());
        setContentView(scroll);
    }

    private void restore(){
        dayInput.setText(sp.getString("day",today()));labelInput.setText(sp.getString("label",""));manualInput.setText(sp.getString("manual",""));
        if(sp.getBoolean("frozen",false)){
            try{
                current=engine.run(sp.getString("session",""),sp.getString("day",today()),sp.getString("seed",""),sp.getString("manual",""));
                render(current,true);
            }catch(Exception e){sp.edit().clear().apply();frozenInfo.setText("旧记录无法复演，已清空");}
        }
    }

    private void generate(boolean systemOnly){
        if(sp.getBoolean("frozen",false)){
            new AlertDialog.Builder(this).setTitle("本局已冻结").setMessage("为保证盲测不赛后改盘，本局不能再次重抽。请点“新一局”开始下一次占测。").setPositiveButton("关闭",null).show();return;
        }
        try{
            String manual=systemOnly?"":manualInput.getText().toString().trim();
            if(!manual.isEmpty())manual=DualNumberFreezeEngine.normalizeManualNumber(manual);
            String day=dayInput.getText().toString().trim();if(!day.matches("\\d{4}-\\d{2}-\\d{2}"))day=today();
            String session="F"+System.currentTimeMillis()+"-"+Integer.toHexString(rng.nextInt());
            String seed=Long.toHexString(rng.nextLong())+Long.toHexString(rng.nextLong());
            sp.edit().putBoolean("frozen",true).putString("session",session).putString("seed",seed).putString("day",day)
                    .putString("manual",manual).putString("label",labelInput.getText().toString().trim()).apply();
            manualInput.setText(manual);dayInput.setText(day);
            current=engine.run(session,day,seed,manual);render(current,false);
        }catch(Exception e){Toast.makeText(this,"请输入000-999，或留空使用系统轴",Toast.LENGTH_SHORT).show();}
    }

    private void render(FreeDivinationEngine.Result r,boolean restored){
        frozenInfo.setText((restored?"已恢复冻结局":"已冻结")+" · "+sp.getString("label","")+" · 日期 "+r.day+" · 局号 "+shortId(r.sessionId));
        StringBuilder b=new StringBuilder();
        b.append("【一行结论】\n").append(r.oneLine()).append("\n\n");
        b.append("【冻结数字】\n手动轴：").append(r.manualNumber.isEmpty()?"未使用":r.manualNumber).append("\n系统轴：").append(r.autoNumber).append("\n\n");
        b.append("【方向】\n胜平负：").append(r.wdl).append("　防：").append(r.wdlDefense).append("\n").append(r.handicapNote).append("\n");
        b.append("SideIndex ").append(String.format(Locale.US,"%+.2f",r.sideIndex)).append("\n\n");
        b.append("【进球结构】\n范围：").append(r.goalRange).append("　中心：").append(r.centerGoals).append("球\n");
        b.append("大小：").append(r.ou25).append("　单双：").append(r.effectiveParity).append("　四象：").append(r.quadTag).append("\n");
        b.append("GoalIndex ").append(String.format(Locale.US,"%+.2f",r.goalIndex)).append("\n\n");
        b.append("【比分】\n主推：").append(r.scorePrimary).append("\nTop3：").append(r.scoreTop3).append("\n\n");
        b.append("【半全场】\n主推：").append(r.halfFull).append("\nTop3：").append(r.halfFullTop3).append("\n\n");
        b.append("【多重合参】\n").append(r.raw.summary()).append("\n").append(r.familySummary).append("\n");
        if(r.raw.manualFeature!=null)b.append("手动：").append(r.raw.manualFeature.summary()).append("\n");
        if(r.raw.autoFeature!=null)b.append("系统：").append(r.raw.autoFeature.summary()).append("\n");
        if(!r.conflictNote.isEmpty())b.append("\n⚠ 冲突：").append(r.conflictNote).append("\n");
        b.append("\n信号等级：").append(r.grade).append("\n");
        b.append("\n说明：本版没有实际比赛/赔率输入；胜平负中的“主/客”只是未来比赛两侧的通用映射。正式使用时请在赛前先冻结，再对应到目标比赛复盘，禁止赛后换局。");
        output.setText(b.toString());
    }

    private void newRound(){
        sp.edit().clear().apply();current=null;labelInput.setText("");manualInput.setText("");dayInput.setText(today());frozenInfo.setText("尚未冻结");output.setText("新一局已准备。输入三位数后生成；也可以直接使用系统轴。");
    }
    private void copyResult(){
        if(current==null){Toast.makeText(this,"当前没有结果",Toast.LENGTH_SHORT).show();return;}
        ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("自由占测V4",output.getText()));Toast.makeText(this,"结果已复制",Toast.LENGTH_SHORT).show();
    }

    private EditText input(String hint,boolean numeric){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(13);e.setSingleLine(true);e.setPadding(dp(8),dp(8),dp(8),dp(8));if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER);return e;}
    private TextView tv(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(42,47,49));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(11);b.setMinHeight(dp(46));return b;}
    private LinearLayout.LayoutParams wgt(){return new LinearLayout.LayoutParams(0,-2,1);}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private static String today(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));return f.format(new Date());}
    private static String shortId(String s){if(s==null)return "";return s.length()>14?s.substring(0,14):s;}
}
