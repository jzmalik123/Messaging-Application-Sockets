
import org.json.simple.JSONObject;

import java.net.*;
import java.io.*;
import java.util.*;

//Client class to run a client and connect it with the server on port 8000
public class Client {

    private final String indentation = " ";
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket socket;

    private final String server;
    private String identity;
    private final int portNumber;

    Client(String server, int portNumber, String identity) {
        this.server = server;
        this.portNumber = portNumber;
        this.identity = identity;
    }

    // Method to start the client on the server with the port number
    public boolean start() {
        try {
            socket = new Socket(server, portNumber);
        } catch (IOException ec) {
            return false;
        }

        String msg = "Connected " + socket.getInetAddress() + ":" + socket.getPort();
        printLine(msg);

        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            printLine("Input/output Stream Exception: " + eIO);
            return false;
        }

        // Thread to serve client and wait for the messages or notifications
        new Thread(){
            @Override
            public void run(){
                serve();
            }
        }.start();
        try {
            outputStream.writeObject(identity);
        } catch (IOException eIO) {
            printLine("Login Exception: " + eIO);
            disconnect();
            return false;
        }
        return true;
    }

    // Serve method that's running in the thread
    private void serve() {
        while (true) {
            try {
                JSONObject msg = (JSONObject) inputStream.readObject();
                System.out.println(msg.toJSONString());
            } catch (IOException e) {
                printLine(indentation + "Server has closed the connection: " + e + indentation);
                break;
            } catch (ClassNotFoundException e2) {
            }
        }
    }

    private void printLine(String msg) {
        System.out.println(msg);
    }

    // Method to disconnect from the server
    private void disconnect() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Client diconnected");
        }

    }

    // openRequest For First Time

    public void openRequest(){
        JSONObject request = new JSONObject();

        request.put("_class", "OpenRequest");
        request.put("identity", this.identity);
        try {
            outputStream.writeObject(request);
        } catch (IOException e) {
            printLine("Exception writing to server: " + e);
        }
    }

    // Publish Request to send Messages
    public void publishRequest(String body, String from, String pic){
        JSONObject message = new JSONObject();
        message.put("_class", "Message");
        message.put("from", from);
        message.put("body", body);
        message.put("pic", pic);
        message.put("when", System.currentTimeMillis());

        JSONObject request = new JSONObject();
        request.put("_class", "PublishRequest");
        request.put("identity", this.identity);
        request.put("message", message);

        try {
            outputStream.writeObject(request);
        } catch (IOException e) {
            printLine("Exception writing to server: " + e);
        }
    }

    // Subscribe Request to subscribe to channel
    public void subscribeRequest(String channel){
        JSONObject request = new JSONObject();
        request.put("_class", "SubscribeRequest");
        request.put("identity", this.identity);
        request.put("channel", channel);

        try {
            outputStream.writeObject(request);
        } catch (IOException e) {
            printLine("Exception writing to server: " + e);
        }
    }

    // Unsubscribe request to unsubscribe from channel
    public void unsubscribeRequest(String channel){
        JSONObject request = new JSONObject();
        request.put("_class", "UnsubscribeRequest");
        request.put("identity", this.identity);
        request.put("channel", channel);

        try {
            outputStream.writeObject(request);
        } catch (IOException e) {
            printLine("Exception writing to server: " + e);
        }
    }

    // getRequest to get MessageList
    public void getRequest(long after){
        JSONObject request = new JSONObject();
        request.put("_class", "GetRequest");
        request.put("identity", this.identity);
        request.put("after", after);

        try {
            outputStream.writeObject(request);
        } catch (IOException e) {
            printLine("Exception writing to server: " + e);
        }
    }

    // Main method to run the client
    public static void main(String[] args) throws IOException {
        int portNumber = 8000;
        String serverAddress = "localhost";
        String userName;
        Client client;
        try (Scanner scan = new Scanner(System.in)) {
            System.out.println("Enter the identity: ");
            userName = scan.nextLine();
            client = new Client(serverAddress, portNumber, userName);
            if (!client.start()) {
                client.start();
            }

            client.openRequest();

            System.out.println("********** Messaging Application **********\n");
            System.out.println("Follow the given Instructions:\n");


            // Loop to accept input from user continously
            while (true) {

                System.out.println("1. Publish");
                System.out.println("2. Subscribe");
                System.out.println("3. Unsubscribe");
                System.out.println("4. Get Messages");
                System.out.println("5. Quit");
                System.out.println("Enter choice 1-5");

                System.out.print("> ");
                int choice = scan.nextInt();
                scan.nextLine();

                if(choice == 1){
                    System.out.println("Enter Sender Name:");
                    System.out.print("> ");
                    String from = scan.nextLine();
                    System.out.println("Enter Message Body:");
                    System.out.print("> ");
                    String body = scan.nextLine();
                    System.out.println("Enter Message Pic:");
                    System.out.print("> ");
                    String pic = scan.nextLine();
                    client.publishRequest(body, from, pic);
                } else if (choice == 2) {
                    System.out.println("Enter Channel Name:");
                    System.out.print("> ");
                    String channel = scan.nextLine();
                    client.subscribeRequest(channel);
                } else if (choice == 3) {
                    System.out.println("Enter Channel Name:");
                    System.out.print("> ");
                    String channel = scan.nextLine();
                    client.unsubscribeRequest(channel);
                } else if (choice == 4) {
                    System.out.println("Enter After Timestamp:");
                    System.out.print("> ");
                    int after = scan.nextInt();
                    scan.nextLine();
                    client.getRequest(after);
                } else if (choice == 5) {
                    break;
                }
            }

        }
        client.disconnect();
    }
}
