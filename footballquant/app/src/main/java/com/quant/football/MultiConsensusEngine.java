package com.quant.football;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * V4 multi-consensus feature bus.
 * It combines reproducible number families into an EXPERIMENTAL label only.
 * It never changes calibrated market/statistical probabilities in V4.0.
 */
public final class MultiConsensusEngine {
    public static final class Result {
        public String manualNumber="";
        public String autoNumber="";
        public TaiXuanNumberEngine.Feature manualFeature;
        public TaiXuanNumberEngine.Feature autoFeature;
        public String experimentalGoalSignal="中性";
        public String experimentalParitySignal="中性";
        public double experimentalGoalIndex=0;
        public double randomControlIndex=0;
        public int agreeingFamilies=0;
        public int activeFamilies=0;
        public String directionNote="数字层不单独改写胜平负";

        public String summary(){
            StringBuilder b=new StringBuilder("V4多重合参：");
            if(manualFeature!=null)b.append("手动[").append(manualFeature.summary()).append("] · ");
            if(autoFeature!=null)b.append("自动[").append(autoFeature.summary()).append("] · ");
            b.append("实验总球=").append(experimentalGoalSignal)
             .append("(").append(String.format(Locale.US,"%+.2f",experimentalGoalIndex)).append(")")
             .append(" · 实验单双=").append(experimentalParitySignal)
             .append(" · 家族同向").append(agreeingFamilies).append('/').append(activeFamilies)
             .append(" · RandomControl=").append(String.format(Locale.US,"%+.2f",randomControlIndex))
             .append(" · 正式概率权重0");
            return b.toString();
        }
    }

    public Result run(MatchInfo m,String day,String lockedSeed,String manualNumber){
        Result r=new Result();
        r.autoNumber=deriveFrozenAutoNumber(m,lockedSeed);
        r.autoFeature=TaiXuanNumberEngine.analyze(r.autoNumber,day);
        if(manualNumber!=null && !manualNumber.trim().isEmpty()){
            r.manualNumber=DualNumberFreezeEngine.normalizeManualNumber(manualNumber);
            r.manualFeature=TaiXuanNumberEngine.analyze(r.manualNumber,day);
        }

        double sum=0,weight=0;
        FamilyVotes fv=new FamilyVotes();
        sum+=axisScore(r.autoNumber,r.autoFeature,fv);weight+=axisWeight();
        if(r.manualFeature!=null){sum+=axisScore(r.manualNumber,r.manualFeature,fv);weight+=axisWeight();}
        r.experimentalGoalIndex=weight>0?sum/weight:0;
        r.experimentalGoalSignal=r.experimentalGoalIndex>=.35?"偏大":r.experimentalGoalIndex<=-.35?"偏小":"中性";
        r.experimentalParitySignal=paritySignal(r);
        r.agreeingFamilies=fv.maxAgreement();r.activeFamilies=fv.active;
        r.randomControlIndex=randomControl(m,lockedSeed);
        return r;
    }

    private static double axisWeight(){
        return FusionV4Config.EXP_MEIHUA_WEIGHT+FusionV4Config.EXP_FIVE_ELEMENT_WEIGHT+
                FusionV4Config.EXP_TAIXUAN_WEIGHT+FusionV4Config.EXP_STEM_BRANCH_WEIGHT;
    }

    private static double axisScore(String number,TaiXuanNumberEngine.Feature f,FamilyVotes fv){
        DualNumberFreezeEngine.MeihuaChart c=DualNumberFreezeEngine.deriveLegacyMeihua(number);
        double meihua=(trigramScore(c.upperTrigram)+trigramScore(c.lowerTrigram))/2.0;
        fv.add(meihua);fv.add(f.fiveElementBias);fv.add(f.taiXuanBias);fv.add(f.stemBranchBias);
        return meihua*FusionV4Config.EXP_MEIHUA_WEIGHT+
                f.fiveElementBias*FusionV4Config.EXP_FIVE_ELEMENT_WEIGHT+
                f.taiXuanBias*FusionV4Config.EXP_TAIXUAN_WEIGHT+
                f.stemBranchBias*FusionV4Config.EXP_STEM_BRANCH_WEIGHT;
    }

    private static double trigramScore(int t){
        switch(t){
            case 2:return .65; // Dui: exchange/openness
            case 3:return 1.00; // Li: finishing/release
            case 4:return .80; // Zhen: sudden acceleration
            case 5:return .25; // Xun: penetration
            case 6:return -.85; // Kan: obstruction
            case 7:return -1.00; // Gen: stop/containment
            case 8:return -.70; // Kun: absorption/slow
            case 1:default:return .15; // Qian: control, not automatically over
        }
    }

    private static String paritySignal(Result r){
        int v=parityValue(r.autoFeature);
        int n=1;
        if(r.manualFeature!=null){v+=parityValue(r.manualFeature);n++;}
        int p=mod(v,n*2);
        return p%2==1?"单":"双";
    }

    private static int parityValue(TaiXuanNumberEngine.Feature f){
        int digits=0;for(int i=0;i<f.number.length();i++)digits+=f.number.charAt(i)-'0';
        return digits+f.taiXuanState+f.stemNumber+f.branchNumber;
    }

    private static String deriveFrozenAutoNumber(MatchInfo m,String lockedSeed){
        byte[] h=hash(identity(m)+"|"+(lockedSeed==null?"":lockedSeed)+"|AUTO3");
        int x=ByteBuffer.wrap(h,0,4).getInt() & 0x7fffffff;
        return String.format(Locale.US,"%03d",x%1000);
    }

    private static double randomControl(MatchInfo m,String lockedSeed){
        byte[] h=hash(identity(m)+"|"+(lockedSeed==null?"":lockedSeed)+"|RANDOM_CONTROL");
        int x=ByteBuffer.wrap(h,4,4).getInt() & 0x7fffffff;
        return (x/(double)Integer.MAX_VALUE)*2.0-1.0;
    }

    private static String identity(MatchInfo m){
        if(m==null)return "";
        return safe(m.matchId)+"|"+safe(m.home)+"|"+safe(m.away)+"|"+safe(m.kickoff);
    }
    private static String safe(String s){return s==null?"":s;}
    private static byte[] hash(String s){try{return MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));}catch(Exception e){return new byte[32];}}
    private static int mod(int x,int m){int r=x%m;return r<0?r+m:r;}

    private static final class FamilyVotes{
        int pos=0,neg=0,neutral=0,active=0;
        void add(double x){active++;if(x>.05)pos++;else if(x<-.05)neg++;else neutral++;}
        int maxAgreement(){return Math.max(pos,Math.max(neg,neutral));}
    }

    public static String quarterTag(String formalTotalPick,String formalOddPick){
        boolean over=formalTotalPick!=null&&formalTotalPick.startsWith("大");
        boolean under=formalTotalPick!=null&&formalTotalPick.startsWith("小");
        boolean odd="奇数".equals(formalOddPick)||"单".equals(formalOddPick);
        boolean even="偶数".equals(formalOddPick)||"双".equals(formalOddPick);
        if(over&&odd)return "大单";if(over&&even)return "大双";if(under&&odd)return "小单";if(under&&even)return "小双";
        return "临界/不判";
    }
}
