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
        return (float) (getWidth() /
