
import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

//Client class to run a client and connect it with the server on port 8000
public class Client {

    private final String indentation = " ";
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket socket;

    private final String server;
    private String userName;
    private final int portNumber;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    Client(String server, int portNumber, String userName) {
        this.server = server;
        this.portNumber = portNumber;
        this.userName = userName;
    }

//    method to start the client on the server with the port number
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

//        thread to serve client and wait for the messages or notifications
        new Thread(){
            @Override
            public void run(){
                serve();
            }
        }.start();
        try {
            outputStream.writeObject(userName);
        } catch (IOException eIO) {
            printLine("Login Exception: " + eIO);
            disconnect();
            return false;
        }
        return true;
    }

//    serve method that's running in the thread
    private void serve() {
        while (true) {
            try {
                Message msg = (Message) inputStream.readObject();
                switch (msg.getType()) {
                    case MESSAGE:
                        System.out.println(msg.getText());
                        System.out.print("> ");
                        break;
                    case SENDFILE:
                        String home = System.getProperty("user.home");
                        System.out.println(Paths.get(home + "/Downloads/" + msg.getFile().getName()));
                        Files.write(Paths.get(home + "/Downloads/" + msg.getFile().getName()), msg.getFileContent());
                        System.out.println("Successfully sent.");
                        System.out.print("> ");
                        break;
                }
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
//    method used to send message using string
    void sendMessage(Message msg) {
        try {
            outputStream.writeObject(msg);
        } catch (IOException e) {
            printLine("Exception writing to server: " + e);
        }
    }
//    method to disconnect from the server
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

//    method to send public and private file to any user
    public void sendFile(Message msg) throws IOException {
        System.out.println("* For Public file transfer, Enter file name with path");
        System.out.println("* For private file transfer, Type '@username<space>filepath' without quotes");
        System.out.print("> ");
        Scanner scan = new Scanner(System.in);
        String st = scan.nextLine();
        File file;
        if (st.split(" ").length == 2) {
            file = new File(st.split(" ")[1]);
        } else {
            file = new File(st);
        }
        byte[] buffer = Files.readAllBytes(file.toPath());
        outputStream.writeObject(new Message(msg.getType(), st, buffer, file, "", ""));
    }

//    main method to initiate the program
    public static void main(String[] args) throws IOException {
        int portNumber = 8000;
        String serverAddress = "localhost";
        String userName;
        Client client;
        try (Scanner scan = new Scanner(System.in)) {
            System.out.println("Enter the username: ");
            userName = scan.nextLine();
            switch (args.length) {
                case 3:
                    try {
                        userName = args[0];
                        portNumber = Integer.parseInt(args[1]);
                        serverAddress = args[2];
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid port number.");
                        System.out.println("Usage is: > java client [username] [portNumber] [serverAddress]");
                        return;
                    }
                    break;
                case 2:
                    try {
                        userName = args[0];
                        portNumber = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid port number.");
                        System.out.println("Usage is: > java client [username] [portNumber] [serverAddress]");
                        return;
                    }
                    break;
                case 1:
                    userName = args[0];
                    break;
                case 0:
                    if(userName.isEmpty())
                        userName = "Unknown";
                    break;
                default:
                    System.out.println("Usage is: > java client [username] [portNumber] [serverAddress]");
                    return;
            }
//            client object to create and start client
            client = new Client(serverAddress, portNumber, userName);
            if (!client.start()) {
                client.start();
            }
//            menu to show the user
            System.out.println("Messaging Application");
            System.out.println("Follow the given Instructions:");
            System.out.println("1. Type Message for public message");
            System.out.println("2. Type '@username<space>yourmessage' without quotes for private message");
            System.out.println("3. Type 'PUBLISH<space>channelname<space>yourmessage' without quotes to publish a message to a channel");
            System.out.println("3. Type 'SHOW<space>channelname' without quotes to show all messages of a channel");
            System.out.println("4. Type 'SUBSCRIBE<space>channelname' without quotes to subscribe a channel");
            System.out.println("5. Type 'UNSUBSCRIBE<space>channelname' without quotes to subscribe a channel");
            System.out.println("6. Type 'ACTIVECLIENTS' without quotes to check active peers");
            System.out.println("7. Type 'CHANNELLIST' without quotes to send file");
            System.out.println("8. Type 'SENDFILE' without quotes to send file");
            System.out.println("9. Type 'LOGOUT' without quotes to logout");
//            loop to accept input from user continously
            while (true) {
                System.out.print("> ");
                String message = scan.nextLine();
                if (message.equalsIgnoreCase("LOGOUT")) {
                    client.sendMessage(new Message(MessageType.LOGOUT, "", null, null, "", ""));
                    break;
                } else if (message.equalsIgnoreCase("CHANNELLIST")) {
                    client.sendMessage(new Message(MessageType.CHANNELLIST, "", null, null, "", ""));
                } else if (message.split(" ", 3)[0].trim().equalsIgnoreCase("PUBLISH")) {
                    client.sendMessage(new Message(MessageType.PUBLISH, message, null, null, userName, ""));
                } else if (message.split(" ", 2)[0].trim().equalsIgnoreCase("SHOW")) {
                    client.sendMessage(new Message(MessageType.SUBSCRIBE, message, null, null, "", ""));
                } else if (message.split(" ", 2)[0].trim().equalsIgnoreCase("SUBSCRIBE")) {
                    client.sendMessage(new Message(MessageType.SUBSCRIBE, message, null, null, "", ""));
                } else if (message.split(" ", 2)[0].trim().equalsIgnoreCase("UNSUBSCRIBE")) {
                    client.sendMessage(new Message(MessageType.UNSUBSCRIBE, message, null, null, "", ""));
                } else if (message.equalsIgnoreCase("ACTIVECLIENTS")) {
                    client.sendMessage(new Message(MessageType.ACTIVECLIENTS, "", null, null, "", ""));
                } else if (message.equalsIgnoreCase("SENDFILE")) {
                    client.sendFile(new Message(MessageType.SENDFILE, "", null, null, "", ""));
                } else if (!message.isEmpty()) {
                    client.sendMessage(new Message(MessageType.MESSAGE, message, null, null, "", ""));
                }
            }
        }
        client.disconnect();
    }
}
