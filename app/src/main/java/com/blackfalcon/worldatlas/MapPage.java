package com.blackfalcon.worldatlas;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** The map tab: offline map with markers, a details card and my-location button. */
final class MapPage extends Page {
    private MapView map;
    private TextView coords, cardTitle, cardSub;
    private LinearLayout card;
    private Button cardBtn;
    private Object cardRef;
    private int themeIdx;
    private boolean tracking = false;
    private final Runnable gpsUpdater = new Runnable() {
        @Override
        public void run() {
            if (tracking && Gps.I.last != null && map != null) {
                map.setMe(Gps.I.last.getLatitude(), Gps.I.last.getLongitude());
            }
        }
    };

    MapPage(MainActivity a, Ui u) {
        super(a, u);
    }

    @Override
    View build() {
        FrameLayout root = new FrameLayout(act);
        map = new MapView(act, act.data);
        themeIdx = Store.mapTheme(act);
        map.setTheme(themeIdx);
        map.setLang(ui.lang());
        map.setMiles(act.miles());
        map.setListener(new MapView.Listener() {
            @Override
            public void onPick(Object ref) {
                picked(ref);
            }

            @Override
            public void onLongPress(double lat, double lon) {
                pressed(lat, lon);
            }

            @Override
            public void onMoved() {
                if (coords != null) {
                    double lat = map.centerLat();
                    double lon = map.centerLon();
                    
                    // 1. D.DD Format
                    String ddd = String.format(Locale.US, "%.5f°, %.5f°", lat, lon);
                    
                    // 2. DMS Format
                    String dms = Geo.dms(lat, true) + " " + Geo.dms(lon, false);
                    
                    // 3. DDM Format (Degrees, Decimal Minutes)
                    String ddm = formatToDDM(lat, true) + " " + formatToDDM(lon, false);
                    
                    // Teeno formats ko aik doosre ke neechay teen lines mein set karna
                    coords.setText(ddd + "\n" + dms + "\n" + ddm);
                }
            }
        });
        root.addView(map, new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));

