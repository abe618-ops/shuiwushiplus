package com.shuiwushi.cezi;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

public final class CharRepository {
    public static final class Item {
        public final int index;
        public final String ch;
        public final int strokes;
        public final String radical;
        public final String structure;
        public final int frequency;

        Item(int index, String ch, int strokes, String radical, String structure, int frequency) {
            this.index = index;
            this.ch = ch;
            this.strokes = strokes;
            this.radical = radical == null || radical.isEmpty() ? "—" : radical;
            this.structure = structure == null || structure.isEmpty() ? "未知" : structure;
            this.frequency = frequency;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final Map<String, Item> byChar = new HashMap<>();

    public static CharRepository load(Context context) {
        CharRepository repo = new CharRepository();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                context.getAssets().open("char_meta.tsv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\t", -1);
                if (p.length < 6) continue;
                try {
                    int index = Integer.parseInt(p[0]);
                    int strokes = Integer.parseInt(p[2]);
                    int frequency = Integer.parseInt(p[5]);
                    Item item = new Item(index, p[1], strokes, p[3], p[4], frequency);
                    repo.items.add(item);
                    repo.byChar.put(item.ch, item);
                } catch (NumberFormatException ignored) { }
            }
        } catch (Exception e) {
            throw new IllegalStateException("无法读取内置字库 char_meta.tsv", e);
        }
        if (repo.items.size() < 6500) {
            throw new IllegalStateException("字库数量异常：" + repo.items.size());
        }
        return repo;
    }

    public int size() { return items.size(); }

    public Item bySeed(long seed) {
        SplittableRandom random = new SplittableRandom(seed ^ 0x5C6A9D3E2F1847B1L);
        return items.get(random.nextInt(items.size()));
    }

    public Item byChar(String ch) { return byChar.get(ch); }

    public static String structureName(String code) {
        if (code == null) return "未知";
        if (code.startsWith("D")) return "独体/镶嵌";
        if (code.startsWith("A")) return "品字形";
        if (code.startsWith("B")) return "上下";
        if (code.startsWith("E")) return "上中下";
        if (code.startsWith("H")) return "左右";
        if (code.startsWith("M")) return "左中右";
        if (code.startsWith("Q")) return "全包围";
        if (code.startsWith("R")) return "半包围";
        return code;
    }

    public static String frequencyName(int f) {
        switch (f) {
            case 0: return "最常用";
            case 1: return "较常用";
            case 2: return "次常用";
            case 3: return "二级字";
            case 4: return "三级规范字";
            default: return "规范字";
        }
    }
}
