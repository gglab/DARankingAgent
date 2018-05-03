package ranking.da.ftims.darankingagent;

import java.util.Date;

public class Trip {

    private boolean isRunning;
    private boolean isSpeeding;
    private boolean isSuddenAcc;
    private boolean isSuddenBreaking;
    private Long time;
    private Long timeStopped;
    private boolean isFirstTime;
    private Date startDate;
    private Long distanceM;
    private Integer curSpeed;
    private Integer maxSpeed;
    private Long speedingDistance;
    private Integer suddenBrakingNo;
    private Integer suddenAccNo;
    private Integer speedLimit;

    private final DARankingAppDriver driver;

    private onServiceUpdate onServiceUpdate;

    public DARankingAppDriver getDriver() {
        return driver;
    }

    public Integer getSuddenBrakingNo() {
        return suddenBrakingNo;
    }

    public void addSuddenBrakingNo() {
        this.suddenBrakingNo ++;
    }

    public Integer getSuddenAccNo() {
        return suddenAccNo;
    }

    public void addSuddenAccNo() {
        this.suddenAccNo ++;
    }

    public boolean isSuddenAcc() {
        return isSuddenAcc;
    }

    public void setSuddenAcc(boolean suddenAcc) {
        isSuddenAcc = suddenAcc;
    }

    public boolean isSuddenBreaking() {
        return isSuddenBreaking;
    }

    public void setSuddenBreaking(boolean suddenBreaking) {
        isSuddenBreaking = suddenBreaking;
    }

    public boolean isSpeeding() {
        return isSpeeding;
    }

    public void setSpeeding(boolean speeding) {
        isSpeeding = speeding;
    }
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
    public Date getStartDate() {
        return startDate;
    }

    public Integer getSpeedLimit() {
        return speedLimit;
    }

    public void setSpeedLimit(Integer speedLimit) {
        this.speedLimit = speedLimit;
    }

    public interface onServiceUpdate{
        public void update();
    }

    public void setOnGpsServiceUpdate(onServiceUpdate onGpsServiceUpdate){
        this.onServiceUpdate = onServiceUpdate;
    }

    public void update(){
        onServiceUpdate.update();
    }

    public Trip(onServiceUpdate onGpsServiceUpdate, DARankingAppDriver driver){
        isRunning = false;
        distanceM = 0L;
        curSpeed = 0;
        maxSpeed = 0;
        timeStopped = 0L;
        speedingDistance = 0L;
        suddenBrakingNo = 0;
        suddenAccNo = 0;
        time = 0L;
        speedLimit = 0;
        isSpeeding = false;
        this.driver = driver;
        setOnGpsServiceUpdate(onGpsServiceUpdate);
    }

    public void addDistance(long distance){
        distanceM = distanceM + distance;
    }

    public Long getDistance(){
        return distanceM;
    }

    public void addSpeedingDistance(long distance){
        speedingDistance = speedingDistance + distance;
    }

    public Long getSpeedingDistance(){
        return speedingDistance;
    }

    public void setMaxSpeed(int max){
        this.maxSpeed = max;
    }

    public Integer getMaxSpeed() {
        return maxSpeed;
    }

    public void setCurSpeed(int curSpeed) {
        this.curSpeed = curSpeed;
    }

    public boolean isFirstTime() {
        return isFirstTime;
    }

    public void setFirstTime(boolean isFirstTime) {
        this.isFirstTime = isFirstTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public void setTimeStopped(long timeStopped) {
        this.timeStopped += timeStopped;
    }

    public Integer getCurSpeed() {
        return curSpeed;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
