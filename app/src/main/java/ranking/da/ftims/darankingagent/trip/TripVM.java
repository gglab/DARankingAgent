package ranking.da.ftims.darankingagent.trip;

import java.io.Serializable;

public class TripVM implements Serializable {

    private Long distance;
    private String driver;
    private String duration;
    private Integer maxSpeedingVelocity;
    private Long speedingDistance;
    private String start;
    private Integer suddenAccelerations;
    private Integer suddenBrakings;

    public Long getDistance() {
        return distance;
    }

    public void setDistance(Long distance) {
        this.distance = distance;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Integer getMaxSpeedingVelocity() {
        return maxSpeedingVelocity;
    }

    public void setMaxSpeedingVelocity(Integer maxSpeedingVelocity) {
        this.maxSpeedingVelocity = maxSpeedingVelocity;
    }

    public Long getSpeedingDistance() {
        return speedingDistance;
    }

    public void setSpeedingDistance(Long speedingDistance) {
        this.speedingDistance = speedingDistance;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public Integer getSuddenAccelerations() {
        return suddenAccelerations;
    }

    public void setSuddenAccelerations(Integer suddenAccelerations) {
        this.suddenAccelerations = suddenAccelerations;
    }

    public Integer getSuddenBrakings() {
        return suddenBrakings;
    }

    public void setSuddenBrakings(Integer suddenBrakings) {
        this.suddenBrakings = suddenBrakings;
    }

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
