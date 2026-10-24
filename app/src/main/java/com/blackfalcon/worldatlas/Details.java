package com.blackfalcon.worldatlas;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/** Detail page of a country. */
final class CountryPage extends Page {
    private final Data.Country c;
    private int cityLimit = 40;
    private LinearLayout citiesBox;

    CountryPage(MainActivity a, Ui u, Data.Country c) {
        super(a, u);
        this.c = c;
    }

    @Override
    String title() { return Data.name(c, ui.lang()); }

    @Override
    View build() {
        final int lang = ui.lang();
        final Data d = act.data;
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = ui.card();
        TextView flag = ui.tv(Geo.flag(c.iso2), 64, ui.tx);
        flag.setGravity(Gravity.CENTER);
        TextView name = ui.tv(Data.name(c, lang), 24, ui.tx);
        name.setGravity(Gravity.CENTER);
        TextView ty = ui.tv(ui.t(c.sov ? "ty_country" : "ty_territory"), 13, ui.sub);
        ty.setGravity(Gravity.CENTER);
        head.addView(flag, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        head.addView(name, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        head.addView(ty, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        box.addView(head, ui.gap());

        LinearLayout f = ui.card();
        if (!c.capital.equals("-")) Rows.fact(ui, f, ui.t("d_capital"), d.capitalName(c, lang));
        Rows.fact(ui, f, ui.t("d_continent"), ui.t("c_" + c.cont));
        Rows.fact(ui, f, ui.t("d_pop"), Fmt.popM(c.popM, lang));
        Rows.fact(ui, f, ui.t("d_area"), Fmt.area(c.area, act.miles()));
        Rows.fact(ui, f, ui.t("d_density"), Fmt.density(c.popM, c.area, act.miles()));
        if (!c.curCode.isEmpty()) {
            String sym = c.curSym.equals(c.curCode) || c.curSym.isEmpty() ? "" : "  " + c.curSym;
            Rows.fact(ui, f, ui.t("d_currency"), Data.currency(c, lang) + " (" + c.curCode + ")" + sym);
        }
        Rows.fact(ui, f, ui.t("d_call"), "+" + c.call);
        Rows.fact(ui, f, ui.t("d_iso"), c.iso2 + " / " + c.iso3);
        Rows.fact(ui, f, ui.t("d_coords"), String.format(Locale.US, "%.3f, %.3f", c.lat, c.lon));
        Rows.fact(ui, f, ui.t("d_names"), c.en + "\n" + c.ur + "\n" + c.ar);
        double[] me = act.myPos();
        if (me != null) {
            double km = Geo.distanceKm(me[0], me[1], c.lat, c.lon);
            double br = Geo.bearing(me[0], me[1], c.lat, c.lon);
            Rows.fact(ui, f, ui.t("d_me"), Fmt.dist(km, act.miles()) + "  " + Geo.dir16(br));
        }
        box.addView(f, ui.gap());

        LinearLayout r1 = ui.row(box);
        r1.addView(ui.btn(ui.t("d_map"), Ui.BLUE, v -> act.showCountryOnMap(c)), ui.weighted());
        r1.addView(ui.btn(ui.t("d_wiki"), Ui.GRAY, v -> act.openWiki(c.en, c.ur, c.ar)), ui.weighted());
        r1.addView(ui.btn(ui.t("d_copy"), Ui.GRAY, v -> act.copy(summary())), ui.weighted());

        // states, provinces and regions
        box.addView(Rows.header(ui, ui.t("d_subs") + " (" + c.subCount + ")"));
        List<Data.Sub> subs = d.topSubs.get(c.iso2);
        if (subs == null || subs.isEmpty()) {
            box.addView(ui.tv(ui.t("d_nosubs"), 13, ui.sub));
        } else {
            for (final Data.Sub s : subs) {
                String sub = s.type + (s.hasPos() ? "  \uD83D\uDCCD" : "");
                box.addView(Rows.item(ui, "\uD83D\uDDFA\uFE0F", Data.name(s, lang), sub, v -> act.openSub(s)), ui.gap());
            }
            box.addView(ui.tv(ui.t("d_approx"), 12, ui.sub));
        }

        // cities
        List<Data.City> cs = d.citiesBy.get(c.iso2);
        int n = cs == null ? 0 : cs.size();
        box.addView(Rows.header(ui, ui.t("d_cities") + " (" + n + ")"));
        citiesBox = new LinearLayout(act);
        citiesBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(citiesBox, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        fillCities(cs);
        return box;
    }

    private void fillCities(final List<Data.City> cs) {
        citiesBox.removeAllViews();
        if (cs == null) return;
        int lang = ui.lang();
        int shown = Math.min(cs.size(), cityLimit);
        for (int i = 0; i < shown; i++) {
            final Data.City x = cs.get(i);
            String sub = (x.cap ? ui.t("ty_capital") + "  " : "") + Fmt.popK(x.popK, lang);
            citiesBox.addView(Rows.item(ui, x.cap ? "\u2B50" : "\uD83C\uDFD9\uFE0F", Data.name(x, lang), sub, v -> act.openCity(x)), ui.gap());
        }
        if (shown < cs.size()) {
            citiesBox.addView(ui.btn(ui.t("d_more") + " (" + (cs.size() - shown) + ")", Ui.GRAY, v -> {
                cityLimit = 100000;
                fillCities(cs);
            }), new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        }
    }

    private String summary() {
        int lang = ui.lang();
        StringBuilder sb = new StringBuilder();
        sb.append(Geo.flag(c.iso2)).append(' ').append(Data.name(c, lang)).append('\n');
        if (!c.capital.equals("-")) sb.append(ui.t("d_capital")).append(": ").append(act.data.capitalName(c, lang)).append('\n');
        sb.append(ui.t("d_pop")).append(": ").append(Fmt.popM(c.popM, lang)).append('\n');
        sb.append(ui.t("d_area")).append(": ").append(Fmt.area(c.area, act.miles())).append('\n');
        if (!c.curCode.isEmpty()) sb.append(ui.t("d_currency")).append(": ").append(c.curEn).append(" (").append(c.curCode).append(")\n");
        sb.append(ui.t("d_call")).append(": +").append(c.call);
        return sb.toString();
    }
}

/** Detail page of a city. */
final class CityPage extends Page {
    private final Data.City c;

    CityPage(MainActivity a, Ui u, Data.City c) {
        super(a, u);
        this.c = c;
    }

    @Override
    String title() { return Data.name(c, ui.lang()); }

    @Override
    View build() {
        final int lang = ui.lang();
        final Data d = act.data;
        final Data.Country country = d.byIso.get(c.iso);
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = ui.card();
        TextView ic = ui.tv(c.cap ? "\u2B50" : "\uD83C\uDFD9\uFE0F", 52, ui.tx);
        ic.setGravity(Gravity.CENTER);
        TextView name = ui.tv(Data.name(c, lang), 24, ui.tx);
        name.setGravity(Gravity.CENTER);
        TextView ty = ui.tv(ui.t(c.cap ? "ty_capital" : "ty_city"), 13, ui.sub);
        ty.setGravity(Gravity.CENTER);
        head.addView(ic, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        head.addView(name, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        head.addView(ty, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        box.addView(head, ui.gap());

        LinearLayout f = ui.card();
        if (country != null) Rows.fact(ui, f, ui.t("d_country"), Geo.flag(country.iso2) + " " + Data.name(country, lang));
        Rows.fact(ui, f, ui.t("d_pop"), Fmt.popK(c.popK, lang));
        Rows.fact(ui, f, ui.t("d_coords"), String.format(Locale.US, "%.4f, %.4f", c.lat, c.lon)
                + "\n" + Geo.dms(c.lat, true) + " " + Geo.dms(c.lon, false));
        Rows.fact(ui, f, ui.t("d_names"), c.name + (c.ur.isEmpty() ? "" : "\n" + c.ur) + (c.ar.isEmpty() ? "" : "\n" + c.ar));
        double[] me = act.myPos();
        if (me != null) {
            double km = Geo.distanceKm(me[0], me[1], c.lat, c.lon);
            double br = Geo.bearing(me[0], me[1], c.lat, c.lon);
            Rows.fact(ui, f, ui.t("d_me"), Fmt.dist(km, act.miles()) + "  " + Geo.dir16(br));
        }
        box.addView(f, ui.gap());

        LinearLayout r1 = ui.row(box);
        r1.addView(ui.btn(ui.t("d_map"), Ui.BLUE, v -> act.showCityOnMap(c)), ui.weighted());
        r1.addView(ui.btn(ui.t("d_wiki"), Ui.GRAY, v -> act.openWiki(c.name, c.ur, c.ar)), ui.weighted());
        r1.addView(ui.btn(ui.t("d_copy"), Ui.GRAY, v -> act.copy(Data.name(c, lang) + "\n"
                + String.format(Locale.US, "%.5f, %.5f", c.lat, c.lon))), ui.weighted());
        if (country != null) {
            box.addView(ui.btn(Geo.flag(country.iso2) + " " + Data.name(country, lang), Ui.TEAL, v -> act.openCountry(country)),
                    new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        }

        box.addView(Rows.header(ui, ui.t("d_nearby")));
        for (final Data.City n : d.nearestCities(c.lat, c.lon, 6, c)) {
            double km = Geo.distanceKm(c.lat, c.lon, n.lat, n.lon);
            Data.Country nc = d.byIso.get(n.iso);
            String sub = (nc == null ? "" : Data.name(nc, lang) + "  \u2022  ") + Fmt.dist(km, act.miles())
                    + "  " + Geo.dir16(Geo.bearing(c.lat, c.lon, n.lat, n.lon));
            box.addView(Rows.item(ui, n.cap ? "\u2B50" : "\uD83C\uDFD9\uFE0F", Data.name(n, lang), sub, v -> act.openCity(n)), ui.gap());
        }
        return box;
    }
}

/** Detail page of a state, province or other subdivision. */
final class SubPage extends Page {
    private final Data.Sub s;

    SubPage(MainActivity a, Ui u, Data.Sub s) {
        super(a, u);
        this.s = s;
    }

    @Override
    String title() { return Data.name(s, ui.lang()); }

    @Override
    View build() {
        final int lang = ui.lang();
        final Data d = act.data;
        final Data.Country country = d.byIso.get(s.country);
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);

        LinearLayout head = ui.card();
        TextView ic = ui.tv("\uD83D\uDDFA\uFE0F", 52, ui.tx);
        ic.setGravity(Gravity.CENTER);
        TextView name = ui.tv(Data.name(s, lang), 24, ui.tx);
        name.setGravity(Gravity.CENTER);
        TextView ty = ui.tv(s.type, 13, ui.sub);
        ty.setGravity(Gravity.CENTER);
        head.addView(ic, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        head.addView(name, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        head.addView(ty, new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        box.addView(head, ui.gap());

        LinearLayout f = ui.card();
        Rows.fact(ui, f, ui.t("d_type"), s.type);
        Rows.fact(ui, f, ui.t("d_code"), s.code);
        if (country != null) Rows.fact(ui, f, ui.t("d_country"), Geo.flag(country.iso2) + " " + Data.name(country, lang));
        Data.Sub parent = s.parent.isEmpty() ? null : d.subByCode.get(s.parent);
        if (parent != null) Rows.fact(ui, f, ui.t("d_parent"), Data.name(parent, lang));
        if (s.hasPos()) {
            Rows.fact(ui, f, ui.t("d_coords"), String.format(Locale.US, "%.3f, %.3f", s.lat, s.lon));
            double[] me = act.myPos();
            if (me != null) {
                double km = Geo.distanceKm(me[0], me[1], s.lat, s.lon);
                Rows.fact(ui, f, ui.t("d_me"), Fmt.dist(km, act.miles()) + "  " + Geo.dir16(Geo.bearing(me[0], me[1], s.lat, s.lon)));
            }
        }
        if (!s.ur.isEmpty() || !s.ar.isEmpty()) {
            Rows.fact(ui, f, ui.t("d_names"), s.name + (s.ur.isEmpty() ? "" : "\n" + s.ur) + (s.ar.isEmpty() ? "" : "\n" + s.ar));
        }
        box.addView(f, ui.gap());
        TextView note = ui.tv(ui.t(s.hasPos() ? "d_approx" : "d_nopos"), 12, ui.sub);
        note.setPadding(ui.dp(4), 0, ui.dp(4), ui.dp(8));
        box.addView(note);

        LinearLayout r1 = ui.row(box);
        r1.addView(ui.btn(ui.t("d_map"), Ui.BLUE, v -> act.showSubOnMap(s)), ui.weighted());
        r1.addView(ui.btn(ui.t("d_wiki"), Ui.GRAY, v -> act.openWikiSearch(s.name + ", " + (country == null ? "" : country.en))), ui.weighted());
        if (country != null) {
            box.addView(ui.btn(Geo.flag(country.iso2) + " " + Data.name(country, lang), Ui.TEAL, v -> act.openCountry(country)),
                    new LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP));
        }

        if (!s.kids.isEmpty()) {
            box.addView(Rows.header(ui, ui.t("d_kids") + " (" + s.kids.size() + ")"));
            for (final Data.Sub k : s.kids) {
                box.addView(Rows.item(ui, "\uD83D\uDDFA\uFE0F", Data.name(k, lang), k.type + (k.hasPos() ? "  \uD83D\uDCCD" : ""), v -> act.openSub(k)), ui.gap());
            }
        }
        return box;
    }
}
