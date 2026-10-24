package com.blackfalcon.worldatlas;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Language, themes, units, about. */
final class SettingsPage extends Page {
    SettingsPage(MainActivity a, Ui u) {
        super(a, u);
    }

    private RadioButton radio(RadioGroup g, int id, String text, boolean checked) {
        RadioButton rb = new RadioButton(act);
        rb.setId(id);
        rb.setText(text);
        ui.tint(rb);
        g.addView(rb);
        if (checked) rb.setChecked(true);
        return rb;
    }

    @Override
    View build() {
        ScrollView sv = new ScrollView(act);
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        sv.addView(box, new ViewGroup.LayoutParams(Ui.MATCH, Ui.WRAP));

        LinearLayout c1 = ui.card();
        c1.addView(ui.tv(ui.t("st_lang"), 16, ui.tx));
        final RadioGroup rgL = new RadioGroup(act);
        String cur = Store.lang(act);
        radio(rgL, 101, "English", "en".equals(cur));
        radio(rgL, 102, "\u0627\u0631\u062F\u0648", "ur".equals(cur));
        radio(rgL, 103, "\u0627\u0644\u0639\u0631\u0628\u064A\u0629", "ar".equals(cur));
        c1.addView(rgL);
        box.addView(c1, ui.gap());

        LinearLayout c2 = ui.card();
        c2.addView(ui.tv(ui.t("st_theme"), 16, ui.tx));
        final RadioGroup rgT = new RadioGroup(act);
        radio(rgT, 111, ui.t("st_dark"), Store.dark(act));
        radio(rgT, 112, ui.t("st_light"), !Store.dark(act));
        c2.addView(rgT);
        box.addView(c2, ui.gap());

        LinearLayout c3 = ui.card();
        c3.addView(ui.tv(ui.t("st_map"), 16, ui.tx));
        final RadioGroup rgM = new RadioGroup(act);
        for (int i = 0; i < 4; i++) radio(rgM, 121 + i, ui.t("mt_" + i), Store.mapTheme(act) == i);
        c3.addView(rgM);
        box.addView(c3, ui.gap());

        LinearLayout c4 = ui.card();
        c4.addView(ui.tv(ui.t("st_units"), 16, ui.tx));
        final RadioGroup rgU = new RadioGroup(act);
        radio(rgU, 131, ui.t("st_km"), !Store.miles(act));
        radio(rgU, 132, ui.t("st_mi"), Store.miles(act));
        c4.addView(rgU);
        box.addView(c4, ui.gap());

        box.addView(ui.btn(ui.t("st_save"), Ui.ORANGE, v -> {
            int id = rgL.getCheckedRadioButtonId();
            Store.setLang(act, id == 102 ? "ur" : (id == 103 ? "ar" : "en"));
            Store.setDark(act, rgT.getCheckedRadioButtonId() != 112);
            int m = rgM.getCheckedRadioButtonId() - 121;
            Store.setMapTheme(act, m < 0 ? 0 : m);
            Store.setMiles(act, rgU.getCheckedRadioButtonId() == 132);
            act.rebuildAll();
            Toast.makeText(act, ui.t("st_saved"), Toast.LENGTH_SHORT).show();
        }), ui.gap());

        LinearLayout c5 = ui.card();
        c5.addView(ui.tv(ui.t("st_about"), 16, ui.tx));
        TextView body = ui.tv(ui.t("st_about_text"), 13, ui.sub);
        body.setPadding(0, ui.dp(6), 0, ui.dp(6));
        c5.addView(body);
        Data d = act.data;
        c5.addView(ui.tv(d.countries.size() + " / " + d.subs.size() + " / " + d.cities.size(), 12, ui.sub));
        box.addView(c5, ui.gap());
        return sv;
    }
}
