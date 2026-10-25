package com.blackfalcon.worldatlas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Offline world map: pan, pinch zoom, themes, markers with level of detail. Equirectangular projection. */
final class MapView extends View {

    interface Listener {
        void onPick(Object ref);
        void onLongPress(double lat, double lon);
        void onMoved();
    }

    // sea, land, border, label, highlight, city dot, country dot, grid, sub dot
    static final int[][] THEMES = {
            {0xFF0B1F33, 0xFF26422F, 0xFF5E9A70, 0xFFE8F0F2, 0xFF3F7F55, 0xFFFFC857, 0xFFFF8A65, 0x33FFFFFF, 0xFF80DEEA},
            {0xFFBFD9F2, 0xFFF3EEDC, 0xFF9C8F70, 0xFF1F2933, 0xFFFFE08A, 0xFFC62828, 0xFF1565C0, 0x33000000, 0xFF00796B},
            {0xFF2F6F9F, 0xFF6AA84F, 0xFF2E5E2E, 0xFFFFFFFF, 0xFFC5E1A5, 0xFFFFEB3B, 0xFFFF7043, 0x33FFFFFF, 0xFFE0F7FA},
            {0xFFEFE6CF, 0xFFD9C9A3, 0xFF7B6A45, 0xFF3B2F1A, 0xFFF0D98A, 0xFF8E2A1B, 0xFF1F5F8B, 0x33000000, 0xFF2E6B3A},
    };

    private final Data data;
    private final float density;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path[] paths;
    private final Path[] statePaths;
    private final Path[] riverPaths;
    private final Data.City[] citiesByPop;
    private final ScaleGestureDetector scaler;
    private final GestureDetector gestures;

    private double cLon = 65, cLat = 30;
    private float scale = 0f;
    private int theme = 0;
    private int lang = 0;
    private boolean miles = false;
    private String highlightIso = null;
    private Listener listener;

    private double pendLat, pendLon;
    private float pendSd = -1f;

    private boolean hasSel = false;
    private double selLat, selLon;
    private String selLabel = "";
    private boolean hasMe = false;
    private double meLat, meLon;

    // markers drawn in the last frame (for tapping)
    private final float[] px = new float[3000];
    private final float[] py = new float[3000];
    private final Object[] refs = new Object[3000];
    private int nPick = 0;
    private final List<RectF> placed = new ArrayList<RectF>();
    private final RectF tmp = new RectF();

