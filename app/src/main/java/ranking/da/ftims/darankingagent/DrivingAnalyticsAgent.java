package ranking.da.ftims.darankingagent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class DrivingAnalyticsAgent extends AppCompatActivity {

    private DARankingAppDriver driver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("DA", "DrivingAnalyticsAgent");
        driver = (DARankingAppDriver) getIntent().getSerializableExtra("driver");
        setContentView(R.layout.activity_driving_analytics_agent);
        Log.i("DA", "Driver: " + driver.name);
    }
}
