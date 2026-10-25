package com.blackfalcon.worldatlas;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/** Offline atlas data and search. No Android classes, so it is tested on a computer. */
final class Data {

    interface Source {
        InputStream open(String name) throws IOException;
    }

    static final class Country {
        String iso2, iso3, en, ur, ar, capital, call, cont, curCode, curEn, curUr, curAr, curSym;
        double lat, lon, popM, area;
        boolean sov;
        int subCount;
        String[] keys;
    }

    static final class Sub {
        String code, name, type, parent, country, ur, ar;
        double lat = Double.NaN, lon = Double.NaN;
        List<Sub> kids = new ArrayList<Sub>();
        String[] keys;
        boolean hasPos() { return !Double.isNaN(lat); }
    }

    static final class City {
        String name, iso, ur, ar;
        double lat, lon;
        int popK;
        boolean cap;
        String[] keys;
    }

    static final class Poly {
        boolean water;
        String iso;
        float[] xy;
    }

    static final class Line {
        float[] xy;
    }

    static final class Hit {
        static final int COUNTRY = 0, CITY = 1, SUB = 2;
        int type;
        Object ref;
        int score;
        double pop;
        String sortName;
    }

    final List<Country> countries = new ArrayList<Country>();
    final HashMap<String, Country> byIso = new HashMap<String, Country>();
    final List<Sub> subs = new ArrayList<Sub>();
    final HashMap<String, Sub> subByCode = new HashMap<String, Sub>();
    final HashMap<String, List<Sub>> topSubs = new HashMap<String, List<Sub>>();
    final List<City> cities = new ArrayList<City>();
    final HashMap<String, List<City>> citiesBy = new HashMap<String, List<City>>();
    final List<Poly> polys = new ArrayList<Poly>();
    final List<Poly> statePolys = new ArrayList<Poly>();
    final List<Line> rivers = new ArrayList<Line>();
    boolean hasBorders = false;
    boolean hasStates = false;
    boolean hasRivers = false;

    // ---------- loading ----------