    MapView(Context c, Data d) {
        super(c);
        data = d;
        density = c.getResources().getDisplayMetrics().density;
        setLayerType(LAYER_TYPE_SOFTWARE, null); // very large zoomed paths are drawn reliably in software
        setFocusable(true);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        fill.setStyle(Paint.Style.FILL);
        text.setTextAlign(Paint.Align.CENTER);
        halo.setTextAlign(Paint.Align.CENTER);
        halo.setStyle(Paint.Style.STROKE);
        halo.setStrokeWidth(3f * density);
        dot.setStyle(Paint.Style.FILL);

        paths = new Path[d.polys.size()];
        for (int i = 0; i < paths.length; i++) {
            float[] xy = d.polys.get(i).xy;
            Path p = new Path();
            if (xy.length >= 4) {
                p.moveTo(xy[0], -xy[1]);
                for (int k = 2; k + 1 < xy.length; k += 2) p.lineTo(xy[k], -xy[k + 1]);
                p.close();
            }
            paths[i] = p;
        }

        // Initialize State Paths
        statePaths = new Path[d.statePolys.size()];
        for (int i = 0; i < statePaths.length; i++) {
            float[] xy = d.statePolys.get(i).xy;
            Path p = new Path();
            if (xy.length >= 4) {
                p.moveTo(xy[0], -xy[1]);
                for (int k = 2; k + 1 < xy.length; k += 2) p.lineTo(xy[k], -xy[k + 1]);
                p.close();
            }
            statePaths[i] = p;
        }

        // Initialize River Paths
        riverPaths = new Path[d.rivers.size()];
        for (int i = 0; i < riverPaths.length; i++) {
            float[] xy = d.rivers.get(i).xy;
            Path p = new Path();
            if (xy.length >= 4) {
                p.moveTo(xy[0], -xy[1]);
                for (int k = 2; k + 1 < xy.length; k += 2) p.lineTo(xy[k], -xy[k + 1]);
            }
            riverPaths[i] = p;
        }

        citiesByPop = d.cities.toArray(new Data.City[0]);
        java.util.Arrays.sort(citiesByPop, new Comparator<Data.City>() {
            @Override
            public int compare(Data.City a, Data.City b) {
                if (a.cap != b.cap) return a.cap ? -1 : 1;
                return b.popK - a.popK;
            }
        });

        scaler = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector det) {
                zoomAround(det.getScaleFactor(), det.getFocusX(), det.getFocusY());
                return true;
            }
        });
        gestures = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
                if (scale <= 0) return true;
                cLon += dx / scale;
                cLat -= dy / scale;
                clampCenter();
                moved();
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                pick(e.getX(), e.getY());
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                zoomAround(2f, e.getX(), e.getY());
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (listener != null && scale > 0) {
                    double lon = cLon + (e.getX() - getWidth() / 2.0) / scale;
                    double lat = cLat - (e.getY() - getHeight() / 2.0) / scale;
                    listener.onLongPress(lat, Geo.wrap180(lon));
                }
            }
        });
    }

    // ---------- public API ----------

    void setListener(Listener l) { listener = l; }

    void setTheme(int t) {
        theme = Math.max(0, Math.min(THEMES.length - 1, t));
        invalidate();
    }

    void setLang(int l) { lang = l; }

    void setMiles(boolean m) { miles = m; }

    void setHighlight(String iso) {
        highlightIso = iso;
        invalidate();
    }

    void setSelected(double lat, double lon, String label) {
        hasSel = true;
        selLat = lat;
        selLon = lon;
        selLabel = label == null ? "" : label;
        invalidate();
    }

    void clearSelected() {
        hasSel = false;
        invalidate();
    }

    void setMe(double lat, double lon) {
        hasMe = true;
        meLat = lat;
        meLon = lon;
        invalidate();
    }

    double centerLat() { return cLat; }

    double centerLon() { return cLon; }

    /** Zoom measured in dp per degree. */
    float zoomDp() { return density == 0 ? 0 : scale / density; }

    void focus(double lat, double lon, float dpPerDeg) {
        if (getWidth() == 0) {
            pendLat = lat;
            pendLon = lon;
            pendSd = dpPerDeg;
            return;
        }
        cLat = lat;
        cLon = lon;
        scale = clampScale(dpPerDeg * density);
        clampCenter();
        moved();
    }

    void zoomBy(float f) {
        zoomAround(f, getWidth() / 2f, getHeight() / 2f);
    }

    // ---------- internals ----------

    private float minScale() { return Math.max(1f, getWidth() / 400f); }

    private float clampScale(float s) { return Math.max(minScale(), Math.min(300f * density, s)); }

    private void clampCenter() {
        if (cLat > 85) cLat = 85;
        if (cLat < -85) cLat = -85;
        cLon = Geo.wrap180(cLon);
    }

    private void zoomAround(float f, float fx, float fy) {
        if (scale <= 0) return;
        double w = getWidth(), h = getHeight();
        double lonAt = cLon + (fx - w / 2.0) / scale;
        double latAt = cLat - (fy - h / 2.0) / scale;
        scale = clampScale(scale * f);
        cLon = lonAt - (fx - w / 2.0) / scale;
        cLat = latAt + (fy - h / 2.0) / scale;
        clampCenter();
        moved();
    }

    private void moved() {
        invalidate();
        if (listener != null) listener.onMoved();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (scale <= 0 && w > 0) {
            scale = clampScale(w / 90f); // about 90 degrees across at start
        } else if (w > 0) {
            scale = clampScale(scale);
        }
        if (pendSd > 0 && w > 0) {
            float sd = pendSd;
            pendSd = -1f;
            focus(pendLat, pendLon, sd);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN && getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        scaler.onTouchEvent(e);
        if (!scaler.isInProgress()) gestures.onTouchEvent(e);
        return true;
    }

    private float sx(double lon) {
        return (float) (getWidth() / 2.0 + Geo.wrap180(lon - cLon) * scale);
    }

    private float sy(double lat) {
        return (float) (getHeight() / 2.0 - (lat - cLat) * scale);
    }

    private void pick(float x, float y) {
        if (listener == null) return;
        float best = 26f * density;
        int bi = -1;
        for (int i = 0; i < nPick; i++) {
            float d = (float) Math.hypot(px[i] - x, py[i] - y);
            if (d < best) {
                best = d;
                bi = i;
            }
        }
        if (bi >= 0) {
            listener.onPick(refs[bi]);
            return;
        }
        if (data.hasBorders && scale > 0) {
            double lon = Geo.wrap180(cLon + (x - getWidth() / 2.0) / scale);
            double lat = cLat - (y - getHeight() / 2.0) / scale;
            String iso = data.countryAt(lon, lat);
            if (iso != null) {
                Data.Country c = data.byIso.get(iso);
                if (c != null) {
                    listener.onPick(c);
                    return;
                }
            }
        }
        listener.onPick(null);
    }

    private boolean free(float cx, float cy, float w, float h) {
        tmp.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
        for (int i = 0; i < placed.size(); i++) {
            if (RectF.intersects(placed.get(i), tmp)) return false;
        }
        placed.add(new RectF(tmp));
        return true;
    }

    private void label(Canvas cv, String s, float x, float y, float sizeDp, int color, int haloColor, boolean bold) {
        text.setTextSize(sizeDp * density);
        text.setFakeBoldText(bold);
        float w = text.measureText(s);
        float h = sizeDp * density;
        if (!free(x, y, w + 4 * density, h)) return;
        halo.setTextSize(sizeDp * density);
        halo.setFakeBoldText(bold);
        halo.setColor(haloColor);
        text.setColor(color);
        cv.drawText(s, x, y + h * 0.35f, halo);
        cv.drawText(s, x, y + h * 0.35f, text);
    }

    @Override
    protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || scale <= 0) return;
        final int[] th = THEMES[theme];
        float s = scale;
        float sd = s / density;
        cv.drawColor(th[0]);

        // land and water shapes, repeated around the world
        double left = cLon - w / (2.0 * s), right = cLon + w / (2.0 * s);
        stroke.setColor(th[2]);
        stroke.setStrokeWidth(Math.max(1f, density * 0.8f) / s);
        for (int k = -2; k <= 2; k++) {
            double off = 360.0 * k;
            if (off - 180 > right || off + 230 < left) continue;
            cv.save();
            cv.translate((float) (w / 2.0 + (off - cLon) * s), (float) (h / 2.0 + cLat * s));
            cv.scale(s, s);
            for (int i = 0; i < paths.length; i++) {
                Data.Poly p = data.polys.get(i);
                if (p.water) continue;
                boolean hl = highlightIso != null && highlightIso.equals(p.iso);
                fill.setColor(hl ? th[4] : th[1]);
                cv.drawPath(paths[i], fill);
                cv.drawPath(paths[i], stroke);
            }
            for (int i = 0; i < paths.length; i++) {
                Data.Poly p = data.polys.get(i);
                if (!p.water) continue;
                fill.setColor(th[0]);
                cv.drawPath(paths[i], fill);
                cv.drawPath(paths[i], stroke);
            }

            // Draw States / Provinces borders when zoomed in enough
            if (data.hasStates && sd >= 8f) {
                stroke.setColor(th[2] & 0x7FFFFFFF); // slightly lighter/transparent border for states
                stroke.setStrokeWidth(Math.max(0.6f, density * 0.5f) / s);
                for (int i = 0; i < statePaths.length; i++) {
                    cv.drawPath(statePaths[i], stroke);
                }
            }

            // Draw Rivers when zoomed in
            if (data.hasRivers && sd >= 6f) {
                stroke.setColor(th[8]); // river/water feature color
                stroke.setStrokeWidth(Math.max(0.8f, density * 0.6f) / s);
                for (int i = 0; i < riverPaths.length; i++) {
                    cv.drawPath(riverPaths[i], stroke);
                }
            }

            cv.restore();
        }

        drawGrid(cv, th, w, h, s);

        // markers
        nPick = 0;
        placed.clear();
        int labels = 0;
        int halo0 = th[0] & 0x00FFFFFF | 0xCC000000;
        boolean bigZoom = sd >= 8f;
        // countries (until capitals take over)
        if (sd >= 0.9f) {
            for (Data.Country c : data.countries) {
                float x = sx(c.lon), y = sy(c.lat);
                if (x < -40 || x > w + 40 || y < -40 || y > h + 40) continue;
                if (bigZoom && !c.capital.equals("-")) continue;
                dot.setColor(th[6]);
                cv.drawCircle(x, y, (c.sov ? 2.6f : 1.8f) * density, dot);
                if (nPick < px.length) {
                    px[nPick] = x; py[nPick] = y; refs[nPick] = c; nPick++;
                }
                if (labels < 140 && ((c.sov && sd >= 2.3f) || sd >= 6f)) {
                    label(cv, Data.name(c, lang), x, y - 9 * density, c.sov ? 11f : 9.5f, th[3], halo0, c.sov);
                    labels++;
                }
            }
        }
        // cities
        for (Data.City c : citiesByPop) {
            float need;
            if (c.cap) need = 8f;
            else if (c.popK >= 10000) need = 2.2f;
            else if (c.popK >= 3000) need = 3.5f;
            else if (c.popK >= 1000) need = 6f;
            else if (c.popK >= 400) need = 10f;
            else if (c.popK >= 150) need = 18f;
            else need = 34f;
            if (sd < need) continue;
            float x = sx(c.lon), y = sy(c.lat);
            if (x < -40 || x > w + 40 || y < -40 || y > h + 40) continue;
            dot.setColor(th[5]);
            if (c.cap) {
                cv.drawRect(x - 3.2f * density, y - 3.2f * density, x + 3.2f * density, y + 3.2f * density, dot);
            } else {
                cv.drawCircle(x, y, 2.3f * density, dot);
            }
            if (nPick < px.length) {
                px[nPick] = x; py[nPick] = y; refs[nPick] = c; nPick++;
            }
            if (labels < 220 && sd >= need + 0.2f) {
                label(cv, Data.name(c, lang), x, y + 10 * density, c.cap ? 11f : 10f, th[3], halo0, c.cap);
                labels++;
            }
        }
        // subdivisions
        if (sd >= 14f) {
            for (Data.Sub b : data.subs) {
                if (!b.hasPos()) continue;
                float x = sx(b.lon), y = sy(b.lat);
                if (x < -40 || x > w + 40 || y < -40 || y > h + 40) continue;
                dot.setColor(th[8]);
                cv.drawRect(x - 2.2f * density, y - 2.2f * density, x + 2.2f * density, y + 2.2f * density, dot);
                if (nPick < px.length) {
                    px[nPick] = x; py[nPick] = y; refs[nPick] = b; nPick++;
                }
                if (labels < 260 && sd >= 22f) {
                    label(cv, Data.name(b, lang), x, y - 9 * density, 9.5f, th[8], halo0, false);
                    labels++;
                }
            }
        }
        // my position
        if (hasMe) {
            float x = sx(meLon), y = sy(meLat);
            dot.setColor(0x553D8BFF);
            cv.drawCircle(x, y, 14 * density, dot);
            dot.setColor(0xFF2979FF);
            cv.drawCircle(x, y, 6 * density, dot);
            stroke.setStrokeWidth(2 * density);
            stroke.setColor(0xFFFFFFFF);
            cv.drawCircle(x, y, 6 * density, stroke);
        }
        // selected pin
        if (hasSel) {
            float x = sx(selLon), y = sy(selLat);
            dot.setColor(0xFFE53935);
            cv.drawCircle(x, y, 9 * density, dot);
            dot.setColor(0xFFFFFFFF);
            cv.drawCircle(x, y, 3.5f * density, dot);
            stroke.setStrokeWidth(2 * density);
            stroke.setColor(0xFFFFFFFF);
            cv.drawCircle(x, y, 9 * density, stroke);
            if (!selLabel.isEmpty()) {
                text.setTextSize(12 * density);
                text.setFakeBoldText(true);
                float tw = text.measureText(selLabel);
                float bx = x, by = y - 24 * density;
                fill.setColor(0xE6000000);
                cv.drawRoundRect(bx - tw / 2f - 8 * density, by - 11 * density, bx + tw / 2f + 8 * density, by + 9 * density,
                        8 * density, 8 * density, fill);
                text.setColor(0xFFFFFFFF);
                cv.drawText(selLabel, bx, by + 4 * density, text);
            }
        }
        drawScale(cv, th, h);
    }

    private void drawGrid(Canvas cv, int[] th, int w, int h, float s) {
        double[] steps = {0.25, 0.5, 1, 2, 5, 10, 15, 30, 45};
        double step = 45;
        for (double st : steps) {
            if (st * s >= 70 * density) {
                step = st;
                break;
            }
        }
        stroke.setColor(th[7]);
        stroke.setStrokeWidth(1f);
        text.setTextSize(9 * density);
        text.setFakeBoldText(false);
        text.setColor(th[3] & 0x00FFFFFF | 0x99000000);
        double lonL = cLon - w / (2.0 * s), lonR = cLon + w / (2.0 * s);
        for (double lon = Math.floor(lonL / step) * step; lon <= lonR; lon += step) {
            float x = (float) (w / 2.0 + (lon - cLon) * s);
            cv.drawLine(x, 0, x, h, stroke);
            double shown = Geo.wrap180(lon);
            cv.drawText(degText(Math.abs(shown), step) + (shown < 0 ? "W" : (shown > 0 ? "E" : "")), x, 11 * density, text);
        }
        double latT = cLat + h / (2.0 * s), latB = cLat - h / (2.0 * s);
        for (double lat = Math.floor(latB / step) * step; lat <= latT; lat += step) {
            if (lat < -90 || lat > 90) continue;
            float y = (float) (h / 2.0 - (lat - cLat) * s);
            cv.drawLine(0, y, w, y, stroke);
            cv.drawText(degText(Math.abs(lat), step) + (lat < 0 ? "S" : (lat > 0 ? "N" : "")), 22 * density, y - 2 * density, text);
        }
    }

    private static String degText(double v, double step) {
        if (step >= 1) return String.format(Locale.US, "%d\u00B0", Math.round(v));
        if (step >= 0.5) return String.format(Locale.US, "%.1f\u00B0", v);
        return String.format(Locale.US, "%.2f\u00B0", v);
    }

    private void drawScale(Canvas cv, int[] th, int h) {
        double kmPerPx = 111.32 * Math.cos(Math.toRadians(cLat)) / scale;
        if (kmPerPx <= 0) return;
        double[] nice = {0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000};
        double unit = miles ? 1.609344 : 1.0;
        double pick = nice[0];
        for (double v : nice) {
            if (v * unit / kmPerPx <= 120 * density) pick = v;
        }
        float len = (float) (pick * unit / kmPerPx);
        float x0 = 12 * density, y0 = h - 14 * density;
        stroke.setColor(th[3]);
        stroke.setStrokeWidth(2 * density);
        cv.drawLine(x0, y0, x0 + len, y0, stroke);
        cv.drawLine(x0, y0 - 5 * density, x0, y0 + 5 * density, stroke);
        cv.drawLine(x0 + len, y0 - 5 * density, x0 + len, y0 + 5 * density, stroke);
        text.setTextSize(10 * density);
        text.setFakeBoldText(true);
        text.setColor(th[3]);
        text.setTextAlign(Paint.Align.LEFT);
        String t = (pick < 1 ? String.format(Locale.US, "%.1f", pick) : String.format(Locale.US, "%.0f", pick)) + (miles ? " mi" : " km");
        cv.drawText(t, x0, y0 - 8 * density, text);
        text.setTextAlign(Paint.Align.CENTER);
    }
}
