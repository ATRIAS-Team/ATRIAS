package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.time.LocalDateTime;

public class UTScore {
    private String bidderID;
    private LocalDateTime bidTime;
    private Double score;

    public UTScore(String bidderID, Double score){
        this.bidderID = bidderID;
        this.score = score;
    }

    //todo: implement a contructor whioch uses bidTime

}
