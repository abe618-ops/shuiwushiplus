package com.mingyang.meihuaceguifootball;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private final SecureRandom rng = new SecureRandom();
    private LinearLayout content;
    private static final int RED = Color.rgb(213,50,32);
    private static final int GREEN = Color.rgb(0,177,93);
    private static final int DARK = Color.rgb(45,45,45);
    private static final int MUTUAL_RED = Color.rgb(235,45,63);
    private static final int BROWN = Color.rgb(157,105,52);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        build();
    }

    private int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density + .5f); }
    private TextView tv(String s, float sp, int color){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private GradientDrawable bg(int color, float radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private GradientDrawable outline(int color,float radius,int stroke){ GradientDrawable g=bg(color,radius); g.setStroke(dp(stroke),Color.rgb(230,230,230)); return g; }
    private LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(14),dp(12),dp(14),dp(12)); c.setBackground(outline(Color.WHITE,14,1)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(dp(8),dp(7),dp(8),0); c.setLayoutParams(p); return c; }

    private void build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(245,245,245));
        TextView bar=tv("‹      梅花策轨足球盘                                  ⋯",21,Color.WHITE); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(18),0,dp(18),0); bar.setTypeface(Typeface.DEFAULT,Typeface.BOLD); bar.setBackgroundColor(RED); root.addView(bar,new LinearLayout.LayoutParams(-1,dp(58)));
        ScrollView scroll=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0,0,0,dp(24)); scroll.addView(content,new ScrollView.LayoutParams(-1,-2)); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root); render(newBoard());
    }

    private void render(Board b){
        content.removeAllViews();

        LinearLayout result=card();
        TextView cap=tv("足球合参预测",15,Color.rgb(115,115,115)); result.addView(cap);
        TextView headline=tv(b.result,30,RED); headline.setTypeface(Typeface.DEFAULT,Typeface.BOLD); headline.setPadding(0,dp(4),0,0); result.addView(headline);
        TextView second=tv(b.sizeOdd+"   ·   总进球 "+b.goalTotal+"   ·   "+b.scoreTips,18,DARK); second.setPadding(0,dp(6),0,0); result.addView(second);
        TextView note=tv("条件均匀拒绝采样：第 "+b.tries+" 次命中｜本卦宫数 "+b.palaceNumber+" 与 元策数 "+b.ce[1]+" 奇偶一致",13,Color.rgb(95,95,95)); note.setPadding(0,dp(7),0,0); result.addView(note);
        Button again=new Button(this); again.setText("重新完全随机起盘"); again.setTextSize(16); again.setTextColor(Color.WHITE); again.setAllCaps(false); again.setBackground(bg(RED,24)); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(48)); bp.setMargins(0,dp(10),0,0); result.addView(again,bp); again.setOnClickListener(v->render(newBoard()));
        content.addView(result);

        LinearLayout hexCard=card();
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER); row.addView(hexColumn("本卦",b.base,b.baseName,b.palace+"宫:"+b.palaceNumber,GREEN,b.move)); row.addView(hexColumn("互卦",b.mutual,b.mutualName,"体用参照",MUTUAL_RED,0)); row.addView(hexColumn("变卦",b.changed,b.changedName,"动爻:"+b.move,GREEN,0)); hexCard.addView(row);
        TextView relation=tv("体用："+b.bodyElement+" / "+b.useElement+"    上卦="+b.upperElement+"，下卦="+b.lowerElement,14,Color.rgb(90,90,90)); relation.setGravity(Gravity.CENTER); relation.setPadding(0,dp(8),0,0); hexCard.addView(relation); content.addView(hexCard);

        LinearLayout table=card();
        TextView tableTitle=tv("策轨数盘",19,DARK); tableTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD); table.addView(tableTitle);
        HorizontalScrollView hsv=new HorizontalScrollView(this); TextView mono=tv(makeTable(b),17,DARK); mono.setTypeface(Typeface.MONOSPACE); mono.setPadding(0,dp(8),dp(16),dp(3)); hsv.addView(mono); table.addView(hsv); content.addView(table);

        LinearLayout logic=card();
        TextView lt=tv("合参判断",19,DARK); lt.setTypeface(Typeface.DEFAULT,Typeface.BOLD); logic.addView(lt);
        logic.addView(item("① 宫数×元策数", "本卦归"+b.palace+"宫（洛书数"+b.palaceNumber+"），元策数="+b.ce[1]+"；通过奇偶拒绝采样后才成盘。元数用于单双与大小："+b.sizeOdd+"。"));
        logic.addView(item("② 运×世五行", "运作主队/上盘，世作客队/下盘："+b.yunShiText+"。"));
        logic.addView(item("③ 上下卦五行", "上卦作主/上盘、下卦作客/下盘："+b.upperLowerText+"。"));
        logic.addView(item("④ 变爻观察位", "圆点所在动爻作为上盘观察位；阳动给上盘轻权重，阴动给下盘轻权重。本局第"+b.move+"爻为"+(lineAt(b.base,b.move)==1?"阳":"阴")+"爻。"));
        logic.addView(item("合计", b.scoreText));
        content.addView(logic);

        LinearLayout rules=card();
        TextView rt=tv("随机与规则说明",19,DARK); rt.setTypeface(Typeface.DEFAULT,Typeface.BOLD); rules.addView(rt);
        TextView rv=tv("• 基础数全部由 Android SecureRandom 独立抽取，nextInt(bound) 使用无取模偏差的有界均匀抽样。\n• 若“本卦宫数奇偶 ≠ 元策数奇偶”，整盘丢弃并重新抽取，直到满足；不把数字强行加1、减1或取反。\n• 宫数使用截图所对应的后天洛书数：坎1、坤2、震3、巽4、乾6、兑7、艮8、离9。\n• 足球映射是实验性合参规则，用于盲测与复盘，不把传统术数表述成已被科学验证的稳定预测器。",14,Color.rgb(85,85,85)); rv.setLineSpacing(dp(3),1f); rv.setPadding(0,dp(6),0,0); rules.addView(rv); content.addView(rules);
    }

    private View item(String a,String b){ LinearLayout x=new LinearLayout(this); x.setOrientation(LinearLayout.VERTICAL); x.setPadding(0,dp(8),0,0); TextView t1=tv(a,15,RED); t1.setTypeface(Typeface.DEFAULT,Typeface.BOLD); TextView t2=tv(b,14,Color.rgb(70,70,70)); t2.setPadding(0,dp(2),0,0); x.addView(t1); x.addView(t2); return x; }

    private View hexColumn(String top,int[] lines,String name,String sub,int color,int move){
        LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setGravity(Gravity.CENTER); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1); col.setLayoutParams(cp);
        TextView t=tv(top,18,DARK); t.setGravity(Gravity.CENTER); col.addView(t);
        HexView hv=new HexView(lines,color,move); col.addView(hv,new LinearLayout.LayoutParams(dp(120),dp(128)));
        TextView n=tv(name,17,DARK); n.setGravity(Gravity.CENTER); col.addView(n);
        TextView s=tv(sub,13,color==MUTUAL_RED?MUTUAL_RED:BROWN); s.setGravity(Gravity.CENTER); s.setPadding(0,dp(3),0,0); col.addView(s);
        return col;
    }

    class HexView extends View {
        int[] lines; int color; int move; Paint p=new Paint(1);
        HexView(int[] l,int c,int m){ super(MainActivity.this); lines=l; color=c; move=m; }
        @Override protected void onDraw(Canvas c){ super.onDraw(c); float w=getWidth(), y=dp(13); p.setStrokeWidth(dp(7)); p.setStrokeCap(Paint.Cap.SQUARE); p.setColor(color); for(int visual=5;visual>=0;visual--){ int val=lines[visual]; if(visual<3 && color==GREEN) p.setColor(BROWN); else if(color==GREEN) p.setColor(GREEN); else p.setColor(color); float left=dp(14),right=w-dp(14); if(val==1){ c.drawLine(left,y,right,y,p); } else { float mid=w/2, gap=dp(12); c.drawLine(left,y,mid-gap,y,p); c.drawLine(mid+gap,y,right,y,p); } if(move==visual+1){ p.setColor(Color.LTGRAY); c.drawCircle(right+dp(8),y,dp(5),p); } y+=dp(19); } }
    }

    private String makeTable(Board b){
        String[] heads={"万","元","会","运","世"}; StringBuilder s=new StringBuilder();
        s.append(String.format(Locale.CHINA,"%-5s", "数局")); for(String h:heads)s.append(String.format(Locale.CHINA,"%4s",h)); s.append('\n');
        s.append(String.format(Locale.CHINA,"%-5s", "策数")); for(int n:b.ce)s.append(String.format(Locale.CHINA,"%4d",n)); s.append('\n');
        s.append(String.format(Locale.CHINA,"%-5s", "演卦")); for(int n:b.ce)s.append(String.format(Locale.CHINA,"%4s",luoGua(n))); s.append('\n');
        s.append(String.format(Locale.CHINA,"%-5s", "五行")); for(int n:b.ce)s.append(String.format(Locale.CHINA,"%4s",numElement(n))); s.append('\n');
        s.append(String.format(Locale.CHINA,"%-5s", "轨数")); for(int n:b.gui)s.append(String.format(Locale.CHINA,"%4d",n)); s.append('\n');
        s.append(String.format(Locale.CHINA,"%-5s", "演卦")); for(int n:b.gui)s.append(String.format(Locale.CHINA,"%4s",luoGua(n))); s.append('\n');
        s.append(String.format(Locale.CHINA,"%-5s", "五行")); for(int n:b.gui)s.append(String.format(Locale.CHINA,"%4s",numElement(n))); return s.toString();
    }

    private static class Board {
        int[] base,mutual,changed,ce,gui; int move,tries,palaceNumber,goalTotal; String baseName,mutualName,changedName,palace,upperElement,lowerElement,bodyElement,useElement; String result,sizeOdd,scoreTips,yunShiText,upperLowerText,scoreText;
    }

    private Board newBoard(){
        Board b=new Board(); int[] ce=new int[5]; int upNum,lowNum,move; int[] base; String name,palace; int palaceNum; int tries=0;
        do{
            tries++;
            upNum=uniform(1,8); lowNum=uniform(1,8); move=uniform(1,6);
            for(int i=0;i<5;i++) ce[i]=uniform(1,9);
            base=hexFromTrigrams(trigramBits(trigramByMeihua(upNum)),trigramBits(trigramByMeihua(lowNum)));
            name=hexName(base); palace=palaceOf(name); palaceNum=palaceNumber(palace);
        }while((palaceNum&1)!=(ce[1]&1));
        b.tries=tries; b.ce=ce; b.gui=new int[5]; for(int i=0;i<5;i++) b.gui[i]=uniform(1,9); b.base=base; b.move=move; b.baseName=name; b.palace=palace; b.palaceNumber=palaceNum; b.changed=base.clone(); b.changed[move-1]^=1; b.mutual=mutual(base); b.mutualName=hexName(b.mutual); b.changedName=hexName(b.changed);
        String up=upperTrigram(base), lo=lowerTrigram(base); b.upperElement=trigramElement(up); b.lowerElement=trigramElement(lo);
        boolean movingLower=move<=3; b.useElement=movingLower?b.lowerElement:b.upperElement; b.bodyElement=movingLower?b.upperElement:b.lowerElement;
        evaluate(b); return b;
    }

    private int uniform(int min,int max){ return min+rng.nextInt(max-min+1); }

    private void evaluate(Board b){
        double home=0,away=0,draw=0;
        String yunE=numElement(b.ce[3]), shiE=numElement(b.ce[4]); Rel r1=rel(yunE,shiE);
        if(r1.kind==1){home+=2;b.yunShiText=yunE+"克"+shiE+"，主/上盘强";} else if(r1.kind==2){away+=2;b.yunShiText=shiE+"克"+yunE+"，客/下盘强";} else if(r1.kind==3){away+=1;b.yunShiText=yunE+"生"+shiE+"，力量流向客/下盘";} else if(r1.kind==4){home+=1;b.yunShiText=shiE+"生"+yunE+"，力量流向主/上盘";} else {draw+=1;b.yunShiText="同五行，比和，平局权重增加";}
        Rel r2=rel(b.upperElement,b.lowerElement);
        if(r2.kind==1){home+=2;b.upperLowerText="上"+b.upperElement+"克下"+b.lowerElement+"，主/上盘偏强";} else if(r2.kind==2){away+=2;b.upperLowerText="下"+b.lowerElement+"克上"+b.upperElement+"，客/下盘偏强";} else if(r2.kind==3){away+=1;b.upperLowerText="上"+b.upperElement+"生下"+b.lowerElement+"，力量下行，客/下盘受益";} else if(r2.kind==4){home+=1;b.upperLowerText="下"+b.lowerElement+"生上"+b.upperElement+"，力量上行，主/上盘受益";} else {draw+=1;b.upperLowerText="上下同五行，比和，平局权重增加";}
        if(lineAt(b.base,b.move)==1) home+=.75; else away+=.75;
        double diff=home-away;
        if(Math.abs(diff)<1.15 || draw>=2){ b.result="平局倾向"; } else if(diff>0){ b.result="主胜倾向"; } else { b.result="客胜倾向"; }
        int yuan=b.ce[1]; boolean odd=(yuan&1)==1; boolean big;
        if(yuan<=4) big=false; else if(yuan>=7) big=true; else big=countYang(b.base)>=4;
        b.sizeOdd=(big?"大":"小")+(odd?"单":"双");
        if(!big && odd) b.goalTotal=yuan<=2?1:3; else if(!big) b.goalTotal=yuan==1?0:2; else if(odd) b.goalTotal=yuan>=9?5:3; else b.goalTotal=yuan>=8?4:4;
        b.scoreTips=scoreTips(b.result,b.goalTotal,odd);
        b.scoreText=String.format(Locale.CHINA,"主/上盘 %.2f 分，客/下盘 %.2f 分，平局结构 %.2f 分；最终：%s，%s。",home,away,draw,b.result,b.sizeOdd);
    }

    private String scoreTips(String result,int total,boolean odd){
        if(result.startsWith("主")){
            if(total<=1)return "比分 1:0"; if(total==2)return "比分 2:0 / 1:1防"; if(total==3)return "比分 2:1 / 3:0"; if(total==4)return "比分 3:1"; return "比分 3:2 / 4:1";
        } else if(result.startsWith("客")){
            if(total<=1)return "比分 0:1"; if(total==2)return "比分 0:2 / 1:1防"; if(total==3)return "比分 1:2 / 0:3"; if(total==4)return "比分 1:3"; return "比分 2:3 / 1:4";
        } else {
            return odd?"平局与单数冲突：防1:1及一球胜负":"比分 0:0 / 1:1 / 2:2";
        }
    }

    private static class Rel{int kind;Rel(int k){kind=k;}}
    private Rel rel(String a,String b){ if(a.equals(b))return new Rel(0); if(overcomes(a,b))return new Rel(1); if(overcomes(b,a))return new Rel(2); if(generates(a,b))return new Rel(3); if(generates(b,a))return new Rel(4); return new Rel(0); }
    private boolean generates(String a,String b){ return (a.equals("木")&&b.equals("火"))||(a.equals("火")&&b.equals("土"))||(a.equals("土")&&b.equals("金"))||(a.equals("金")&&b.equals("水"))||(a.equals("水")&&b.equals("木")); }
    private boolean overcomes(String a,String b){ return (a.equals("木")&&b.equals("土"))||(a.equals("土")&&b.equals("水"))||(a.equals("水")&&b.equals("火"))||(a.equals("火")&&b.equals("金"))||(a.equals("金")&&b.equals("木")); }
    private int countYang(int[] l){int n=0;for(int x:l)n+=x;return n;}
    private int lineAt(int[] l,int move){return l[move-1];}

    private String trigramByMeihua(int n){ String[] x={"","乾","兑","离","震","巽","坎","艮","坤"}; return x[n]; }
    private int[] trigramBits(String g){ switch(g){case"乾":return new int[]{1,1,1};case"兑":return new int[]{1,1,0};case"离":return new int[]{1,0,1};case"震":return new int[]{1,0,0};case"巽":return new int[]{0,1,1};case"坎":return new int[]{0,1,0};case"艮":return new int[]{0,0,1};default:return new int[]{0,0,0};} }
    private int[] hexFromTrigrams(int[] upper,int[] lower){ return new int[]{lower[0],lower[1],lower[2],upper[0],upper[1],upper[2]}; }
    private int[] mutual(int[] h){ return new int[]{h[1],h[2],h[3],h[2],h[3],h[4]}; }
    private String bitsToTrigram(int a,int b,int c){ int[] z={a,b,c}; for(String g:Arrays.asList("乾","兑","离","震","巽","坎","艮","坤")){ if(Arrays.equals(z,trigramBits(g)))return g; } return "坤"; }
    private String upperTrigram(int[] h){return bitsToTrigram(h[3],h[4],h[5]);}
    private String lowerTrigram(int[] h){return bitsToTrigram(h[0],h[1],h[2]);}
    private String trigramElement(String g){ if(g.equals("乾")||g.equals("兑"))return "金"; if(g.equals("离"))return "火"; if(g.equals("震")||g.equals("巽"))return "木"; if(g.equals("坎"))return "水"; return "土"; }
    private String luoGua(int n){ switch(n){case 1:return"坎";case 2:return"坤";case 3:return"震";case 4:return"巽";case 5:return"中";case 6:return"乾";case 7:return"兑";case 8:return"艮";default:return"离";} }
    private String numElement(int n){ switch(n){case 1:return"水";case 2:return"土";case 3:case 4:return"木";case 5:case 8:return"土";case 6:case 7:return"金";default:return"火";} }
    private int palaceNumber(String p){ switch(p){case"坎":return 1;case"坤":return 2;case"震":return 3;case"巽":return 4;case"乾":return 6;case"兑":return 7;case"艮":return 8;default:return 9;} }

    private String hexName(int[] h){
        String k=upperTrigram(h)+lowerTrigram(h); String n=HEX.get(k); return n==null?k:n;
    }
    private String palaceOf(String name){ String p=PALACE.get(name); return p==null?upperTrigram(trigramBitsToHex(name)):p; }
    private int[] trigramBitsToHex(String unused){return new int[]{0,0,0,0,0,0};}

    private static final Map<String,String> HEX=new HashMap<>();
    private static final Map<String,String> PALACE=new HashMap<>();
    static{
        putHex("乾乾","乾为天"); putHex("兑乾","泽天夬"); putHex("离乾","火天大有"); putHex("震乾","雷天大壮"); putHex("巽乾","风天小畜"); putHex("坎乾","水天需"); putHex("艮乾","山天大畜"); putHex("坤乾","地天泰");
        putHex("乾兑","天泽履"); putHex("兑兑","兑为泽"); putHex("离兑","火泽睽"); putHex("震兑","雷泽归妹"); putHex("巽兑","风泽中孚"); putHex("坎兑","水泽节"); putHex("艮兑","山泽损"); putHex("坤兑","地泽临");
        putHex("乾离","天火同人"); putHex("兑离","泽火革"); putHex("离离","离为火"); putHex("震离","雷火丰"); putHex("巽离","风火家人"); putHex("坎离","水火既济"); putHex("艮离","山火贲"); putHex("坤离","地火明夷");
        putHex("乾震","天雷无妄"); putHex("兑震","泽雷随"); putHex("离震","火雷噬嗑"); putHex("震震","震为雷"); putHex("巽震","风雷益"); putHex("坎震","水雷屯"); putHex("艮震","山雷颐"); putHex("坤震","地雷复");
        putHex("乾巽","天风姤"); putHex("兑巽","泽风大过"); putHex("离巽","火风鼎"); putHex("震巽","雷风恒"); putHex("巽巽","巽为风"); putHex("坎巽","水风井"); putHex("艮巽","山风蛊"); putHex("坤巽","地风升");
        putHex("乾坎","天水讼"); putHex("兑坎","泽水困"); putHex("离坎","火水未济"); putHex("震坎","雷水解"); putHex("巽坎","风水涣"); putHex("坎坎","坎为水"); putHex("艮坎","山水蒙"); putHex("坤坎","地水师");
        putHex("乾艮","天山遁"); putHex("兑艮","泽山咸"); putHex("离艮","火山旅"); putHex("震艮","雷山小过"); putHex("巽艮","风山渐"); putHex("坎艮","水山蹇"); putHex("艮艮","艮为山"); putHex("坤艮","地山谦");
        putHex("乾坤","天地否"); putHex("兑坤","泽地萃"); putHex("离坤","火地晋"); putHex("震坤","雷地豫"); putHex("巽坤","风地观"); putHex("坎坤","水地比"); putHex("艮坤","山地剥"); putHex("坤坤","坤为地");
        palace("乾","乾为天","天风姤","天山遁","天地否","风地观","山地剥","火地晋","火天大有");
        palace("兑","兑为泽","泽水困","泽地萃","泽山咸","水山蹇","地山谦","雷山小过","雷泽归妹");
        palace("离","离为火","火山旅","火风鼎","火水未济","山水蒙","风水涣","天水讼","天火同人");
        palace("震","震为雷","雷地豫","雷水解","雷风恒","地风升","水风井","泽风大过","泽雷随");
        palace("巽","巽为风","风天小畜","风火家人","风雷益","天雷无妄","火雷噬嗑","山雷颐","山风蛊");
        palace("坎","坎为水","水泽节","水雷屯","水火既济","泽火革","雷火丰","地火明夷","地水师");
        palace("艮","艮为山","山火贲","山天大畜","山泽损","火泽睽","天泽履","风泽中孚","风山渐");
        palace("坤","坤为地","地雷复","地泽临","地天泰","雷天大壮","泽天夬","水天需","水地比");
    }
    private static void putHex(String k,String v){HEX.put(k,v);} private static void palace(String p,String... names){for(String n:names)PALACE.put(n,p);}
}
