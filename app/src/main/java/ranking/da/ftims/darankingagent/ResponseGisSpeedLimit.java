package ranking.da.ftims.darankingagent;

import java.io.Serializable;

public class ResponseGisSpeedLimit implements Serializable {
    public String getSpeedLimit() {
        return speedLimit;
    }

    public void setSpeedLimit(String speedLimit) {
        this.speedLimit = speedLimit;
    }

    public Integer getIntSpeedLimit(){
        return Integer.parseInt(speedLimit);
    }

    String speedLimit;

}
