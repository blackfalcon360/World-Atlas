package com.blackfalcon.worldatlas;

import java.util.Locale;

/** Number formatting in English (0), Urdu (1), Arabic (2). Digits stay 0-9. */
final class Fmt {
    private Fmt() {}

    private static final String[] MILLION = {"million", "\u0645\u0644\u06CC\u0646", "\u0645\u0644\u064A\u0648\u0646"};
    private static final String[] BILLION = {"billion", "\u0627\u0631\u0628", "\u0645\u0644\u064A\u0627\u0631"};
    private static final String[] THOUSAND = {"thousand", "\u06C1\u0632\u0627\u0631", "\u0623\u0644\u0641"};

    /** Population given in millions. */
    static String popM(double m, int lang) {
        if (m <= 0) return "\u2014";
        if (m >= 1000) return String.format(Locale.US, "%.2f %s", m / 1000.0, BILLION[lang]);
        if (m >= 100) return String.format(Locale.US, "%.0f %s", m, MILLION[lang]);
        if (m >= 10) return String.format(Locale.US, "%.1f %s", m, MILLION[lang]);
        if (m >= 1) return String.format(Locale.US, "%.2f %s", m, MILLION[lang]);
        double people = m * 1_000_000.0;
        if (people >= 10000) return String.format(Locale.US, "%.0f %s", people / 1000.0, THOUSAND[lang]);
        if (people >= 1000) return String.format(Locale.US, "%.1f %s", people / 1000.0, THOUSAND[lang]);
        return String.format(Locale.US, "%d", Math.round(people));
    }

    /** City population given in thousands. */
    static String popK(int k, int lang) {
        if (k <= 0) return "\u2014";
        if (k >= 10000) return String.format(Locale.US, "%.1f %s", k / 1000.0, MILLION[lang]);
        if (k >= 1000) return String.format(Locale.US, "%.2f %s", k / 1000.0, MILLION[lang]);
        return String.format(Locale.US, "%d %s", k, THOUSAND[lang]);
    }

    static String area(double km2, boolean miles) {
        double v = miles ? km2 * 0.386102159 : km2;
        String u = miles ? "mi\u00B2" : "km\u00B2";
        if (v < 10) return String.format(Locale.US, "%.1f %s", v, u);
        return String.format(Locale.US, "%,d %s", Math.round(v), u);
    }

    static String dist(double km, boolean miles) {
        double v = miles ? km * 0.621371192 : km;
        String u = miles ? "mi" : "km";
        if (v < 10) return String.format(Locale.US, "%.1f %s", v, u);
        return String.format(Locale.US, "%,d %s", Math.round(v), u);
    }

    static String density(double popM, double areaKm2, boolean miles) {
        if (popM <= 0 || areaKm2 <= 0) return "\u2014";
        double perKm = popM * 1_000_000.0 / areaKm2;
        double v = miles ? perKm * 2.58998811 : perKm;
        String u = miles ? "/mi\u00B2" : "/km\u00B2";
        if (v < 10) return String.format(Locale.US, "%.1f %s", v, u);
        return String.format(Locale.US, "%,d %s", Math.round(v), u);
    }
}
