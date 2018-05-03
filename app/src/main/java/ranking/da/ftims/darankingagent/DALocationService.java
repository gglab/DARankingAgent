package ranking.da.ftims.darankingagent;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DALocationService extends Service implements LocationListener, SensorEventListener {

    private LocationManager locManager;
    Location lastlocation = new Location("last");
    Trip trip;
    double currentLongitude = 0;
    double currentLattitude = 0;
    double lastLongitude = 0;
    double lastLattitude = 0;
    Integer currentSpeedLimit = 0;

    private SensorManager senManager;
    private Sensor gSensor;
    private ResponseGisSpeedLimit responseGisSpeedLimit;
    private long lastUpdate;
    private float last_x = 0f, last_y = 0f, last_z = 0f, last_v =0f;
    private static final float NOISE_THRESHOLD = 2f;
    private static final float ACC_THRESHOLD = 5f;
    private static final float BREAKS_THRESHOLD = 10f;

    PendingIntent contentIntent;

    @Override
    public void onCreate() {

//        Intent notificationIntent = new Intent(this, DrivingAnalyticsAgent.class);
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//
//        updateNotification(false);

        gSensor = senManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senManager.registerListener(this, gSensor , SensorManager.SENSOR_DELAY_NORMAL);
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
    }

//    public void updateNotification(boolean asData) {
//        Notification.Builder builder = new Notification.Builder(getBaseContext())
//                .setContentTitle(getString(R.string.running))
//                .setContentIntent(contentIntent);
//
//        if (asData) {
//            builder.setContentText(String.format(getString(R.string.notification), trip.getMaxSpeed(), trip.getDistance()));
//        } else {
//            builder.setContentText(String.format(getString(R.string.notification), '-', '-'));
//        }
//        Notification notification = builder.build();
//        startForeground(R.string.noti_id, notification);
//    }

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
            setSpeedLimit(currentLattitude, currentLongitude);
//            currentSpeedLimit = 5;
//            if(currentSpeedLimit != null){
//                trip.setSpeedLimit(currentSpeedLimit);
//            }
            Float distance = lastlocation.distanceTo(location);
            if (location.getAccuracy() < distance) {
                trip.addDistance(distance.longValue());
                lastLattitude = currentLattitude;
                lastLongitude = currentLongitude;
            }
            if (location.hasSpeed()) {
                lastlocation.setSpeed(location.getSpeed());
                Double speed = location.getSpeed() * 3.6d;
                trip.setCurSpeed(speed.intValue());
                if (location.getSpeed() == 0) {
                    new isStillStopped().execute();
                }
            }
            if(trip.getCurSpeed() > trip.getSpeedLimit() ){
                trip.addSpeedingDistance(distance.longValue());
                int maxSpeed = trip.getCurSpeed() - trip.getSpeedLimit();
                if(trip.getMaxSpeed()<maxSpeed){
                    trip.setMaxSpeed(maxSpeed);
                }
                trip.setSpeeding(true);
            }else{
                trip.setSpeeding(false);
            }

            trip.update();
            //updateNotification(true);
        }
    }

    private void setSpeedLimit(double currentLattitude, double currentLongitude) {
        Log.i("DA", "authenticateUser");
        Call<ResponseGisSpeedLimit> speedLimitCall = DrivingAnalyticsAgent.gisService.getGisSpeedingLimit(TokenCredentials.tokenId, currentLattitude, currentLongitude);
        try{
            speedLimitCall.enqueue(new Callback<ResponseGisSpeedLimit>() {
                @Override
                public void onResponse(Call<ResponseGisSpeedLimit> call, Response<ResponseGisSpeedLimit> response) {
                    if(response.isSuccessful()){
                        Log.i("DA", "Success: " + response.body().toString());
                        responseGisSpeedLimit = response.body();
                        trip.setSpeedLimit(responseGisSpeedLimit.getIntSpeedLimit());
                    }
                }
                @Override
                public void onFailure(Call<ResponseGisSpeedLimit> call, Throwable t) {
                    Log.e("DA", "Fail: " + t.toString());
                }
            });
        }
        catch(Exception e){
            Log.e("DA", "Exception: " + e.toString());
        }

    }

    @Override
    public void onDestroy() {
        locManager.removeUpdates(this);
        senManager.unregisterListener(this);
        //stopForeground(true);
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            trip = DrivingAnalyticsAgent.getTrip();
            if (trip.isRunning()) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                long curTime = System.currentTimeMillis();

                if ((curTime - lastUpdate) > 1000) {
                    long diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;
                    float g = (x * x + y * y + z * z)/(SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
                    float v = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 1000;
                    if (NOISE_THRESHOLD > g) {
                        g = 0f;
                        trip.setSuddenAcc(false);
                        trip.setSuddenAcc(false);
                    } else if (g > BREAKS_THRESHOLD && lastlocation.hasSpeed() && lastlocation.getSpeed() > v) {
                        trip.addSuddenBrakingNo();
                        trip.setSuddenBreaking(true);
                        trip.setSuddenAcc(false);
                    } else if (g > ACC_THRESHOLD && lastlocation.hasSpeed() && lastlocation.getSpeed() < v){
                        trip.addSuddenAccNo();
                        trip.setSuddenBreaking(false);
                        trip.setSuddenAcc(true);
                    }else{
                        trip.setSuddenAcc(false);
                        trip.setSuddenAcc(false);
                    }
                    last_x = x;
                    last_y = y;
                    last_z = z;
                }
                trip.update();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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
