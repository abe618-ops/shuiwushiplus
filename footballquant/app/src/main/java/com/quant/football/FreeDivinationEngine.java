package com.quant.football;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Generic / no-match reading adapter built on top of the V4 number-family bus.
 *
 * IMPORTANT: this engine intentionally has no bookmaker, team, xG or fixture input. Therefore
 * every football mapping below is an experimental modern mapping used for blind testing only.
 * It must not be presented as a calibrated market/statistical probability model.
 */
public final class FreeDivinationEngine {
    private final MultiConsensusEngine consensus = new MultiConsensusEngine();

    public static final class Result {
        public String sessionId="",day="",manualNumber="",autoNumber="";
        public String wdl="",wdlDefense="",handicapNote="";
        public String goalRange="",ou25="",parity="",effectiveParity="",quadTag="";
        public int centerGoals=2;
        public String scorePrimary="",scoreTop3="",halfFull="",halfFullTop3="";
        public String grade="B",conflictNote="",familySummary="";
        public double sideIndex=0,goalIndex=0,randomControl=0;
        public int agreeingFamilies=0,activeFamilies=0;
        public MultiConsensusEngine.Result raw;

        public String oneLine(){
            return "胜平负 "+wdl+"（防"+wdlDefense+"）｜进球 "+goalRange+" 中心"+centerGoals+
                    "｜"+ou25+"｜"+effectiveParity+"｜"+quadTag+"｜比分 "+scoreTop3+
                    "｜半全场 "+halfFullTop3+"｜"+grade;
        }
    }

    public Result run(String sessionId,String day,String lockedSeed,String manualNumber){
        MatchInfo pseudo=new MatchInfo();
        pseudo.matchId="FREE-"+safe(sessionId);
        pseudo.home="主方";pseudo.away="客方";pseudo.kickoff=safe(day);

        MultiConsensusEngine.Result mx=consensus.run(pseudo,day,lockedSeed,manualNumber);
        Result r=new Result();r.sessionId=sessionId;r.day=day;r.raw=mx;
        r.manualNumber=mx.manualNumber;r.autoNumber=mx.autoNumber;r.goalIndex=mx.experimentalGoalIndex;
        r.randomControl=mx.randomControlIndex;r.agreeingFamilies=mx.agreeingFamilies;r.activeFamilies=mx.activeFamilies;

        double side=axisSide(mx.autoNumber);
        int axes=1;
        if(mx.manualNumber!=null&&!mx.manualNumber.isEmpty()){side+=axisSide(mx.manualNumber);axes++;}
        side/=axes;r.sideIndex=clamp(side,-1,1);

        if(r.sideIndex>=.23){r.wdl="主胜";r.wdlDefense=r.sideIndex<.42?"平":"主胜";}
        else if(r.sideIndex<=-.23){r.wdl="客胜";r.wdlDefense=r.sideIndex>-.42?"平":"客胜";}
        else {r.wdl="平";r.wdlDefense=r.sideIndex>=0?"主胜":"客胜";}

        r.handicapNote=handicapNote(r.wdl,r.sideIndex);
        goalStructure(r);
        r.parity=mx.experimentalParitySignal;
        r.effectiveParity=r.parity;
        if("平".equals(r.wdl)&&"单".equals(r.parity)){
            r.effectiveParity="双*";
            r.conflictNote="平局比分总进球必为双数；数字单双原始信号=单，比分层按胜平负结构优先并降级。";
        }
        r.quadTag=quarter(r.ou25,r.effectiveParity);
        scoreStructure(r);
        halfFullStructure(r);
        r.grade=grade(r);
        r.familySummary="家族同向 "+r.agreeingFamilies+"/"+r.activeFamilies+
                " · Side "+signed(r.sideIndex)+" · Goal "+signed(r.goalIndex)+
                " · RandomControl "+signed(r.randomControl);
        return r;
    }

    private static void goalStructure(Result r){
        double g=r.goalIndex;
        if(g<=-.60){r.centerGoals=1;r.goalRange="0-2球";r.ou25="小2.5";}
        else if(g<=-.25){r.centerGoals=2;r.goalRange="1-2球";r.ou25="小2.5";}
        else if(g<.25){r.centerGoals=2;r.goalRange="2-3球";r.ou25="大小临界";}
        else if(g<.65){r.centerGoals=3;r.goalRange="2-4球";r.ou25="大2.5";}
        else {r.centerGoals=4;r.goalRange="3-5球";r.ou25="大2.5";}
    }

    private static String handicapNote(String wdl,double s){
        if("主胜".equals(wdl))return Math.abs(s)>=.55?"无具体盘口：强侧浅让偏主方":"无具体盘口：主方不败优先，深让慎重";
        if("客胜".equals(wdl))return Math.abs(s)>=.55?"无具体盘口：强侧偏客方":"无具体盘口：客方不败优先，深让慎重";
        return "无具体盘口：平局/受让思路优先";
    }

