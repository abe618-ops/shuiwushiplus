package com.abe618.liurenfootball;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MatchService {
    private static final String[] ESPN_LEAGUES = {
            "eng.1","eng.2","esp.1","ger.1","ita.1","fra.1","ned.1","por.1",
            "uefa.champions","uefa.europa","uefa.europa.conf","usa.1","bra.1","arg.1","mex.1","jpn.1"
    };

    private MatchService() {}

    static List<Models.MatchItem> fetchMatches(LocalDate date) throws Exception {
        Map<String, Models.MatchItem> merged = new LinkedHashMap<>();
        String ymd = date.format(DateTimeFormatter.BASIC_ISO_DATE);
        for (String leagueCode : ESPN_LEAGUES) {
            try {
                String url = "https://site.api.espn.com/apis/site/v2/sports/soccer/" + leagueCode
                        + "/scoreboard?dates=" + ymd + "&limit=100";
                JSONObject root = new JSONObject(get(url));
                JSONArray events = root.optJSONArray("events");
                if (events == null) continue;
                for (int i = 0; i < events.length(); i++) {
                    JSONObject e = events.optJSONObject(i);
                    if (e == null) continue;
                    Models.MatchItem m = parseEspnEvent(e, leagueCode);
                    if (m != null) merged.put(m.date + "|" + m.home + "|" + m.away, m);
                }
            } catch (Exception ignored) {
            }
        }
        if (merged.isEmpty()) {
            for (Models.MatchItem m : fetchTheSportsDb(date)) merged.put(m.date + "|" + m.home + "|" + m.away, m);
        }
        return new ArrayList<>(merged.values());
    }

    static Models.TeamMeta searchTeamMeta(String teamName) throws Exception {
        Models.TeamMeta meta = new Models.TeamMeta();
        meta.name = teamName;
        String q = URLEncoder.encode(teamName, StandardCharsets.UTF_8.name());
        JSONObject root = new JSONObject(get("https://www.thesportsdb.com/api/v1/json/123/searchteams.php?t=" + q));
        JSONArray teams = root.optJSONArray("teams");
        if (teams == null || teams.length() == 0) return meta;
        JSONObject t = teams.optJSONObject(0);
        if (t == null) return meta;
        meta.teamId = t.optString("idTeam", "");
        String y = t.optString("intFormedYear", "");
        try { if (!y.isEmpty()) meta.foundedYear = Integer.parseInt(y); } catch (Exception ignored) {}
        return meta;
    }

    private static Models.MatchItem parseEspnEvent(JSONObject e, String leagueCode) {
        JSONArray comps = e.optJSONArray("competitions");
        if (comps == null || comps.length() == 0) return null;
        JSONObject comp = comps.optJSONObject(0);
        if (comp == null) return null;
        JSONArray cs = comp.optJSONArray("competitors");
        if (cs == null) return null;
        Models.MatchItem m = new Models.MatchItem();
        m.id = e.optString("id", "");
        m.league = leagueCode;
        String dateTime = e.optString("date", "");
        if (dateTime.length() >= 10) m.date = dateTime.substring(0, 10);
        if (dateTime.length() >= 16) m.time = dateTime.substring(11, 16) + "Z";
        for (int j = 0; j < cs.length(); j++) {
            JSONObject c = cs.optJSONObject(j);
            if (c == null) continue;
            JSONObject team = c.optJSONObject("team");
            if (team == null) continue;
            String name = team.optString("displayName", team.optString("name", ""));
            String id = team.optString("id", "");
            if ("home".equalsIgnoreCase(c.optString("homeAway"))) {
                m.home = name; m.homeTeamId = id;
            } else {
                m.away = name; m.awayTeamId = id;
            }
        }
        if (m.home == null || m.home.isEmpty() || m.away == null || m.away.isEmpty()) return null;
        return m;
    }

    private static List<Models.MatchItem> fetchTheSportsDb(LocalDate date) throws Exception {
        List<Models.MatchItem> out = new ArrayList<>();
        String url = "https://www.thesportsdb.com/api/v1/json/123/eventsday.php?d=" + date + "&s=Soccer";
        JSONObject root = new JSONObject(get(url));
        JSONArray events = root.optJSONArray("events");
        if (events == null) return out;
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.optJSONObject(i);
            if (e == null) continue;
            Models.MatchItem m = new Models.MatchItem();
            m.id = e.optString("idEvent", "");
            m.league = e.optString("strLeague", "Soccer");
            m.date = e.optString("dateEvent", date.toString());
            m.time = e.optString("strTime", "");
            m.home = e.optString("strHomeTeam", "主队");
            m.away = e.optString("strAwayTeam", "客队");
            m.homeTeamId = e.optString("idHomeTeam", "");
            m.awayTeamId = e.optString("idAwayTeam", "");
            out.add(m);
        }
        return out;
    }

    private static String get(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent", "LiurenFootball/0.1 Android");
        c.setRequestProperty("Accept", "application/json");
        c.connect();
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        if (in == null) throw new IllegalStateException("HTTP " + code);
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        c.disconnect();
        if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code + ": " + sb);
        return sb.toString();
    }
}
