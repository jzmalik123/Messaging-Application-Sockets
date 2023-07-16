
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

//Server class to start a server and to wait and accept clients
//Server will create threads for all client with there sockets and send message to each user using those sockets
public class Server {

    private static int cliendId = 0;
    private final ArrayList<ClientThread> threadList;
    private final SimpleDateFormat dateFormat;
    private final int portNumber;
    private boolean flag;
    private final String indentation = " ";
    private final ArrayList<Channel> channels;
//    constructor used to create channel list and initialize variables
    public Server(int port) {
        this.portNumber = port;
        dateFormat = new SimpleDateFormat("HH:mm:ss");
        threadList = new ArrayList<>();
        channels = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            channels.add(new Channel(String.valueOf(i), "Test" + i, new ArrayList<>()));            
        }
    }
//    method to start the server on the given port number
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

//    method used to publish messages to a given channel
    private synchronized void publishMesssage(String msg, String channel){
        Channel ch = null;
        for (Channel c : channels) {
            if (c.getChannelName().equalsIgnoreCase(channel)) {
                ch = c;
            }
        }
        if(ch != null){
            for (ClientThread ct : threadList) {
                if(ch.getUsers().contains(ct.userName)){
                    ct.sendObject(msg);
                }
            }
        }
    }
//    method used to send public and private messages to the user
    private synchronized boolean sendMessage(Message message, String text) {
        String t = dateFormat.format(new Date());

        String[] split = text.split(" ", 3);
        boolean isPrivate = false;
        if (split[1].charAt(0) == '@') {
            isPrivate = true;
        }

        if (isPrivate == true) {
            String userName = split[1].substring(1, split[1].length());

            text = split[0] + split[2];
            String msg = t + " " + text + "\n";
            boolean found = false;
            for (int i = threadList.size(); --i >= 0;) {
                ClientThread client = threadList.get(i);
                if (client.getUserName().equals(userName)) {
                    if (message.getType() == MessageType.MESSAGE) {
                        if (!client.sendObject(msg)) {
                            threadList.remove(i);
                        }
                    } else if (message.getType() == MessageType.SENDFILE) {
                        if (!client.sendFile(message.getFile(), message.getFileContent())) {
                            threadList.remove(i);
                        }
                    }
                    found = true;
                    break;
                }

            }
            if (found != true) {
                return false;
            }
        } else {
            String msg = t + " " + text + "\n";

            if (message.getType() == MessageType.MESSAGE) {
                for (int i = threadList.size(); --i >= 0;) {
                    ClientThread client = threadList.get(i);
                    if (!client.sendObject(msg)) {
                        threadList.remove(i);
                    }
                }
            } else if (message.getType() == MessageType.SENDFILE) {
                for (int i = threadList.size(); --i >= 0;) {
                    ClientThread client = threadList.get(i);
                    if (!client.sendFile(message.getFile(), message.getFileContent())) {
                        threadList.remove(i);
                    }
                }
            }
        }
        return true;

    }
//    message used to remove client by id. So, it will remove thread from thread list
    synchronized void removeClientById(int id) {

        String disconnectedClient = "";
        for (int i = 0; i < threadList.size(); ++i) {
            ClientThread ct = threadList.get(i);
            if (ct.id == id) {
                disconnectedClient = ct.getUserName();
                threadList.remove(i);
                break;
            }
        }
        sendMessage(new Message(MessageType.MESSAGE, "", null, null, "", ""), indentation + disconnectedClient + " has left the chat room." + indentation);
    }

