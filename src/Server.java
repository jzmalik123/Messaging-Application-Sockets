
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

//Server class to start a server and to wait and accept clients
//Server will create threads for all client with there sockets and send message to each user using those sockets
public class Server {

    private static int clientId = 0;
    private final ArrayList<ClientThread> threadList;
    private final SimpleDateFormat dateFormat;
    private final int portNumber;
    private boolean flag;
    private final ArrayList<Channel> channels;

    // Constructor used to create channel list and initialize variables
    public Server(int port) {
        this.portNumber = port;
        dateFormat = new SimpleDateFormat("HH:mm:ss");
        threadList = new ArrayList<>();
        channels = new ArrayList<>();
    }


    // Method to start the server on the given port number
    public void startServer() {
        flag = true;
        try {
            try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
                while (flag) {
//                    socket to listen for the connection to be made to this socket. 
                    Socket socket = serverSocket.accept();
                    if (!flag) {
                        break;
                    }
//                    server creating a client thread here to listen for client messages
                    ClientThread t = new ClientThread(socket);
                    threadList.add(t);

                    t.start();
                }
            }
//            closing all the clients
            for (int i = 0; i < threadList.size(); ++i) {
                ClientThread tc = threadList.get(i);
                tc.inputStream.close();
                tc.outputStream.close();
                tc.socket.close();
            }
        } catch (IOException e) {
        }
    }

    private synchronized void openRequest(ClientThread thread){
        Channel userChannel = new Channel(thread.identity);
        userChannel.addSubscriber(thread.identity);
        userChannel.addSubscriberThread(thread);
        channels.add(userChannel);
    }

    private synchronized void respondSuccess(ClientThread thread){
        JSONObject response = new JSONObject();
        response.put("_class", "SuccessResponse");
        thread.sendResponse(response);
    }

    private synchronized void respondError(ClientThread thread, String error){
        JSONObject response = new JSONObject();
        response.put("_class", "ErrorResponse");
        response.put("error", error);
        thread.sendResponse(response);
    }

    private synchronized void respondMessageList(ClientThread thread, ArrayList<Message> sentMessages){
        ArrayList<JSONObject> messages = new ArrayList<>();
        JSONArray jsonMessages = new JSONArray();
        for (Message message : sentMessages){
            jsonMessages.add(message.toJSONObject());
            messages.add(message.toJSONObject());
        }
        JSONObject response = new JSONObject();
        response.put("_class", "MessageListResponse");
        response.put("messages", jsonMessages);
        thread.sendResponse(response);
    }
    private synchronized Channel getChannelByIdentity(String identity){
        Channel channel = null;
        for (Channel c : channels) {
            if (c.getIdentity().equalsIgnoreCase(identity)) {
                channel = c;
            }
        }
        return channel;
    }

    private synchronized ArrayList<Channel> getSubscribedChannels(ClientThread thread){
        ArrayList<Channel> subscribedChannels = new ArrayList<>();
        for (Channel channel : channels) {
            if (channel.getIdentity().equalsIgnoreCase(thread.identity)) {
                subscribedChannels.add(channel);
            }
        }
        return subscribedChannels;
    }

    private synchronized ArrayList<Message> getSentMessages(ClientThread thread, long after){
        ArrayList<Channel> subscribedChannels = getSubscribedChannels(thread);
        ArrayList<Message> sentMessages = new ArrayList<>();
        for (Channel subscribedChannel : subscribedChannels) {
            for (Message message : subscribedChannel.getMessages()) {
                if (message.getWhen() > after) {
                    sentMessages.add(message);
                }
            }
        }
        Collections.sort(sentMessages);
        return sentMessages;
    }

    // Thread client class to create a thread of each client on server to receive messages and give response to others accordingly
    class ClientThread extends Thread {

        Socket socket;
        ObjectInputStream inputStream;
        ObjectOutputStream outputStream;
        int id;
        String clientIP;
        String identity;
        JSONObject request;
        String date;

        ClientThread(Socket socket) {
            id = ++clientId;
            this.socket = socket;
            clientIP = socket.getInetAddress().getHostAddress();
            if (id != Integer.parseInt(clientIP.substring(clientIP.length() - 1, clientIP.length()))) {
                clientIP = clientIP.substring(0, clientIP.length() - 1);
                clientIP += id;
            } else {
                clientIP += ++id;
            }
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                inputStream = new ObjectInputStream(socket.getInputStream());
                identity = (String) inputStream.readObject();
            } catch (IOException e) {
                return;
            } catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        // Thread class method override will run when thread will be started
        @Override
        public void run() {
            boolean running = true;
            while (running) {

                // Listen for the messages from the client
                try {
                    request = (JSONObject) inputStream.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    break;
                }

                String requestType = (String) request.get("_class");
                this.identity = (String) request.get("identity");

                if(requestType.equalsIgnoreCase("OpenRequest")){
                    openRequest(this);
                    respondSuccess(this);
                } else if (requestType.equalsIgnoreCase("PublishRequest")) {
                    Channel channel = getChannelByIdentity(identity);
                    if(channel != null){
                        Message message = new Message((JSONObject) request.get("message"));
                        if(message.isValid()){
                            // Add message to the channel
                            channel.getMessages().add(message);

                            // Send message to all subscribers of this channel
                            channel.sendMessageToSubscribers(message);

                            // Return Success
                            respondSuccess(this);
                        } else {
                            // Respond with Error if length is extra
                            respondError(this, "MESSAGE TOO BIG: " + message.getBody().length());
                        }
                    } else {
                        // Respond with error if channel is invalid
                        respondError(this, "No Such Channel: " + identity);
                    }
                } else if (requestType.equalsIgnoreCase("SubscribeRequest")) {
                    String requestedChannel = (String) request.get("channel");
                    Channel channel = getChannelByIdentity(requestedChannel);
                    if(channel != null){
                        channel.addSubscriberThread(this);
                        channel.addSubscriber(this.identity);
                        respondSuccess(this);
                    } else {
                        // Respond with error if channel is invalid
                        respondError(this, "No Such Channel: " + requestedChannel);
                    }
                } else if (requestType.equalsIgnoreCase("UnsubscribeRequest")) {
                    String requestedChannel = (String) request.get("channel");
                    Channel channel = getChannelByIdentity(requestedChannel);
                    if(channel != null){
                        channel.unsubscribeThread(this);
                        channel.removeSubscriber(this.identity);
                        respondSuccess(this);
                    } else {
                        // Respond with error if channel is invalid
                        respondError(this, "No Such Channel: " + requestedChannel);
                    }
                } else if (requestType.equalsIgnoreCase("GetRequest")) {
                    long after = (long) request.get("after");
                    respondMessageList(this, getSentMessages(this, after));
                }
            }
            close();
        }

        private void close() {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
            }
        }

        // Method to send message from server to client
        public boolean sendResponse(JSONObject response) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                outputStream.writeObject(response);
            } catch (IOException e) {}
            return true;
        }
    }
    public static void main(String arg[]){
        new Server(8000).startServer();
    }
}