    private static void scoreStructure(Result r){
        String[] pool;
        if("主胜".equals(r.wdl)){
            pool=r.goalIndex<-.25?new String[]{"1:0","2:0","2:1"}:
                    r.goalIndex>.45?new String[]{"3:1","2:1","3:2","4:1"}:
                            new String[]{"2:1","1:0","2:0","3:1"};
        }else if("客胜".equals(r.wdl)){
            pool=r.goalIndex<-.25?new String[]{"0:1","0:2","1:2"}:
                    r.goalIndex>.45?new String[]{"1:3","1:2","2:3","1:4"}:
                            new String[]{"1:2","0:1","0:2","1:3"};
        }else{
            pool=r.goalIndex>.45?new String[]{"2:2","3:3","1:1"}:new String[]{"1:1","0:0","2:2"};
        }
        List<ScoreRank> ranks=new ArrayList<>();
        for(String s:pool){
            int t=total(s);double cost=Math.abs(t-r.centerGoals);
            boolean odd=(t%2)==1;boolean wantOdd="单".equals(r.effectiveParity);
            boolean wantEven=r.effectiveParity.startsWith("双");
            if((wantOdd&&!odd)||(wantEven&&odd))cost+=1.10;
            ranks.add(new ScoreRank(s,cost));
        }
        Collections.sort(ranks,new Comparator<ScoreRank>(){public int compare(ScoreRank a,ScoreRank b){return Double.compare(a.cost,b.cost);}});
        StringBuilder b=new StringBuilder();for(int i=0;i<Math.min(3,ranks.size());i++){if(i>0)b.append(" / ");b.append(ranks.get(i).score);}r.scoreTop3=b.toString();r.scorePrimary=ranks.get(0).score;
    }

    private static void halfFullStructure(Result r){
        if("主胜".equals(r.wdl)){
            if(r.goalIndex<-.20){r.halfFull="平/胜";r.halfFullTop3="平/胜 > 胜/胜 > 平/平";}
            else {r.halfFull="胜/胜";r.halfFullTop3="胜/胜 > 平/胜 > 平/平";}
        }else if("客胜".equals(r.wdl)){
            if(r.goalIndex<-.20){r.halfFull="平/负";r.halfFullTop3="平/负 > 负/负 > 平/平";}
            else {r.halfFull="负/负";r.halfFullTop3="负/负 > 平/负 > 平/平";}
        }else {r.halfFull="平/平";r.halfFullTop3="平/平 > 胜/平 > 负/平";}
    }

    private static String grade(Result r){
        double ratio=r.activeFamilies>0?r.agreeingFamilies/(double)r.activeFamilies:0;
        String g=ratio>=.75?"A-":ratio>=.625?"B+":ratio>=.50?"B":"C";
        if(!r.conflictNote.isEmpty())g=down(g);
        if(Math.abs(r.sideIndex)<.10&&Math.abs(r.goalIndex)<.15)g=down(g);
        return g;
    }
    private static String down(String g){if("A-".equals(g))return "B+";if("B+".equals(g))return "B";return "C";}

    private static double axisSide(String number){
        DualNumberFreezeEngine.MeihuaChart c=DualNumberFreezeEngine.deriveLegacyMeihua(number);
        double x=(strength(c.upperTrigram)-strength(c.lowerTrigram))*.70;
        int a=number.charAt(0)-'0',b=number.charAt(1)-'0',z=number.charAt(2)-'0';
        x+=((a-b)/9.0)*.20;
        x+=((c.movingLine-3.5)/2.5)*.10;
        x+=((z%3)-1)*.05;
        return clamp(x,-1,1);
    }
    private static double strength(int t){switch(t){case 1:return 1.0;case 3:return .70;case 4:return .55;case 5:return .35;case 2:return .20;case 6:return -.30;case 7:return -.55;case 8:default:return -.75;}}
    private static String quarter(String ou,String parity){
        boolean odd="单".equals(parity),even=parity.startsWith("双");
        if(ou.startsWith("大")&&odd)return "大单";if(ou.startsWith("大")&&even)return "大双";
        if(ou.startsWith("小")&&odd)return "小单";if(ou.startsWith("小")&&even)return "小双";return "临界/不判";
    }
    private static int total(String s){String[]p=s.split(":");return Integer.parseInt(p[0])+Integer.parseInt(p[1]);}
    private static final class ScoreRank{final String score;final double cost;ScoreRank(String s,double c){score=s;cost=c;}}
    private static String signed(double x){return String.format(Locale.US,"%+.2f",x);}
    private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
    private static String safe(String s){return s==null?"":s;}
}
