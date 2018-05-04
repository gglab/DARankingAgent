package ranking.da.ftims.darankingagent.rest;

import java.io.Serializable;

public class DARankingAppDriver implements Serializable{
    private String id;
    private String rank;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
