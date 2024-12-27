package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.time.LocalDateTime;

public class UTScore {
    private final String bidderID;
    private LocalDateTime bidTime;
    private final Double score;

    private String tag;

    public UTScore(String bidderID, Double score){
        this.bidderID = bidderID;
        this.score = score;
    }

    public UTScore(String bidderID, LocalDateTime bidTime, Double score){
        this.bidderID = bidderID;
        this.score = score;
        this.bidTime = bidTime;
    }

    public void setTag(String tag){ this.tag = tag;}

    public String getTag(){ return tag;}

    public String getBidderID(){ return bidderID;}

    public Double getScore(){ return score;}

    public LocalDateTime getBidTime() {return bidTime;}
}
