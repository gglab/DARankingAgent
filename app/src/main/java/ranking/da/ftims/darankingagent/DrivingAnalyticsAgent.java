package ranking.da.ftims.darankingagent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.stream.IntStream;

import ranking.da.ftims.darankingagent.rest.DARankingAppDriver;
import ranking.da.ftims.darankingagent.rest.GisService;
import ranking.da.ftims.darankingagent.rest.TokenCredentials;
import ranking.da.ftims.darankingagent.service.DALocationService;
import ranking.da.ftims.darankingagent.trip.Trip;
import ranking.da.ftims.darankingagent.rest.TripSyncResponse;
import ranking.da.ftims.darankingagent.trip.TripVM;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DrivingAnalyticsAgent extends AppCompatActivity implements LocationListener, SensorEventListener {

    private static final String GIS_PORT = "8081";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
    private static final SimpleDateFormat SDF_AGENT = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
    private static Trip trip;
    private SharedPreferences sharedPreferences;
    private DARankingAppDriver driver;
    private TextView mDriverView;
    private TextView mStartDateTime;
    private TextView mSatellitesView;
    private TextView mAccuracyView;
    private TextView mLatitudeView;
    private TextView mLongitudeView;
    private TextView mSpeedLimitView;
    private TextView mSpeedingDistView;
    private TextView mSpeedingVMaxView;
    private TextView mGView;
    private TextView mBraksNoView;
    private TextView mAccNoView;
    private TextView mStatusBarView;
    private TextView mDistanceView;
    private Chronometer mTimeView;
    private TextView mSpeedView;

    private FloatingActionButton startButton;
    private FloatingActionButton pauseButton;
    private FloatingActionButton resetButton;
    private FloatingActionButton syncTripButton;

    private LocationManager locManager;
    private Trip.onServiceUpdate onServiceUpdate;

    private SensorManager senManager;
    private Sensor gSensor;

    private long lastUpdate;
    private static final float NOISE_THRESHOLD = 0.25f;
    private static final int S = 1000;
    private float[] gValues;
    private float[] sumGValues = new float[3];
    private int i_acc = 0;
    private String url;
    private Retrofit retrofit;

    private static GisService gisService;

    private boolean firstfix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("DA", "DrivingAnalyticsAgent");
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        driver = (DARankingAppDriver) getIntent().getSerializableExtra("driver");
        initGisService();
        setContentView(R.layout.activity_driving_analytics_agent);

        trip = new Trip(onServiceUpdate, driver);
        onServiceUpdate = new Trip.onServiceUpdate() {
            @Override
            public void update() {
                long distanceTemp = trip.getDistance();
                String displayDist = String.valueOf(distanceTemp);
                mDistanceView.setText(displayDist);
                int speedLimit = trip.getSpeedLimit();
                String displaySpeedLimit = String.valueOf(speedLimit);
                if(trip.isLimitForLocation()){
                    mSpeedLimitView.setText(displaySpeedLimit);
                }else{
                    mSpeedLimitView.setText(displaySpeedLimit.concat("!"));
                }
                if(trip.isSpeeding()){
                    mStatusBarView.setText("Speeding!");
                    String displayMaxspeed = trip.getMaxSpeed().toString();
                    mSpeedingVMaxView.setText(displayMaxspeed);
                    long speedingDist = trip.getSpeedingDistance();
                    String displaySpeedingDist = String.valueOf(speedingDist);
                    mSpeedingDistView.setText(displaySpeedingDist);
                }else if(trip.isSuddenBreaking()){
                    mStatusBarView.setText("Sudden Breaking!");
                    String displaySuddenBreaking = trip.getSuddenBrakingNo().toString();
                    mBraksNoView.setText(displaySuddenBreaking);
                }else if(trip.isSuddenAcc()){
                    mStatusBarView.setText("Sudden Acceleration!");
                    String displaySuddenAcc = trip.getSuddenAccNo().toString();
                    mAccNoView.setText(displaySuddenAcc);
                }else{
                    mStatusBarView.setText("Safe driving :)");
                }
            }
        };

        mDriverView = findViewById(R.id.driver);
        mDriverView.setText(driver.getName());
        Log.i("DA", "Driver: " + driver.getName());

        mSpeedView = findViewById(R.id.speed);
        mSatellitesView = findViewById(R.id.satellites);
        mDistanceView = findViewById(R.id.distance);
        mTimeView = findViewById(R.id.time);
        mLatitudeView = findViewById(R.id.latitude);
        mLongitudeView = findViewById(R.id.longitude);
        mStartDateTime = findViewById(R.id.startDateTime);
        mAccuracyView  = findViewById(R.id.accuracy);
        mSpeedLimitView = findViewById(R.id.speedLimit);
        mSpeedingDistView = findViewById(R.id.speedingDist);
        mSpeedingVMaxView = findViewById(R.id.maxSpeeding);
        mGView = findViewById(R.id.g);
        mBraksNoView = findViewById(R.id.breaks);
        mAccNoView = findViewById(R.id.acc);

        mStatusBarView = findViewById(R.id.status);
        mStatusBarView.setText("Waiting for GPS signal...");
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        pauseButton.setVisibility(View.INVISIBLE);
        resetButton = findViewById(R.id.resetButton);
        resetButton.setVisibility(View.INVISIBLE);
        syncTripButton = findViewById(R.id.syncTripButton);
        syncTripButton.setVisibility(View.INVISIBLE);

        mTimeView.setText("00:00:00");
        mTimeView.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            boolean isPair = true;
            @Override
            public void onChronometerTick(Chronometer chrono) {
                long time;
                if(trip.isRunning()){
                    time= SystemClock.elapsedRealtime() - chrono.getBase();
                    trip.setTime(time);
                }else{
                    time = trip.getTime();
                }

                String displayTime = getTimeString(time);
                chrono.setText(displayTime);

                if (trip.isRunning()){
                    chrono.setText(displayTime);
                } else {
                    if (isPair) {
                        isPair = false;
                        chrono.setText(displayTime);
                    }else{
                        isPair = true;
                        chrono.setText("");
                    }
                }

            }
        });

        lastUpdate = System.currentTimeMillis();
        Log.i("DA", "LocationManager");
        locManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        senManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        gSensor = senManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void initGisService(){
        url = (String) getIntent().getSerializableExtra("url");
        StringBuffer server = new StringBuffer(url.substring(0, url.lastIndexOf(':') + 1));
        server.append(GIS_PORT);
        retrofit = new Retrofit.Builder().baseUrl(server.toString()).addConverterFactory((GsonConverterFactory.create())).build();
        gisService = retrofit.create(GisService.class);
    }

    public static GisService getGisService() {
        return gisService;
    }

    public void onStartClick(View v){
        if (!trip.isRunning()) {
            mStatusBarView.setText("Trip started");
            pauseButton.setVisibility(View.VISIBLE);
            trip.setRunning(true);
            Date now = Calendar.getInstance().getTime();
            trip.setStartDate(now);
            String displayNow = SDF_AGENT.format(now);
            mStartDateTime.setText(displayNow);
            mTimeView.setBase(SystemClock.elapsedRealtime() - trip.getTime());
            mTimeView.start();
            trip.setFirstTime(true);
            startService(new Intent(getBaseContext(), DALocationService.class));
            resetButton.setVisibility(View.INVISIBLE);
            startButton.setVisibility(View.INVISIBLE);
            syncTripButton.setVisibility(View.INVISIBLE);
        }
    }

    public void onPauseClick(View v){
        if (trip.isRunning()){
            pauseButton.setVisibility(View.INVISIBLE);
            trip.setRunning(false);
            mStatusBarView.setText("Trip is paused");
            stopService(new Intent(getBaseContext(), DALocationService.class));
            resetButton.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.VISIBLE);
            syncTripButton.setVisibility(View.VISIBLE);
        }
    }

    public void onResetClick(View v){
        resetButton.setVisibility(View.INVISIBLE);
        pauseButton.setVisibility(View.INVISIBLE);
        syncTripButton.setVisibility(View.INVISIBLE);
        mTimeView.stop();
        mStartDateTime.setText("");
        mBraksNoView.setText("");
        mAccNoView.setText("");
        mSpeedingVMaxView.setText("");
        mSpeedingDistView.setText("");
        mSpeedLimitView.setText("");
        mSpeedView.setText("");
        mDistanceView.setText("");
        mTimeView.setText("00:00:00");
        mStatusBarView.setText("New trip");
        trip = new Trip(onServiceUpdate, driver);
        stopService(new Intent(getBaseContext(), DALocationService.class));
    }

    public void onSyncTripClick(final View v){
        if(!trip.isRunning()){
            Log.i("DA", "Sending trip to server...");
            mStatusBarView.setText("Sending trip to server...");
            TripVM tripVM = new TripVM();
            tripVM.setDistance(trip.getDistance());
            tripVM.setDriver(trip.getDriver().getId());
            tripVM.setDuration(getTimeString(trip.getTime()));
            tripVM.setMaxSpeedingVelocity(trip.getMaxSpeed());
            tripVM.setSpeedingDistance(trip.getSpeedingDistance());
            tripVM.setStart(SDF.format(trip.getStartDate()));
            tripVM.setSuddenAccelerations(trip.getSuddenAccNo());
            tripVM.setSuddenBrakings(trip.getSuddenBrakingNo());
            Log.i("DA", tripVM.toString());

            Call<TripSyncResponse> TripSyncResponse = LoginActivity.service.createTripFromAgent(TokenCredentials.getTokenId(), tripVM);
            try{
                TripSyncResponse.enqueue(new Callback<TripSyncResponse>() {
                    @Override
                    public void onResponse(Call<TripSyncResponse> call, Response<TripSyncResponse> response) {
                        if(response.isSuccessful()){
                            Log.i("DA", "Success: " + response.body().toString());
                            onResetClick(v);
                        }
                    }
                    @Override
                    public void onFailure(Call<TripSyncResponse> call, Throwable t) {
                        Log.e("DA", "Fail: " + t.toString());
                    }
                });
            }
            catch(Exception e){
                Log.e("DA", "Exception: " + e.toString());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        locManager.removeUpdates(this);
        senManager.unregisterListener(this);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(trip);
        prefsEditor.putString("trip", json);
        prefsEditor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        firstfix = true;
        if (!trip.isRunning()){
            Gson gson = new Gson();
            String json = sharedPreferences.getString("trip[", "");
            trip = gson.fromJson(json, Trip.class);
        }
        if (trip == null){
            trip = new Trip(onServiceUpdate, driver);
        }else{
            trip.setOnGpsServiceUpdate(onServiceUpdate);
        }
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        senManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopService(new Intent(getBaseContext(), DALocationService.class));
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.hasAccuracy()) {
            Float accuracy = location.getAccuracy();
            String displayAccuracy = accuracy.toString();
            mAccuracyView.setText(displayAccuracy);
            if (firstfix){
                mStatusBarView.setText("");
                startButton.setVisibility(View.VISIBLE);
                if (!trip.isRunning()) {
                    resetButton.setVisibility(View.INVISIBLE);
                }
                firstfix = false;
            }
        }else{
            firstfix = true;
        }
        if (location.hasSpeed()) {
            Double speed = location.getSpeed() * 3.6;
            String displaySpeed = String.valueOf(speed.intValue());
            mSpeedView.setText(displaySpeed);
        }

        Double lat = location.getLatitude();
        String displayLatitude = String.format("%.6f", lat);
        mLatitudeView.setText(displayLatitude);
        Double lon = location.getLongitude();
        String displayLongitude = String.format("%.6f", lon);
        mLongitudeView.setText(displayLongitude);
    }

    private String getTimeString(long time){
        StringBuffer result = new StringBuffer();
        int h   = (int)(time /3600000);
        int m = (int)(time  - h*3600000)/60000;
        int s= (int)(time  - h*3600000 - m*60000)/1000 ;
        if(h<10){
            result.append("0");
        }
        result.append(h);
        result.append(":");
        if(m<10){
            result.append("0");
        }
        result.append(m);
        result.append(":");
        if(s<10){
            result.append("0");
        }
        result.append(s);
        return result.toString();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (extras.get("satellites") != null) {
            String displaySatellites = String.valueOf(extras.getInt("satellites"));
            mSatellitesView.setText(displaySatellites);
        }
    }

    public static Trip getTrip() {
        return trip;
    }

    public void onBackPressed(){
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
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
            gValues = lowPass(event.values.clone(), gValues);
            for (int i = 0; i < gValues.length; i++) {
                sumGValues[i] += gValues[i];
            }
            i_acc++;
            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) >= S) {
                float x_avg = sumGValues[0]/i_acc;
                float y_avg = sumGValues[1]/i_acc;
                float z_avg = sumGValues[2]/i_acc;
                i_acc = 0;
                sumGValues = new float[3];
                long diffTime = curTime - lastUpdate;
                lastUpdate = curTime;
                double g = Math.sqrt(x_avg * x_avg + y_avg * y_avg + z_avg * z_avg);
                double v = g * diffTime/S;
                StringBuffer sb = new StringBuffer();
                sb.append(String.format("%.2f", g));
                sb.append("[m/s2] ");
                sb.append(String.format("%.2f", v));
                sb.append("[m/s]");
                mGView.setText(sb.toString());
            }
        }
    }

    protected float[] lowPass( float[] input, float[] output ) {
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
