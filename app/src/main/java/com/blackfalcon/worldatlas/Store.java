package com.blackfalcon.worldatlas;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

final class Store {
    private Store() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("worldatlas", Context.MODE_PRIVATE);
    }

    static String lang(Context c) { return p(c).getString("lang", "en"); }
    static void setLang(Context c, String v) { p(c).edit().putString("lang", v).apply(); }
    static int langIdx(Context c) {
        String l = lang(c);
        return "ur".equals(l) ? 1 : ("ar".equals(l) ? 2 : 0);
    }

    static boolean dark(Context c) { return p(c).getBoolean("dark", true); }
    static void setDark(Context c, boolean v) { p(c).edit().putBoolean("dark", v).apply(); }

    static int mapTheme(Context c) { return p(c).getInt("map_theme", 0); }
    static void setMapTheme(Context c, int v) { p(c).edit().putInt("map_theme", v).apply(); }

    static boolean miles(Context c) { return p(c).getBoolean("miles", false); }
    static void setMiles(Context c, boolean v) { p(c).edit().putBoolean("miles", v).apply(); }

    static int best(Context c, int mode) { return p(c).getInt("best_" + mode, 0); }
    static void setBest(Context c, int mode, int v) { p(c).edit().putInt("best_" + mode, v).apply(); }

    /** Recent items, newest first, keys like C:PK, T:Karachi|PK, S:PK-PB. */
    static List<String> recents(Context c) {
        ArrayList<String> out = new ArrayList<String>();
        String s = p(c).getString("recent", "");
        for (String x : s.split("\n")) if (!x.isEmpty()) out.add(x);
        return out;
    }

    static void addRecent(Context c, String key) {
        List<String> l = recents(c);
        l.remove(key);
        l.add(0, key);
        while (l.size() > 12) l.remove(l.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (String x : l) sb.append(x).append('\n');
        p(c).edit().putString("recent", sb.toString()).apply();
    }
}