//    thread client class to create a thread of each client on server to receive messages 
//    and give response to others accordingly
    class ClientThread extends Thread {

        Socket socket;
        ObjectInputStream inputStream;
        ObjectOutputStream outputStream;
        int id;
        String clientIP;
        String userName;
        Message message;
        String date;
//        constructor to create client profile on server
        ClientThread(Socket socket) {
            id = ++cliendId;
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
                userName = (String) inputStream.readObject();
                sendMessage(new Message(MessageType.MESSAGE, "", null, null, "", ""), indentation + userName + " has joined the chat room." + indentation);
            } catch (IOException e) {
                return;
            } catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

//        thread class methoded overrided here and it will run when thread will be started
        @Override
        public void run() {
            boolean flag = true;
            while (flag) {
//                listen for the messages from the client
                try {
                    message = (Message) inputStream.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    break;
                }
                String text = this.message.getText();

                switch (this.message.getType()) {
//                    if the client send a text message to all or to specific cleint
                    case MESSAGE:
                        boolean confirmation = sendMessage(this.message, userName + ": " + text);
                        if (confirmation == false) {
                            String msg = indentation + "No such user exists." + indentation;
                            sendObject(msg);
                        }
                        break;
//                        if client send text message to a channel
                    case PUBLISH:
                        String[] arr = text.split(" ", 3);
                        for (Channel channel : channels) {
                            if(channel.getChannelName().equalsIgnoreCase(arr[1])){
                                channel.getMessages().add(new Message(MessageType.MESSAGE, arr[2],null,null,message.getFrom(), dateFormat.format(new Date())));
                                publishMesssage(arr[2], arr[1]);
                                break;
                            }
                        }
                        break;
//                        if cleint request to view his published messages in a channel
                    case SHOW:
                        String[] msgs = text.split(" ", 2);
                        for (Channel channel : channels) {
                            if(channel.getChannelName().equalsIgnoreCase(msgs[1])){
                                for (Message m : channel.getMessages()) {
                                    if(m.getFrom().equalsIgnoreCase(userName)){
                                        sendObject(m.getText());
                                    }
                                }
                            }
                        }
                        break;
//                        if client send a file to all or a specific user
                    case SENDFILE: {
                        confirmation = sendMessage(this.message, userName + ": " + text);
                        if (confirmation == false) {
                            String msg = indentation + "No such user exists." + indentation;
                            sendObject(msg);
                        }
                        break;
                    }
//                    if client subsribe a channel
                    case SUBSCRIBE:
                        String channelName = text.split(" ", 2)[1];
                        for (Channel channel : channels) {
                            if(channel.getChannelName().equalsIgnoreCase(channelName)){
                                if(channel.getUsers().contains(this.getUserName())){
                                    sendObject("You already Subscribed this channel");
                                }else{
                                    channel.getUsers().add(this.getUserName());
                                    sendObject("Channel Subscribed successfully");                                    
                                }
                                break;
                            }
                        }
                        break;
//                        if cleint unsubscribe a channel
                    case UNSUBSCRIBE:
                        channelName = text.split(" ", 2)[1];
                        for (Channel channel : channels) {
                            if(channel.getChannelName().equalsIgnoreCase(channelName)){
                                if(channel.getUsers().contains(this.getUserName())){
                                    channel.getUsers().add(this.getUserName());
                                    sendObject("Channel Unsubscribed successfully");                                    
                                }else{
                                    sendObject("You have not Subscription of this channel");
                                }
                            }
                            break;
                        }
                        break;
//                        if cleint logout from the server
                    case LOGOUT:
                        flag = false;
                        break;
//                        if client request to view all channel list
                    case CHANNELLIST:
                        sendObject("List of the Available Channels " + "\n");
                        for (Channel c : channels) {
                            sendObject(c.getChannelId() + "- Channel Name: " + c.getChannelName() + " Subscriptions " + c.getUsers().size());
                        }
                        break;
//                        if client request to view all client list
                    case ACTIVECLIENTS:
                        sendObject("List of the users connected at " + dateFormat.format(new Date()) + "\n");
                        for (int i = 0; i < threadList.size(); ++i) {
                            ClientThread ct = threadList.get(i);
                            sendObject(ct.clientIP + " Username: " + ct.userName + " since " + ct.date);
                        }
                        break;
                }
            }
            removeClientById(id);
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

//        method to send message from server to client
        private boolean sendObject(String msg) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                outputStream.writeObject(new Message(MessageType.MESSAGE, msg, null, null, "", ""));
            } catch (IOException e) {
            }
            return true;
        }
//        method to send file to the user
        private boolean sendFile(File file, byte[] content) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                outputStream.writeObject(new Message(MessageType.SENDFILE, "", content, file, "", ""));
            } // if an error occurs, do not abort just inform the user // if an error occurs, do not abort just inform the user
            catch (IOException e) {
            }
            return true;
        }
    }
    public static void main(String arg[]){
        new Server(8000).startServer();
    }
}
