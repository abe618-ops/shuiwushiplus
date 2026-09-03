package com.quant.football;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarketDataSource {
    private final HtmlDataSource http;
    private static final Pattern TR=Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>");
    private static final Pattern TD=Pattern.compile("(?is)<td\\b[^>]*>(.*?)</td>");
    private static final Pattern FLOAT=Pattern.compile("(?<!\\d)(\\d+(?:\\.\\d{1,3})?)(?!\\d)");
    private static final Pattern TOTAL_LINE=Pattern.compile("(?<!\\d)(1\\.5|1\\.75|2(?:\\.0)?|2\\.25|2\\.5|2\\.75|3(?:\\.0)?|3\\.25|3\\.5|3\\.75|4(?:\\.0)?)(?!\\d)");

    public MarketDataSource(HtmlDataSource http){this.http=http;}

    public MarketSnapshot fetch(MatchInfo m,String externalUrl,String day) throws Exception {
        MarketSnapshot s=new MarketSnapshot();
        if(m.matchId!=null&&!m.matchId.trim().isEmpty()){
            String id=m.matchId.trim();
            try{s.bookmakers.addAll(parseEuropean(http.get("https://odds.500.com/fenxi/ouzhi-"+id+".shtml","https://odds.500.com/")));}catch(Exception ignored){}
            try{String h=http.get("https://odds.500.com/fenxi/yazhi-"+id+".shtml","https://odds.500.com/");LineInfo li=parseLineSummary(h,"亚盘");s.asianSummary=li.summary;s.asianAbsLine=li.value;}catch(Exception e){s.asianSummary="亚盘抓取失败";}
            try{String h=http.get("https://odds.500.com/fenxi/daxiao-"+id+".shtml","https://odds.500.com/");LineInfo li=parseLineSummary(h,"大小");s.totalSummary=li.summary;s.totalLine=li.value;}catch(Exception e){s.totalSummary="大小球抓取失败";}
            s.sourceNote="500百家欧赔/亚盘/大小球；独立公司优先澳门、William Hill、Ladbrokes、Bet365、Pinnacle。";
        }else s.sourceNote="缺少500比赛ID，跳过百家赔率页。";
        s.external=fetchExternal(externalUrl,m,day);
        return s;
    }

    public List<BookmakerOdds> parseEuropean(String html){
        Map<String,BookmakerOdds> found=new LinkedHashMap<>();Matcher rm=TR.matcher(html);
        while(rm.find()){
            String row=rm.group(1),plain=text(row),company=canonicalCompany(plain);if(company==null||found.containsKey(company))continue;
            List<Double> odds=new ArrayList<>();for(String c:cells(row))for(double v:numbers(text(c)))if(v>=1.05&&v<=25.0)odds.add(v);
            if(odds.size()<3)continue;BookmakerOdds b=new BookmakerOdds();b.company=company;
            if(odds.size()>=6){b.openHome=odds.get(0);b.openDraw=odds.get(1);b.openAway=odds.get(2);b.nowHome=odds.get(3);b.nowDraw=odds.get(4);b.nowAway=odds.get(5);}
            else{b.nowHome=odds.get(0);b.nowDraw=odds.get(1);b.nowAway=odds.get(2);}
            if(b.validNow())found.put(company,b);
        }return new ArrayList<>(found.values());
    }

    private LineInfo parseLineSummary(String html,String kind){
        List<String> lines=new ArrayList<>();List<Double> vals=new ArrayList<>();Matcher rm=TR.matcher(html);
        while(rm.find()){
            String plain=text(rm.group(1)),company=canonicalCompany(plain);if(company==null)continue;
            Matcher lm=TOTAL_LINE.matcher(plain);List<Double> xs=new ArrayList<>();while(lm.find())try{xs.add(Double.parseDouble(lm.group(1)));}catch(Exception ignored){}
            if("大小".equals(kind)&&!xs.isEmpty()){
                double first=xs.get(0),last=xs.get(xs.size()-1);vals.add(last);lines.add(company+":"+fmt(first)+(Math.abs(last-first)<.001?"稳":"→"+fmt(last)));
            }else if("亚盘".equals(kind)){
                double ah=parseAsian(plain);if(ah>=0){vals.add(ah);lines.add(company+":"+fmt(ah));}
            }
            if(lines.size()>=5)break;
        }
        double mean=0;for(double v:vals)mean+=v;if(!vals.isEmpty())mean/=vals.size();
        return new LineInfo(lines.isEmpty()?kind+"：未解析到目标公司盘口":kind+"："+join(lines,"；"),mean);
    }

    private ExternalModelData fetchExternal(String template,MatchInfo m,String day){
        if(template==null||template.trim().isEmpty())return null;
        try{
            String u=template.trim().replace("{matchId}",enc(m.matchId)).replace("{home}",enc(m.home)).replace("{away}",enc(m.away)).replace("{date}",enc(day));
            String raw=http.get(u,u.startsWith("https://")?u:"https://example.com/");JSONObject j=new JSONObject(raw);ExternalModelData x=new ExternalModelData();
            x.home=pick(j,"home","pHome","homeProb");x.draw=pick(j,"draw","pDraw","drawProb");x.away=pick(j,"away","pAway","awayProb");
            x.over25=pick(j,"over25","pOver25","over_2_5");x.btts=pick(j,"btts","pBtts","bttsYes");
            x.eloHome=pick(j,"eloHome","homeElo","elo_home");x.eloAway=pick(j,"eloAway","awayElo","elo_away");
            x.xgHome=pick(j,"xgHome","homeXg","xg_home");x.xgAway=pick(j,"xgAway","awayXg","xg_away");
            x.model=j.optString("model",j.optString("source","External AI"));x.note=j.optString("note","");
            double sum=x.home+x.draw+x.away;if(sum>1.5&&sum<=300){x.home/=100;x.draw/=100;x.away/=100;sum=x.home+x.draw+x.away;}if(sum>0.5&&Math.abs(sum-1)>0.02){x.home/=sum;x.draw/=sum;x.away/=sum;}
            if(x.over25>1&&x.over25<=100)x.over25/=100;if(x.btts>1&&x.btts<=100)x.btts/=100;
            return x;
        }catch(Exception e){ExternalModelData x=new ExternalModelData();x.model="External AI";x.note="外部模型调用失败："+e.getMessage();return x;}
    }

    private static double pick(JSONObject j,String...keys){for(String k:keys)if(j.has(k))return j.optDouble(k,0);return 0;}
    private static String canonicalCompany(String row){String x=row.toLowerCase(Locale.US);if(row.contains("澳门")||x.contains("macau"))return"澳门";if(row.contains("威廉")||x.contains("william hill")||x.contains("williamhill"))return"William Hill";if(row.contains("立博")||x.contains("ladbrokes"))return"Ladbrokes";if(x.contains("bet365"))return"Bet365";if(x.contains("pinnacle")||row.contains("平博"))return"Pinnacle";return null;}
    private static double parseAsian(String s){
        String x=s.replace("受让","");
        if(x.contains("两球半"))return 2.5;if(x.contains("两/两球半"))return 2.25;if(x.contains("两球"))return 2.0;if(x.contains("球半/两"))return 1.75;if(x.contains("球半"))return 1.5;if(x.contains("一/球半"))return 1.25;if(x.contains("一球"))return 1.0;if(x.contains("半/一"))return .75;if(x.contains("半球"))return .5;if(x.contains("平/半"))return .25;if(x.contains("平手"))return 0;
        Matcher m=Pattern.compile("(?<!\\d)(0(?:\\.0)?|0\\.25|0\\.5|0\\.75|1(?:\\.0)?|1\\.25|1\\.5|1\\.75|2(?:\\.0)?|2\\.25|2\\.5)(?!\\d)").matcher(x);if(m.find())try{return Math.abs(Double.parseDouble(m.group(1)));}catch(Exception ignored){}return -1;
    }
    private static List<String> cells(String row){List<String>o=new ArrayList<>();Matcher m=TD.matcher(row);while(m.find())o.add(m.group(1));return o;}
    private static List<Double> numbers(String s){List<Double>o=new ArrayList<>();Matcher m=FLOAT.matcher(s);while(m.find())try{o.add(Double.parseDouble(m.group(1)));}catch(Exception ignored){}return o;}
    private static String text(String html){if(html==null)return"";return html.replaceAll("(?is)<script.*?</script>"," ").replaceAll("(?is)<style.*?</style>"," ").replaceAll("(?is)<[^>]+>"," ").replace("&nbsp;"," ").replace("&#160;"," ").replaceAll("\\s+"," ").trim();}
    private static String enc(String x){try{return URLEncoder.encode(x==null?"":x,"UTF-8");}catch(Exception e){return"";}}
    private static String join(List<String>x,String sep){StringBuilder b=new StringBuilder();for(int i=0;i<x.size();i++){if(i>0)b.append(sep);b.append(x.get(i));}return b.toString();}
    private static String fmt(double x){return String.format(Locale.US,"%.2f",x).replaceAll("0+$","").replaceAll("\\.$","");}
    private static class LineInfo{final String summary;final double value;LineInfo(String s,double v){summary=s;value=v;}}
}
