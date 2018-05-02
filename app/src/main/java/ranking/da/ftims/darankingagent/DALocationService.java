package ranking.da.ftims.darankingagent;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;

public class DALocationService extends Service implements LocationListener {

    private LocationManager locManager;
    Location lastlocation = new Location("last");
    Trip trip;
    double currentLongitude = 0;
    double currentLattitude = 0;
    double lastLongitude = 0;
    double lastLattitude = 0;
    Integer lastSpeedLimit = 0;
    Integer currentSpeedLimit = 0;

    PendingIntent contentIntent;

    @Override
    public void onCreate() {

        Intent notificationIntent = new Intent(this, DrivingAnalyticsAgent.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        updateNotification(false);

        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
    }

    public void updateNotification(boolean asData) {
        Notification.Builder builder = new Notification.Builder(getBaseContext())
                .setContentTitle(getString(R.string.running))
                .setContentIntent(contentIntent);

        if (asData) {
            builder.setContentText(String.format(getString(R.string.notification), trip.getMaxSpeed(), trip.getDistance()));
        } else {
            builder.setContentText(String.format(getString(R.string.notification), '-', '-'));
        }
        Notification notification = builder.build();
        startForeground(R.string.noti_id, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        trip = DrivingAnalyticsAgent.getTrip();
        if (trip.isRunning()) {
            currentLattitude = location.getLatitude();
            currentLongitude = location.getLongitude();
            if (trip.isFirstTime()) {
                lastLattitude = currentLattitude;
                lastLongitude = currentLongitude;
                trip.setFirstTime(false);
            }
            lastlocation.setLatitude(lastLattitude);
            lastlocation.setLongitude(lastLongitude);
            currentSpeedLimit = 5;
            if(currentSpeedLimit != null){
                trip.setSpeedLimit(currentSpeedLimit);
                lastSpeedLimit = currentSpeedLimit;
            }
            double distance = lastlocation.distanceTo(location);
            if (location.getAccuracy() < distance) {
                trip.addDistance(distance);
                lastLattitude = currentLattitude;
                lastLongitude = currentLongitude;
            }
            if (location.hasSpeed()) {
                Double speed = location.getSpeed() * 3.6d;
                trip.setCurSpeed(speed);
                if (location.getSpeed() == 0) {
                    new isStillStopped().execute();
                }
            }
            if(trip.getCurSpeed().intValue() > trip.getSpeedLimit() ){
                trip.addSpeedingDistance(distance);
                int maxSpeed = trip.getCurSpeed().intValue() - trip.getSpeedLimit();
                if(trip.getMaxSpeed()<maxSpeed){
                    trip.setMaxSpeed(maxSpeed);
                }
                trip.setSpeeding(true);
            }else{
                trip.setSpeeding(false);
            }

            trip.update();
            updateNotification(true);
        }
    }

    @Override
    public void onDestroy() {
        locManager.removeUpdates(this);
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    class isStillStopped extends AsyncTask<Void, Integer, String> {
        int timer = 0;

        @Override
        protected String doInBackground(Void... unused) {
            try {
                while (trip.getCurSpeed() == 0) {
                    Thread.sleep(1000);
                    timer++;
                }
            } catch (InterruptedException t) {
                return ("The sleep operation failed");
            }
            return ("return object when task is finished");
        }

        @Override
        protected void onPostExecute(String message) {
            trip.setTimeStopped(timer);
        }
    }

}
