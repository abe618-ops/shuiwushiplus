package com.quant.football;

import java.util.Locale;

/**
 * V3 audit-safe divination adapter.
 * Only the legacy 3-digit Meihua mapping is currently marked reproducible here.
 * Other traditions are registered but never faked with a shared random vote.
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
        b.detail="V3 已停用旧版十门共用随机投票与奇偶反转。请使用 runDualNumbers(manual, auto) 进入可复演数字盘；六爻、奇门、大六壬、太乙等完整历法盘需各自适配器与所需时间/地点输入。";
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
        d.append("其他术数：按独立适配器逐门实现；未实现时明确显示未实现，不生成伪盘。\n");
        d.append("正式融合权重=").append(FusionV3Config.FORMAL_DIVINATION_WEIGHT).append("，待前瞻验证后再升级。");
        b.detail=d.toString();
        return b;
    }

    private static String chart(DualNumberFreezeEngine.MeihuaChart c){
        return String.format(Locale.US,"%s｜上卦%d 下卦%d 动爻%d",c.number,c.upperTrigram,c.lowerTrigram,c.movingLine);
    }

    public static String summary(Bundle b){
        return "术数V3：可复演"+b.reproducibleCount+"项 / 输入不足"+b.insufficientCount+"项 / 待实现"+b.notImplementedCount+"项";
    }
}
