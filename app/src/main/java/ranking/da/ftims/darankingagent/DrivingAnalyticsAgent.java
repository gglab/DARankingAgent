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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DrivingAnalyticsAgent extends AppCompatActivity implements LocationListener, SensorEventListener {

    //private static final Integer CALCULATION_POINTS = 2;
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
    //private LocationListener locListener;
    private Trip.onServiceUpdate onServiceUpdate;

    private SensorManager senManager;
    private Sensor gSensor;

    private long lastUpdate;
    private static final float NOISE_THRESHOLD = 2f;
    private float last_x = 0f, last_y = 0f, last_z = 0f, last_v =0f;

    private String url;
    private Retrofit retrofit;
    static GisService gisService;

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
                mSpeedLimitView.setText(displaySpeedLimit);
                if(trip.isSpeeding()){
                    mStatusBarView.setText("Speeding!");
                    String displayMaxspeed = trip.getMaxSpeed().toString();
                    mSpeedingVMaxView.setText(displayMaxspeed);
                    long speedingDist = trip.getSpeedingDistance();
                    String displaySpeedingDist = String.valueOf(speedingDist);
                    mSpeedingDistView.setText(displaySpeedingDist);
                }else if(trip.isSuddenBreaking()){
                    mStatusBarView.setText("Sudden Breaking!");
                    String displaySudddenBreaking = trip.getSuddenBrakingNo().toString();
                    mBraksNoView.setText(displaySudddenBreaking);
                }else if(trip.isSuddenAcc()){
                    mStatusBarView.setText("Sudden Acceleration!");
                    String displaySudddenAcc = trip.getSuddenAccNo().toString();
                    mAccNoView.setText(displaySudddenAcc);
                }else{
                    mStatusBarView.setText("Safe driving :)");
                }
            }
        };

        mDriverView = findViewById(R.id.driver);
        mDriverView.setText(driver.name);
        Log.i("DA", "Driver: " + driver.name);

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
        gSensor = senManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        //locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 0, locListener);
    }

    private void initGisService(){
        url = (String) getIntent().getSerializableExtra("url");
        StringBuffer server = new StringBuffer(url.substring(0, url.lastIndexOf(':') + 1));
        server.append(":");
        server.append(GIS_PORT);
        retrofit = new Retrofit.Builder().baseUrl(server.toString()).addConverterFactory((GsonConverterFactory.create())).build();
        gisService = retrofit.create(GisService.class);
    }

    public void onStartClick(View v){
        if (!trip.isRunning()) {
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
            mStatusBarView.setText("");
            stopService(new Intent(getBaseContext(), DALocationService.class));
            resetButton.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.VISIBLE);
            syncTripButton.setVisibility(View.VISIBLE);
        }
    }

    public void onResetClick(View v){
        resetData();
        stopService(new Intent(getBaseContext(), DALocationService.class));
    }

    public void onSyncTripClick(final View v){
        Log.i("DA", "Sending trip to server...");
        TripVM tripVM = new TripVM();
        tripVM.distance = trip.getDistance();
        tripVM.driver = trip.getDriver().id;
        tripVM.duration = getTimeString(trip.getTime());
        tripVM.maxSpeedingVelocity = trip.getMaxSpeed();
        tripVM.speedingDistance = trip.getSpeedingDistance();
        tripVM.start = SDF.format(trip.getStartDate());
        tripVM.suddenAccelerations = trip.getSuddenAccNo();
        tripVM.suddenBrakings = trip.getSuddenBrakingNo();
        Log.i("DA", tripVM.toString());

        Call<TripSyncResponse> TripSyncResponse = LoginActivity.service.createTripFromAgent(TokenCredentials.tokenId, tripVM);
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

    public void resetData(){
        resetButton.setVisibility(View.INVISIBLE);
        pauseButton.setVisibility(View.INVISIBLE);
        syncTripButton.setVisibility(View.INVISIBLE);
        mTimeView.stop();
        mStartDateTime.setText("");
        mBraksNoView.setText("");
        mSpeedingVMaxView.setText("");
        mSpeedingDistView.setText("");
        mSpeedLimitView.setText("");
        mSpeedView.setText("");
        mDistanceView.setText("");
        mTimeView.setText("00:00:00");
        trip = new Trip(onServiceUpdate, driver);
    }

    @Override
    public void onPause() {
        super.onPause();
        locManager.removeUpdates(this);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(trip);
        prefsEditor.putString("trip", json);
        prefsEditor.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        firstfix = true;
        if (!trip.isRunning()){
            Gson gson = new Gson();
            String json = sharedPreferences.getString("tri[", "");
            trip = gson.fromJson(json, Trip.class);
        }
        if (trip == null){
            trip = new Trip(onServiceUpdate, driver);
        }else{
            trip.setOnGpsServiceUpdate(onServiceUpdate);
        }

        if (locManager.getAllProviders().indexOf(LocationManager.GPS_PROVIDER) >= 0) {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        } else {
            Log.w("MainActivity", "No GPS location provider found. GPS data display will not be available.");
        }

        if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsDisabledDialog();
        }

        //locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locListener);
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
            Float speed = location.getSpeed() * 3.6f;
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
        Log.i("DA", "Status changed ");
        Log.i("DA", "Status changed: " + provider);
        Log.i("DA", "Status changed: " + status);
        Log.i("DA", "Status changed: " + extras.toString());
        if (extras.get("satellites") != null) {
            String displaySatellites = String.valueOf(extras.getInt("satellites"));
            mSatellitesView.setText(displaySatellites);
            Log.i("DA", "Satellites: " + String.valueOf(extras.getInt("satellites")));
        }
    }

    public void showGpsDisabledDialog(){
        Log.e("DA", "GPS Disabled!");
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
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 1000) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                float g = (x * x + y * y + z * z)/(SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
                float v = Math.abs(x + y + z - last_x - last_y - last_z)/ diffTime * 10000;
                if (NOISE_THRESHOLD > g) {
                    g = 0f;
                    v = 0f;
                }
                StringBuffer sb = new StringBuffer();
                sb.append(g);
                sb.append(" m/s2 ");
                sb.append(v);
                sb.append(" m/s");
                mGView.setText(sb.toString());
                last_x = x;
                last_y = y;
                last_z = z;
                last_v = v;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
