package de.hirola.runningplan.tracking;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.hirola.sportslibrary.Global;
import de.hirola.sportslibrary.model.Track;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * A background service to get location updates for recording trainings.
 * The tracks are saved in a local datastore.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 */
public class LocationTrackingService extends Service implements LocationListener {

    private final IBinder serviceBinder = new LocationTrackingServiceBinder();
    private final LocationTrackingDatabaseHelper databaseHelper = new LocationTrackingDatabaseHelper(null);
    private Track.Id trackId = null; // actual recording track
    private LocationManager locationManager; // location manager
    private boolean isRecording; // flag the state of recording track
    private boolean gpsAvailable; // (gps) tracking available?
    private float runningDistance; // distance

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRecording = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // stop location updates
        locationManager.removeUpdates(this);
        locationManager = null;
        // remove the recorded track
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
        if (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
            System.out.println("gps enabled");
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
        if (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
            System.out.println("gps disabled");
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        // insert location to local tracking database
        databaseHelper.insertLocation(location);
    }

    @Override
    // want to change in future releases
    // https://stackoverflow.com/questions/64638260/android-locationlistener-abstractmethoderror-on-onstatuschanged-and-onproviderd
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    public class LocationTrackingServiceBinder extends Binder {

        public LocationTrackingServiceBinder() {
            super();
        }

        public LocationTrackingService getService() {
            return LocationTrackingService.this;
        }

    }

    public Track.Id startTrackRecording() {
        if (isRecording) {
            if (Global.DEBUG) {
                //TODO: Logging
            }
            return trackId;
        }
        // create a new track and start recording
        // we needed name, description, start time
        Track track = new Track("Running unit");
        long primaryKey = databaseHelper.insertTrack(track);
        trackId = new Track.Id(primaryKey);
        return trackId;
    }

    public void pauseTrackRecording() {

    }

    public void resumeTrackRecording(Track.Id id) {

    }

    public void stopAndRemoveTrackRecording() {

    }

    public void stopTrackRecording() {

    }

    @Nullable
    public Track getRecordedTrack(Track.Id trackId) {
        if (!this.trackId.equals(trackId) ) {
            return databaseHelper.getTrack(trackId);
        }
        return null;
    }

    private void initializeGPSLocationManager() throws SecurityException {
        // start location updates
        // get location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // locationServicesAllowed is true, if gps available
        // minimal time between update = 1 sec
        // minimal distance = 1 m
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        gpsAvailable = true;
        try {

        } catch (SecurityException exception) {
            gpsAvailable = false;
            if (Global.DEBUG) {
                //TODO: Logging
                exception.printStackTrace();
            }
        }
    }

}
