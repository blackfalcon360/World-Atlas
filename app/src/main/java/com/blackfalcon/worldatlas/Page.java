package com.blackfalcon.worldatlas;

import android.view.View;

/** One screen: a tab or a detail page. */
abstract class Page {
    final MainActivity act;
    final Ui ui;

    Page(MainActivity a, Ui u) {
        act = a;
        ui = u;
    }

    /** Heading for detail pages. */
    String title() { return ""; }

    abstract View build();

    void onShow() { }

    void onHide() { }

    void onPermission() { }
}
