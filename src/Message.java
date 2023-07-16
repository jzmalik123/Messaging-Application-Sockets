
import org.json.simple.JSONObject;

import java.io.*;

//Message class to store user messages
public class Message implements Comparable<Message>{

    private final String from;
    private final String body;
    private final long when;
    private final String pic;

    public Message(String from, String body, long when, String pic) {
        this.from = from;
        this.body = body;
        this.when = System.currentTimeMillis();
        this.pic = pic;
    }

    public Message(JSONObject message){
        this.from = (String) message.get("from");
        this.body = (String) message.get("body");
        this.when = System.currentTimeMillis();
        this.pic = (String) message.get("pic");
    }
    public boolean isValid() {
        return body.length() < 1234;
    }

    public JSONObject toJSONObject(){
        JSONObject message = new JSONObject();
        message.put("from", this.from);
        message.put("body", this.body);
        message.put("when", this.when);
        if(this.pic != null && !this.pic.equalsIgnoreCase("")){
            message.put("pic", this.pic);
        }
        return message;
    }

    public String getBody() {
        return body;
    }

    public long getWhen() {
        return when;
    }

    // overriding the compareTo method of Comparable class
    @Override public int compareTo(Message compareMessage) {
        long compareWhen = ((Message)compareMessage).getWhen();

        //  For Ascending order
        return (int) (this.when - compareWhen);
    }
}