    static Data load(Source src) throws IOException {
        Data d = new Data();
        for (String[] f : rows(src, "countries.tsv")) {
            if (f.length < 18) continue;
            Country c = new Country();
            c.iso2 = f[0]; c.iso3 = f[1]; c.en = f[2]; c.ur = f[3]; c.ar = f[4]; c.capital = f[5];
            c.lat = Double.parseDouble(f[6]); c.lon = Double.parseDouble(f[7]);
            c.popM = Double.parseDouble(f[8]); c.area = Double.parseDouble(f[9]);
            c.call = f[10]; c.cont = f[11]; c.sov = "1".equals(f[12]);
            c.curCode = f[13]; c.curEn = f[14]; c.curUr = f[15]; c.curAr = f[16]; c.curSym = f[17];
            c.keys = new String[]{norm(c.en), norm(c.ur), norm(c.ar), norm(c.iso2), norm(c.iso3)};
            d.countries.add(c);
            d.byIso.put(c.iso2, c);
        }
        for (String[] f : rows(src, "subdivisions.tsv")) {
            if (f.length < 9) continue;
            Sub s = new Sub();
            s.code = f[0]; s.name = f[1]; s.type = f[2]; s.parent = f[3]; s.country = f[4];
            if (!f[5].isEmpty() && !f[6].isEmpty()) {
                s.lat = Double.parseDouble(f[5]);
                s.lon = Double.parseDouble(f[6]);
            }
            s.ur = f[7]; s.ar = f[8];
            s.keys = new String[]{norm(s.name), norm(s.ur), norm(s.ar), norm(s.code)};
            d.subs.add(s);
            d.subByCode.put(s.code, s);
        }
        for (Sub s : d.subs) {
            if (s.parent.isEmpty()) {
                List<Sub> l = d.topSubs.get(s.country);
                if (l == null) {
                    l = new ArrayList<Sub>();
                    d.topSubs.put(s.country, l);
                }
                l.add(s);
            } else {
                Sub p = d.subByCode.get(s.parent);
                if (p != null) p.kids.add(s);
            }
        }
        for (Country c : d.countries) {
            List<Sub> l = d.topSubs.get(c.iso2);
            c.subCount = l == null ? 0 : l.size();
        }
        for (String[] f : rows(src, "cities.tsv")) {
            if (f.length < 8) continue;
            City c = new City();
            c.name = f[0]; c.iso = f[1];
            c.lat = Double.parseDouble(f[2]); c.lon = Double.parseDouble(f[3]);
            c.popK = Integer.parseInt(f[4]); c.cap = "1".equals(f[5]);
            c.ur = f[6]; c.ar = f[7];
            c.keys = new String[]{norm(c.name), norm(c.ur), norm(c.ar)};
            d.cities.add(c);
            List<City> l = d.citiesBy.get(c.iso);
            if (l == null) {
                l = new ArrayList<City>();
                d.citiesBy.put(c.iso, l);
            }
            l.add(c);
        }
        for (List<City> l : d.citiesBy.values()) {
            Collections.sort(l, new Comparator<City>() {
                @Override
                public int compare(City a, City b) {
                    if (a.cap != b.cap) return a.cap ? -1 : 1;
                    if (a.popK != b.popK) return b.popK - a.popK;
                    return a.name.compareTo(b.name);
                }
            });
        }
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(src.open("world.txt"), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.length() < 5 || line.charAt(0) == '#') continue;
                String[] t = line.split(" ");
                Poly p = new Poly();
                p.water = t[0].equals("W");
                p.iso = t[1];
                if (!p.iso.equals("-")) d.hasBorders = true;
                int n = (t.length - 2) / 2;
                p.xy = new float[n * 2];
                for (int i = 0; i < n * 2; i++) p.xy[i] = Float.parseFloat(t[2 + i]);
                d.polys.add(p);
            }
            r.close();
        } catch (IOException e) {
            // map without shapes still works
        }

        // Load States/Provinces (states.txt)
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(src.open("states.txt"), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.length() < 5 || line.charAt(0) == '#') continue;
                String[] t = line.split(" ");
                Poly p = new Poly();
                p.water = false;
                p.iso = t[1];
                d.hasStates = true;
                int n = (t.length - 2) / 2;
                p.xy = new float[n * 2];
                for (int i = 0; i < n * 2; i++) p.xy[i] = Float.parseFloat(t[2 + i]);
                d.statePolys.add(p);
            }
            r.close();
        } catch (IOException e) {
            // states optional
        }

        // Load Rivers (rivers.txt)
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(src.open("rivers.txt"), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.length() < 5 || line.charAt(0) == '#') continue;
                String[] t = line.split(" ");
                if (t[0].equals("R")) {
                    Line l = new Line();
                    d.hasRivers = true;
                    int n = (t.length - 1) / 2;
                    l.xy = new float[n * 2];
                    for (int i = 0; i < n * 2; i++) l.xy[i] = Float.parseFloat(t[1 + i]);
                    d.rivers.add(l);
                }
            }
            r.close();
        } catch (IOException e) {
            // rivers optional
        }

        return d;
    }

    private static List<String[]> rows(Source src, String name) throws IOException {
        List<String[]> out = new ArrayList<String[]>();
        BufferedReader r = new BufferedReader(new InputStreamReader(src.open(name), "UTF-8"));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                out.add(line.split("\t", -1));
            }
        } finally {
            r.close();
        }
        return out;
    }

    // ---------- names ----------

    static String name(Country c, int lang) {
        String s = lang == 1 ? c.ur : (lang == 2 ? c.ar : c.en);
        return (s == null || s.isEmpty()) ? c.en : s;
    }

    static String name(City c, int lang) {
        String s = lang == 1 ? c.ur : (lang == 2 ? c.ar : c.name);
        return (s == null || s.isEmpty()) ? c.name : s;
    }

    static String name(Sub s, int lang) {
        String v = lang == 1 ? s.ur : (lang == 2 ? s.ar : s.name);
        return (v == null || v.isEmpty()) ? s.name : v;
    }

    static String currency(Country c, int lang) {
        String s = lang == 1 ? c.curUr : (lang == 2 ? c.curAr : c.curEn);
        return (s == null || s.isEmpty()) ? c.curEn : s;
    }

    City capitalCity(Country c) {
        List<City> l = citiesBy.get(c.iso2);
        if (l != null) {
            for (City x : l) if (x.cap) return x;
        }
        return null;
    }

    String capitalName(Country c, int lang) {
        if (c.capital.equals("-")) return "-";
        City x = capitalCity(c);
        return x == null ? c.capital : name(x, lang);
    }

    // ---------- nearest places ----------

    City nearestCity(double lat, double lon) {
        City best = null;
        double bd = 1e18;
        for (City c : cities) {
            double d = Geo.distanceKm(lat, lon, c.lat, c.lon);
            if (d < bd) {
                bd = d;
                best = c;
            }
        }
        return best;
    }

    List<City> nearestCities(double lat, double lon, int n, City exclude) {
        final double[] dd = new double[cities.size()];
        List<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < cities.size(); i++) {
            City c = cities.get(i);
            dd[i] = c == exclude ? 1e18 : Geo.distanceKm(lat, lon, c.lat, c.lon);
            idx.add(i);
        }
        Collections.sort(idx, new Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Double.compare(dd[a], dd[b]);
            }
        });
        List<City> out = new ArrayList<City>();
        for (int i = 0; i < n && i < idx.size(); i++) out.add(cities.get(idx.get(i)));
        return out;
    }

    /** Country whose land polygon contains the point (needs the optional borders file), else null. */
    String countryAt(double lon, double lat) {
        if (!hasBorders) return null;
        for (Poly p : polys) {
            if (p.water || p.iso.equals("-")) continue;
            for (int k = -1; k <= 1; k++) {
                if (Geo.inPolygon(lon + 360.0 * k, lat, p.xy)) return p.iso;
            }
        }
        return null;
    }

    // ---------- search ----------

    static String norm(String s) {
        if (s == null || s.isEmpty()) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(t.length());
        boolean space = true;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (Character.getType(ch) == Character.NON_SPACING_MARK || ch == '\u0640') continue;
            switch (ch) {
                case '\u064A': case '\u0649': case '\u06D0': case '\u06D2': case '\u0626': ch = '\u06CC'; break;
                case '\u0643': ch = '\u06A9'; break;
                case '\u0629': case '\u06C1': case '\u06C3': case '\u06BE': ch = '\u0647'; break;
                case '\u0623': case '\u0625': case '\u0622': ch = '\u0627'; break;
                case '\u0624': ch = '\u0648'; break;
                default: break;
            }
            String rep = null;
            switch (ch) {
                case '\u0142': rep = "l"; break;
                case '\u00F8': rep = "o"; break;
                case '\u0111': rep = "d"; break;
                case '\u0131': rep = "i"; break;
                case '\u00DF': rep = "ss"; break;
                case '\u00E6': rep = "ae"; break;
                case '\u0153': rep = "oe"; break;
                case '\u00F0': rep = "d"; break;
                case '\u00FE': rep = "th"; break;
                default: break;
            }
            if (rep != null) {
                sb.append(rep);
                space = false;
            } else if (Character.isLetterOrDigit(ch)) {
                sb.append(Character.toLowerCase(ch));
                space = false;
            } else if (!space) {
                sb.append(' ');
                space = true;
            }
        }
        int n = sb.length();
        if (n > 0 && sb.charAt(n - 1) == ' ') sb.setLength(n - 1);
        return sb.toString();
    }

    private static int score(String key, String q, String[] tokens) {
        if (key.isEmpty()) return 0;
        if (key.equals(q)) return 100;
        if (key.startsWith(q)) return 85;
        if (key.contains(" " + q)) return 70;
        if (key.contains(q)) return 50;
        if (tokens.length > 1) {
            for (String t : tokens) if (!key.contains(t)) return 0;
            return 40;
        }
        return 0;
    }

    /** filter: 0 all, 1 countries, 2 states/provinces (any level), 3 cities. */
    List<Hit> search(String query, int filter, int max) {
        String q = norm(query);
        List<Hit> out = new ArrayList<Hit>();
        if (q.isEmpty()) return out;
        String[] tokens = q.split(" ");
        if (filter == 0 || filter == 1) {
            for (Country c : countries) {
                int best = 0;
                for (int i = 0; i < c.keys.length; i++) {
                    int s = score(c.keys[i], q, tokens);
                    if (i >= 3 && s < 100) s = 0; // ISO codes only on exact match
                    best = Math.max(best, s);
                }
                if (!c.capital.equals("-")) best = Math.max(best, score(norm(c.capital), q, tokens) / 2);
                if (best > 0) out.add(hit(Hit.COUNTRY, c, best, c.popM, c.en));
            }
        }
        if (filter == 0 || filter == 3) {
            for (City c : cities) {
                int best = 0;
                for (String k : c.keys) best = Math.max(best, score(k, q, tokens));
                if (best > 0) out.add(hit(Hit.CITY, c, best, c.popK / 1000.0, c.name));
            }
        }
        if (filter == 0 || filter == 2) {
            for (Sub s : subs) {
                int best = 0;
                for (int i = 0; i < s.keys.length; i++) {
                    int v = score(s.keys[i], q, tokens);
                    if (i == 3 && v < 100) v = 0;
                    best = Math.max(best, v);
                }
                if (best > 0) out.add(hit(Hit.SUB, s, best, s.parent.isEmpty() ? 1 : 0, s.name));
            }
        }
        Collections.sort(out, new Comparator<Hit>() {
            @Override
            public int compare(Hit a, Hit b) {
                if (a.score != b.score) return b.score - a.score;
                if (a.type != b.type) return a.type - b.type;
                if (a.pop != b.pop) return Double.compare(b.pop, a.pop);
                return a.sortName.compareTo(b.sortName);
            }
        });
        if (out.size() > max) return new ArrayList<Hit>(out.subList(0, max));
        return out;
    }

    private static Hit hit(int type, Object ref, int score, double pop, String sortName) {
        Hit h = new Hit();
        h.type = type;
        h.ref = ref;
        h.score = score;
        h.pop = pop;
        h.sortName = sortName;
        return h;
    }
}
