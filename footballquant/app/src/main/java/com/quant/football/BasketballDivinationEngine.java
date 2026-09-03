package com.quant.football;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Random;

public class BasketballDivinationEngine {
    private static final String[] NAMES={"六爻","奇门遁甲","大六壬","太乙神数","梅花易数","小成图","金口诀","灵棋经","神易数","河洛九宫数"};
    public static class Bundle {public int exactOver,exactUnder,randomOver,randomUnder,exactHome,exactAway,randomHome,randomAway;public String total="中性",side="均衡",seed="";}
    public Bundle run(BasketballMatch m,String day,String lockedSeed){Bundle b=new Bundle();b.seed=lockedSeed==null?"":lockedSeed;String id=m.matchId+"|"+m.home+"|"+m.away+"|"+day;for(String n:NAMES){vote(b,rng(id+"|TIME|"+m.kickoff,n),true);vote(b,rng(id+"|RAND|"+b.seed,n),false);}int o=b.exactOver+b.randomOver,u=b.exactUnder+b.randomUnder;int h=b.exactHome+b.randomHome,a=b.exactAway+b.randomAway;b.total=o>=u+4?"偏大":u>=o+4?"偏小":"中性";b.side=h>=a+4?"偏主":a>=h+4?"偏客":"均衡";return b;}
    private void vote(Bundle b,Random r,boolean exact){boolean over=r.nextDouble()>.5,home=r.nextDouble()>.5;if(exact){if(over)b.exactOver++;else b.exactUnder++;if(home)b.exactHome++;else b.exactAway++;}else{if(over)b.randomOver++;else b.randomUnder++;if(home)b.randomHome++;else b.randomAway++;}}
    private static Random rng(String seed,String name){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] x=md.digest((seed+"|"+name).getBytes("UTF-8"));return new Random(ByteBuffer.wrap(x,0,8).getLong());}catch(Exception e){return new Random((seed+name).hashCode());}}
    public static String summary(Bundle b){return String.format(Locale.US,"术数Shadow：总分%s（正时 大%d/小%d，随机 大%d/小%d）· 胜负%s（正时 主%d/客%d，随机 主%d/客%d）",b.total,b.exactOver,b.exactUnder,b.randomOver,b.randomUnder,b.side,b.exactHome,b.exactAway,b.randomHome,b.randomAway);}
}
