
import java.util.ArrayList;


// Channel class to store channels and allow users to publish messages and subcribe and unsubscribe it
public class Channel {
    private String channelId;
    private String channelName;
    private ArrayList<Message> messages;
    private ArrayList<String> users;
    public Channel(String channelId, String channelName, ArrayList<String> users) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.users = users;
        messages = new ArrayList<>();
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public ArrayList<String> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<String> users) {
        this.users = users;
    }
    
}
