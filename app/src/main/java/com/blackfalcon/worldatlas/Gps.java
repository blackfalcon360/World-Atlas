package com.blackfalcon.worldatlas;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.ArrayList;

/** Optional position of the phone. Used only on the device. */
final class Gps implements LocationListener {
    static final Gps I = new Gps();

    Location last;
    boolean running;
    private LocationManager lm;
    private final ArrayList<Runnable> listeners = new ArrayList<Runnable>();

    static boolean has(Context c) {
        return c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    static boolean hasAny(Context c) {
        return has(c) || c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    void start(Context c, Runnable l) {
        if (!listeners.contains(l)) listeners.add(l);
        if (!hasAny(c)) return;
        try {
            lm = (LocationManager) c.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            if (last == null) {
                String[] provs = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER};
                for (String pr : provs) {
                    try {
                        Location k = lm.getLastKnownLocation(pr);
                        if (k != null && (last == null || k.getTime() > last.getTime())) last = k;
                    } catch (Exception e) {
                        // provider not available
                    }
                }
            }
            if (!running && has(c)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, this);
                running = true;
            }
        } catch (Exception e) {
            running = false;
        }
    }

    void stop(Runnable l) {
        listeners.remove(l);
        if (listeners.isEmpty() && running && lm != null) {
            try {
                lm.removeUpdates(this);
            } catch (Exception e) {
                // ignore
            }
            running = false;
        }
    }

    @Override
    public void onLocationChanged(Location l) {
        last = l;
        for (Runnable r : new ArrayList<Runnable>(listeners)) r.run();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }
}
