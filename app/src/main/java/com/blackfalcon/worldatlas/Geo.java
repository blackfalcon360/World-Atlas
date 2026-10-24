package com.blackfalcon.worldatlas;

import java.util.Locale;

/** Pure geometry helpers (no Android classes) so they can be tested on a computer. */
final class Geo {
    private Geo() {}

    static final double R = 6371.0088; // km

    static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1), dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dl = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }

    private static final String[] DIR16 = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

    static String dir16(double deg) {
        double d = deg % 360.0;
        if (d < 0) d += 360.0;
        return DIR16[(int) Math.floor((d + 11.25) / 22.5) % 16];
    }

    /** Wraps a longitude difference into [-180, 180). */
    static double wrap180(double d) {
        return ((d + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
    }

    static String dms(double value, boolean isLat) {
        char h = isLat ? (value >= 0 ? 'N' : 'S') : (value >= 0 ? 'E' : 'W');
        double a = Math.abs(value);
        int d = (int) a;
        double mFull = (a - d) * 60.0;
        int m = (int) mFull;
        double s = (mFull - m) * 60.0;
        if (Double.parseDouble(String.format(Locale.US, "%.1f", s)) >= 60.0) {
            s = 0;
            m++;
        }
        if (m >= 60) {
            m = 0;
            d++;
        }
        return String.format(Locale.US, "%d\u00B0%02d'%04.1f\"%c", d, m, s, h);
    }

    /** Ray casting; poly = lon,lat pairs. */
    static boolean inPolygon(double x, double y, float[] poly) {
        boolean c = false;
        int n = poly.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = poly[2 * i], yi = poly[2 * i + 1], xj = poly[2 * j], yj = poly[2 * j + 1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) c = !c;
        }
        return c;
    }

    static String flag(String iso2) {
        if (iso2 == null || iso2.length() != 2) return "";
        int a = 0x1F1E6 + (Character.toUpperCase(iso2.charAt(0)) - 'A');
        int b = 0x1F1E6 + (Character.toUpperCase(iso2.charAt(1)) - 'A');
        return new String(Character.toChars(a)) + new String(Character.toChars(b));
    }
}
