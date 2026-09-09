package com.quant.football;

import java.util.Locale;

/**
 * V4 audit-safe traditional adapter.
 * Only reproducible implemented rules are allowed to report a real chart.
 * Unimplemented traditions must never be replaced by shared random voting.
 */
public class DivinationEngine {
    public static class Bundle {
        public String overallGoal="未评估";
        public String parityPick="未评估";
        public String manualSummary="待输入";
        public String autoSummary="待生成";
        public String detail="";
        public int reproducibleCount=0;
        public int insufficientCount=0;
        public int notImplementedCount=0;
    }

    public Bundle run(MatchInfo m,String day,String lockedSeed){
        Bundle b=new Bundle();
        b.notImplementedCount=9;
        b.detail="V4 不再使用十门共用伪随机投票。可复演数字层由 MultiConsensusEngine 负责；六爻、奇门、大六壬、太乙等完整历法盘需独立适配器与完整输入。";
        return b;
    }

    public Bundle runDualNumbers(String manualNumber, String autoNumber){
        Bundle b=new Bundle();
        StringBuilder d=new StringBuilder();
        if(manualNumber!=null && !manualNumber.trim().isEmpty()){
            DualNumberFreezeEngine.MeihuaChart c=DualNumberFreezeEngine.deriveLegacyMeihua(manualNumber);
            b.manualSummary=chart(c);
            b.reproducibleCount++;
            d.append("手动轴 梅花V1: ").append(b.manualSummary).append("\n");
        }else{
            b.manualSummary="待输入";
            b.insufficientCount++;
        }
        if(autoNumber!=null && !autoNumber.trim().isEmpty()){
            DualNumberFreezeEngine.MeihuaChart c=DualNumberFreezeEngine.deriveLegacyMeihua(autoNumber);
            b.autoSummary=chart(c);
            b.reproducibleCount++;
            d.append("自动轴 梅花V1: ").append(b.autoSummary).append("\n");
        }else{
            b.autoSummary="待生成";
            b.insufficientCount++;
        }
        b.notImplementedCount=9;
        d.append("其他术数：逐门独立适配；未实现显示未实现，不生成伪盘。\n");
        d.append("正式传统层权重=").append(FusionV4Config.FORMAL_TRADITION_WEIGHT).append("，待前瞻验证晋级。");
        b.detail=d.toString();
        return b;
    }

    private static String chart(DualNumberFreezeEngine.MeihuaChart c){
        return String.format(Locale.US,"%s｜上卦%d 下卦%d 动爻%d",c.number,c.upperTrigram,c.lowerTrigram,c.movingLine);
    }

    public static String summary(Bundle b){
        return "术数V4：可复演"+b.reproducibleCount+"项 / 输入不足"+b.insufficientCount+"项 / 待独立适配"+b.notImplementedCount+"项";
    }
}
