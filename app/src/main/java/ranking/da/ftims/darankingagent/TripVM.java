package ranking.da.ftims.darankingagent;

import java.io.Serializable;
import java.util.Date;

public class TripVM implements Serializable {

    Long distance;
    String driver;
    String duration;
    Integer maxSpeedingVelocity;
    Long speedingDistance;
    String start;
    Integer suddenAccelerations;
    Integer suddenBrakings;

    @Override
    public String toString() {
        return "TripVM{" +
                "distance=" + distance +
                ", driver='" + driver + '\'' +
                ", duration=" + duration +
                ", maxSpeedingVelocity=" + maxSpeedingVelocity +
                ", speedingDistance=" + speedingDistance +
                ", start=" + start +
                ", suddenAccelerations=" + suddenAccelerations +
                ", suddenBrakings=" + suddenBrakings +
                '}';
    }
}
