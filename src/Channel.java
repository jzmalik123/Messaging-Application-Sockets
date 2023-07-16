
import org.json.simple.JSONObject;

import java.util.ArrayList;


// Channel class to store channels and allow users to publish messages and subcribe and unsubscribe it
public class Channel {
    private String identity; // Mapped to clients identity
    private ArrayList<Message> messages; // Array to store messages
    private ArrayList<String> subscribers; //
    private ArrayList<Server.ClientThread> subscribedThreads; // Threads of clients subscribed to channel
    public Channel(String identity, ArrayList<String> subscribers) {
        this.identity = identity;
        this.subscribers = subscribers;
        messages = new ArrayList<>();
        this.subscribedThreads = new ArrayList<>();
    }
    public Channel(String identity) {
        this.identity = identity;
        this.subscribers = new ArrayList<>();;
        messages = new ArrayList<>();
        this.subscribedThreads = new ArrayList<>();
    }
    public void addSubscriber(String identity){
        subscribers.add(identity);
    }
    public void removeSubscriber(String identity){
        subscribers.remove(identity);
    }
    public void addSubscriberThread(Server.ClientThread thread){
        if(!subscribedThreads.contains(thread)){
            subscribedThreads.add(thread);
        }
    }
    public void unsubscribeThread(Server.ClientThread thread){
        if(subscribedThreads.contains(thread)){
            subscribedThreads.remove(thread);
        }
    }
    public ArrayList<Message> getMessages() {
        return messages;
    }

    public String getIdentity() {
        return identity;
    }

    public void sendMessageToSubscribers(Message message){
        JSONObject response = message.toJSONObject();
        for (Server.ClientThread thread : this.subscribedThreads) {
            thread.sendResponse(response);
        }
    }
}