        coords = ui.tv("", 10, Color.WHITE);
        coords.setBackground(ui.rounded(0x99000000, 8));
        coords.setPadding(ui.dp(8), ui.dp(4), ui.dp(8), ui.dp(4));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.START);
        cp.setMargins(ui.dp(6), ui.dp(6), 0, 0);
        root.addView(coords, cp);

        LinearLayout side = new LinearLayout(act);
        side.setOrientation(LinearLayout.VERTICAL);
        side.addView(round("+", v -> map.zoomBy(1.8f)));
        side.addView(round("\u2212", v -> map.zoomBy(1f / 1.8f)));
        side.addView(round("\u25CE", v -> locate()));
        side.addView(round("\uD83C\uDFA8", v -> {
            themeIdx = (themeIdx + 1) % MapView.THEMES.length;
            Store.setMapTheme(act, themeIdx);
            map.setTheme(themeIdx);
            Toast.makeText(act, ui.t("mt_" + themeIdx), Toast.LENGTH_SHORT).show();
        }));
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.TOP | Gravity.END);
        sp.setMargins(0, ui.dp(6), ui.dp(6), 0);
        root.addView(side, sp);

        card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        card.setBackground(ui.rounded(ui.dark ? 0xEE1C1C1E : 0xEEFFFFFF, 14));
        cardTitle = ui.tv("", 17, ui.tx);
        cardSub = ui.tv("", 13, ui.sub);
        card.addView(cardTitle);
        card.addView(cardSub);
        LinearLayout r = ui.row(card);
        cardBtn = ui.btn(ui.t("m_details"), Ui.BLUE, v -> openCard());
        r.addView(cardBtn, ui.weighted());
        r.addView(ui.btn(ui.t("close"), Ui.GRAY, v -> hideCard()), ui.weighted());
        card.setVisibility(View.GONE);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(Ui.MATCH, Ui.WRAP, Gravity.BOTTOM);
        bp.setMargins(ui.dp(8), 0, ui.dp(8), ui.dp(28));
        root.addView(card, bp);

        TextView note = ui.tv(ui.t("m_note"), 9, 0xCCFFFFFF);
        note.setBackground(ui.rounded(0x66000000, 6));
        note.setPadding(ui.dp(6), ui.dp(1), ui.dp(6), ui.dp(1));
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(Ui.WRAP, Ui.WRAP, Gravity.BOTTOM | Gravity.END);
        np.setMargins(0, 0, ui.dp(6), ui.dp(4));
        if (!act.data.hasBorders) root.addView(note, np);
        return root;
    }

    // DDM Format helper function
    private String formatToDDM(double coordinate, boolean isLatitude) {
        String direction = isLatitude ? (coordinate >= 0 ? "N" : "S") : (coordinate >= 0 ? "E" : "W");
        coordinate = Math.abs(coordinate);
        int degrees = (int) coordinate;
        double minutes = (coordinate - degrees) * 60;
        return String.format(Locale.US, "%d° %.4f' %s", degrees, minutes, direction);
    }

    private Button round(String text, View.OnClickListener l) {
        Button b = ui.btn(text, Ui.GRAY, l);
        b.setTextSize(18);
        b.setBackground(ui.rounded(0xCC000000, 22));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ui.dp(44), ui.dp(44));
        p.setMargins(0, 0, 0, ui.dp(6));
        b.setLayoutParams(p);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    @Override
    void onShow() {
        if (tracking) Gps.I.start(act, gpsUpdater);
    }

    @Override
    void onHide() {
        Gps.I.stop(gpsUpdater);
    }

    @Override
    void onPermission() {
        locate();
    }

    // ---------- called by MainActivity ----------

    void focusOn(double lat, double lon, float sd, String label, Object ref, String iso) {
        if (map == null) return;
        map.focus(lat, lon, sd);
        map.setSelected(lat, lon, label);
        map.setHighlight(act.data.hasBorders ? iso : null);
        if (ref != null) showCard(ref);
        else hideCard();
    }

    // ---------- card ----------

    private void showCard(Object ref) {
        cardRef = ref;
        int lang = ui.lang();
        Data d = act.data;
        cardBtn.setVisibility(View.VISIBLE);
        cardBtn.setText(ui.t("m_details"));
        if (ref instanceof Data.Country) {
            Data.Country c = (Data.Country) ref;
            cardTitle.setText(Geo.flag(c.iso2) + "  " + Data.name(c, lang));
            cardSub.setText((c.capital.equals("-") ? "" : ui.t("d_capital") + ": " + d.capitalName(c, lang) + "  \u2022  ")
                    + Fmt.popM(c.popM, lang));
        } else if (ref instanceof Data.City) {
            Data.City c = (Data.City) ref;
            Data.Country k = d.byIso.get(c.iso);
            cardTitle.setText((c.cap ? "\u2B50  " : "\uD83C\uDFD9\uFE0F  ") + Data.name(c, lang));
            cardSub.setText((k == null ? "" : Data.name(k, lang) + "  \u2022  ") + Fmt.popK(c.popK, lang));
        } else if (ref instanceof Data.Sub) {
            Data.Sub s = (Data.Sub) ref;
            Data.Country k = d.byIso.get(s.country);
            cardTitle.setText("\uD83D\uDDFA\uFE0F  " + Data.name(s, lang));
            cardSub.setText(s.type + (k == null ? "" : "  \u2022  " + Data.name(k, lang)));
        }
        card.setVisibility(View.VISIBLE);
    }

    private void hideCard() {
        cardRef = null;
        card.setVisibility(View.GONE);
        map.clearSelected();
        map.setHighlight(null);
    }

    private void openCard() {
        if (cardRef instanceof Data.Country) act.openCountry((Data.Country) cardRef);
        else if (cardRef instanceof Data.City) act.openCity((Data.City) cardRef);
        else if (cardRef instanceof Data.Sub) act.openSub((Data.Sub) cardRef);
        else if (cardRef instanceof String) act.copy((String) cardRef);
    }

    private void picked(Object ref) {
        if (ref == null) {
            hideCard();
            return;
        }
        double lat, lon;
        String label;
        int lang = ui.lang();
        String iso = null;
        if (ref instanceof Data.Country) {
            Data.Country c = (Data.Country) ref;
            lat = c.lat; lon = c.lon; label = Data.name(c, lang); iso = c.iso2;
        } else if (ref instanceof Data.City) {
            Data.City c = (Data.City) ref;
            lat = c.lat; lon = c.lon; label = Data.name(c, lang); iso = c.iso;
        } else {
            Data.Sub s = (Data.Sub) ref;
            lat = s.lat; lon = s.lon; label = Data.name(s, lang); iso = s.country;
        }
        map.setSelected(lat, lon, label);
        map.setHighlight(act.data.hasBorders ? iso : null);
        showCard(ref);
    }

    private void pressed(double lat, double lon) {
        map.setSelected(lat, lon, ui.t("m_point"));
        card.setVisibility(View.VISIBLE);
        Data.City n = act.data.nearestCity(lat, lon);
        String near = "";
        if (n != null) {
            double km = Geo.distanceKm(lat, lon, n.lat, n.lon);
            Data.Country k = act.data.byIso.get(n.iso);
            near = ui.t("m_nearest") + ": " + Data.name(n, ui.lang()) + (k == null ? "" : ", " + Data.name(k, ui.lang()))
                    + " (" + Fmt.dist(km, act.miles()) + ")";
        }
        String iso = act.data.countryAt(lon, lat);
        if (iso != null && act.data.byIso.get(iso) != null) {
            near = Geo.flag(iso) + " " + Data.name(act.data.byIso.get(iso), ui.lang()) + "\n" + near;
        }
        cardTitle.setText(String.format(Locale.US, "%.4f, %.4f", lat, lon));
        cardSub.setText(Geo.dms(lat, true) + " " + Geo.dms(lon, false) + "\n" + near);
        cardRef = String.format(Locale.US, "%.5f, %.5f", lat, lon);
        cardBtn.setText(ui.t("copy"));
    }

    private void locate() {
        if (!Gps.hasAny(act)) {
            Toast.makeText(act, ui.t("m_perm"), Toast.LENGTH_LONG).show();
            act.requestLocation();
            return;
        }
        tracking = true;
        Gps.I.start(act, gpsUpdater);
        if (Gps.I.last == null) {
            Toast.makeText(act, ui.t("m_nofix"), Toast.LENGTH_LONG).show();
            return;
        }
        double lat = Gps.I.last.getLatitude(), lon = Gps.I.last.getLongitude(); // Corrected from Gpss to Gps
        map.setMe(lat, lon);
        map.focus(lat, lon, Math.max(map.zoomDp(), 14f));
        pressedMe(lat, lon);
    }

    private void pressedMe(double lat, double lon) {
        map.clearSelected();
        card.setVisibility(View.VISIBLE);
        Data.City n = act.data.nearestCity(lat, lon);
        String near = "";
        if (n != null) {
            double km = Geo.distanceKm(lat, lon, n.lat, n.lon);
            Data.Country k = act.data.byIso.get(n.iso);
            near = ui.t("m_nearest") + ": " + Data.name(n, ui.lang()) + (k == null ? "" : ", " + Data.name(k, ui.lang()))
                    + " (" + Fmt.dist(km, act.miles()) + ")";
        }
        cardTitle.setText("\uD83D\uDCCD  " + ui.t("m_you"));
        cardSub.setText(String.format(Locale.US, "%.5f, %.5f", lat, lon) + "\n" + near);
        cardRef = String.format(Locale.US, "%.5f, %.5f", lat, lon);
        cardBtn.setText(ui.t("copy"));
    }
}
