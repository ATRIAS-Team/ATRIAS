package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import java.util.ArrayList;

public class MessageContent {
    String action;
    ArrayList<String> values;

    public MessageContent(String action, ArrayList<String> values){
        this.action = action;
        this.values = values;
    }

    public MessageContent(String action){
        this.action = action;
        this.values = new ArrayList();

    }

    public String getAction(){
        return this.action;
    }

    public ArrayList<String> getValues(){
        return this.values;
    }
    /*
    public String serialize(){

        String valuesStr = "[";
        for (int i = 0; i < values.size() - 1; i++) {
            valuesStr += values.get(i) + ",";
        }
        valuesStr += values.get(values.size() - 1) + "]";
        return "{"+"action:"+action+","+"values:"+valuesStr+"}";
         *
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static MessageContent deserialize(String messageStr){
        String[] parts = messageStr.split(",(?=values)");
        String action = parts[0].split(":")[1];
        ArrayList<String> values = (ArrayList<String>) Arrays.asList(parts[1].split(","));
        return new MessageContent(action, values);
    }
    */
}
