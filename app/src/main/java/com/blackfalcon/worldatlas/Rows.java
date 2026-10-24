package com.blackfalcon.worldatlas;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Row builders shared by the detail pages. */
final class Rows {
    private Rows() {}

    /** Label/value line inside a card. */
    static void fact(Ui ui, LinearLayout box, String label, String value) {
        LinearLayout r = ui.row(box);
        r.setPadding(0, ui.dp(4), 0, ui.dp(4));
        TextView l = ui.tv(label, 13, ui.sub);
        TextView v = ui.tv(value, 14, ui.tx);
        v.setTextIsSelectable(true);
        r.addView(l, new LinearLayout.LayoutParams(0, Ui.WRAP, 0.4f));
        r.addView(v, new LinearLayout.LayoutParams(0, Ui.WRAP, 0.6f));
    }

    /** Tappable list item with an icon, a title and a subtitle. */
    static LinearLayout item(Ui ui, String icon, String title, String subtitle, View.OnClickListener click) {
        LinearLayout c = ui.card();
        LinearLayout r = ui.row(c);
        TextView ic = ui.tv(icon, 24, ui.tx);
        ic.setGravity(Gravity.CENTER);
        ic.setPadding(0, 0, ui.dp(10), 0);
        r.addView(ic, new LinearLayout.LayoutParams(ui.dp(46), Ui.WRAP));
        LinearLayout col = new LinearLayout(ui.c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(ui.tv(title, 15, ui.tx));
        if (subtitle != null && !subtitle.isEmpty()) col.addView(ui.tv(subtitle, 12, ui.sub));
        r.addView(col, new LinearLayout.LayoutParams(0, Ui.WRAP, 1f));
        if (click != null) c.setOnClickListener(click);
        return c;
    }

    static TextView header(Ui ui, String text) {
        TextView t = ui.tv(text, 16, ui.tx);
        t.setPadding(ui.dp(4), ui.dp(10), ui.dp(4), ui.dp(6));
        t.setFakeBoldText(true);
        return t;
    }
}
