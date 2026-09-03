package com.quant.football;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Random;

public class DivinationEngine {
    private static final String[] SYSTEMS={"六爻","奇门遁甲","大六壬","太乙神数","梅花易数","小成图","金口诀","灵棋经","神易数","河洛九宫数"};
    public static class Bundle {
        public int exactOverCount,exactUnderCount,exactNeutralCount,randomOverCount,randomUnderCount,randomNeutralCount,totalOverCount,totalUnderCount,totalNeutralCount;
        public String overallGoal="中性",parityPick="中性",detail="";
    }
    public Bundle run(MatchInfo m,String day,String lockedSeed){
        Bundle b=new Bundle();StringBuilder details=new StringBuilder();int odd=0,even=0;
        String base=(m.matchId==null?"":m.matchId)+"|"+m.home+"|"+m.away+"|"+day+"|"+m.kickoff;
        for(String s:SYSTEMS){int e=vote(rng(base+"|TIME",s));int r=vote(rng(base+"|RAND|"+(lockedSeed==null?"":lockedSeed),s));count(b,e,true);count(b,r,false);if((e&1)==1)odd++;else even++;if((r&1)==1)odd++;else even++;details.append(s).append(" 正时").append(sig(e)).append(" / 随机").append(sig(r)).append("\n");}
        b.totalOverCount=b.exactOverCount+b.randomOverCount;b.totalUnderCount=b.exactUnderCount+b.randomUnderCount;b.totalNeutralCount=b.exactNeutralCount+b.randomNeutralCount;
        b.overallGoal=b.totalOverCount>=b.totalUnderCount+3?"偏大":b.totalUnderCount>=b.totalOverCount+3?"偏小":"中性";
        // V6.2 reverse parity experiment: mirror the raw parity result.
        b.parityPick=odd>even?"偶数":even>odd?"奇数":"中性";b.detail=details.toString();return b;
    }
    private static int vote(Random r){double x=r.nextDouble();if(x<.42)return 0;if(x>.58)return 2;return 1;}
    private static void count(Bundle b,int v,boolean exact){if(exact){if(v==0)b.exactUnderCount++;else if(v==2)b.exactOverCount++;else b.exactNeutralCount++;}else{if(v==0)b.randomUnderCount++;else if(v==2)b.randomOverCount++;else b.randomNeutralCount++;}}
    private static String sig(int x){return x==2?"偏大":x==0?"偏小":"中";}
    private static Random rng(String seed,String name){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] x=md.digest((seed+"|"+name).getBytes("UTF-8"));return new Random(ByteBuffer.wrap(x,0,8).getLong());}catch(Exception e){return new Random((seed+name).hashCode());}}
    public static String summary(Bundle b){return String.format(Locale.US,"术数20盘：大%d / 小%d / 中%d · %s · 反转奇偶%s",b.totalOverCount,b.totalUnderCount,b.totalNeutralCount,b.overallGoal,b.parityPick);}
}
