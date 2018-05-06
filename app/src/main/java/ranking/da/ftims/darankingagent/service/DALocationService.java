package ranking.da.ftims.darankingagent.service;

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
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import ranking.da.ftims.darankingagent.DrivingAnalyticsAgent;
import ranking.da.ftims.darankingagent.rest.TokenCredentials;
import ranking.da.ftims.darankingagent.trip.Trip;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DALocationService extends Service implements LocationListener, SensorEventListener {

    private LocationManager locManager;
    private Location lastLocation;
    private Trip trip;
    private double currentLongitude;
    private double currentLatitude;
    private double lastLongitude;
    private double lastLatitude;
    private Integer currentSpeedLimit;

    private SensorManager senManager;
    private Sensor gSensor;
    private long lastUpdate;

    private static final float BREAKS_THRESHOLD = 3f;
    private static final float ACC_THRESHOLD = 2f;
    private static final float NOISE_THRESHOLD = 0.25f;
    private static final int S = 1000;
    private float[] gValues;
    private float[] sumGValues = new float[3];
    private int i_acc = 0;

    @Override
    public void onCreate() {

        lastLocation = new Location("last");
        lastLocation.setSpeed(0f);
        currentLongitude = currentLatitude = lastLatitude = lastLongitude = 0d;
        currentSpeedLimit = 50;
        senManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        gSensor = senManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senManager.registerListener(this, gSensor , SensorManager.SENSOR_DELAY_NORMAL);
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        trip = DrivingAnalyticsAgent.getTrip();
        if (trip.isRunning()) {
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
            if (trip.isFirstTime()) {
                lastLatitude = currentLatitude;
                lastLongitude = currentLongitude;
                trip.setFirstTime(false);
            }
            lastLocation.setLatitude(lastLatitude);
            lastLocation.setLongitude(lastLongitude);
            setSpeedLimit(currentLatitude, currentLongitude);
            Float distance = lastLocation.distanceTo(location);
            if (location.getAccuracy() < distance) {
                trip.addDistance(distance.longValue());
                lastLatitude = currentLatitude;
                lastLongitude = currentLongitude;
            }
            if (location.hasSpeed()){
                float speedMS = location.getSpeed();
                if(speedMS<lastLocation.getSpeed()){
                  trip.setSlowing(true);
                }else{
                    trip.setSlowing(false);
                }
                lastLocation.setSpeed(location.getSpeed());
                Double speed = speedMS * 3.6d;
                trip.setCurSpeed(speed.intValue());
                if (speed > trip.getSpeedLimit().doubleValue() && location.getAccuracy() < distance) {
                    trip.addSpeedingDistance(distance.longValue());
                    int maxSpeed = trip.getCurSpeed() - trip.getSpeedLimit();
                    if (trip.getMaxSpeed() < maxSpeed) {
                        trip.setMaxSpeed(maxSpeed);
                    }
                    trip.setSpeeding(true);
                } else {
                    trip.setSpeeding(false);
                }
            }
            trip.update();
        }
    }

    private void setSpeedLimit(double currentLatitude, double currentLongitude) {
        Log.i("DA", "get speed limit from GIS for location: " + currentLongitude + " - " + currentLatitude);
        Call<String> speedLimitCall = DrivingAnalyticsAgent.getGisService().getGisSpeedingLimit(TokenCredentials.getTokenId(), currentLongitude, currentLatitude);
        try{
            speedLimitCall.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if(response.isSuccessful()){
                        try{
                            Log.w("DA", "Success!: Speed limit = " + response.body());
                            trip.setSpeedLimit(Integer.parseInt(response.body()));
                            trip.setLimitForLocation(true);
                        }catch(NumberFormatException e){
                            Log.w("DA", "No speed limit value for given location: " + response.body());
                            trip.setLimitForLocation(false);
                            trip.setDefaultSpeedLimit();
                        }
                    }
                }
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("DA", "Fail: " + t.toString());
                    trip.setLimitForLocation(false);
                    trip.setDefaultSpeedLimit();
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            trip = DrivingAnalyticsAgent.getTrip();
            if (trip.isRunning()) {
                gValues = lowPass(event.values.clone(), gValues);
                for (int i = 0; i < gValues.length; i++) {
                    sumGValues[i] += gValues[i];
                }
                i_acc++;
                long curTime = System.currentTimeMillis();
                if ((curTime - lastUpdate) >= S) {
                    float x_avg = sumGValues[0] / i_acc;
                    float y_avg = sumGValues[1] / i_acc;
                    float z_avg = sumGValues[2] / i_acc;
                    i_acc = 0;
                    sumGValues = new float[3];
                    long diffTime = curTime - lastUpdate;
                    lastUpdate = curTime;
                    double g = Math.sqrt(x_avg * x_avg + y_avg * y_avg + z_avg * z_avg);
                    double v = g * diffTime/S;
                    if (g > BREAKS_THRESHOLD && trip.isSlowing() ) {
                        trip.addSuddenBrakingNo();
                        trip.setSuddenBreaking(true);
                        trip.setSuddenAcc(false);
                    } else if (g > ACC_THRESHOLD && !trip.isSlowing()) {
                        trip.addSuddenAccNo();
                        trip.setSuddenBreaking(false);
                        trip.setSuddenAcc(true);
                    } else {
                        trip.setSuddenAcc(false);
                        trip.setSuddenAcc(false);
                    }
                }
            }
        }
    }


    private float[] lowPass( float[] input, float[] output ) {
            if ( output == null ) return input;
            for ( int i=0; i<input.length; i++ ) {
                output[i] = output[i] + NOISE_THRESHOLD * (input[i] - output[i]);
            }
            return output;
        }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
