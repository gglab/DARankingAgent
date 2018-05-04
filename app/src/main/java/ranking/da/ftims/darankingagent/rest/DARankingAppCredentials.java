package ranking.da.ftims.darankingagent.rest;

import java.io.Serializable;

public class DARankingAppCredentials implements Serializable{

    private String password;
    private boolean rememberMe;
    private String username;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }



}
