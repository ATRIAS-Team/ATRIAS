package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.time.LocalDateTime;

public class UTScore {
    private String bidderID;
    private LocalDateTime bidTime;
    private Double score;

    private String tag;

    public UTScore(String bidderID, Double score){
        this.bidderID = bidderID;
        this.score = score;
    }

    public void setTag(String tag){ this.tag = tag;}

    public String getTag(){ return tag;}

    public String getBidderID(){ return bidderID;}

    public Double getScore(){ return score;}

    //todo: implement a contructor whioch uses bidTime

}
