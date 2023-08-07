import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class OnlineServer {
    
    public static boolean opened;
    public static boolean clientConnected;
    public static boolean handledReceived = true;
    public static boolean kickCallsDisconnect = true;

    private static ServerSocket serverSocket;     // this user
    private static Socket clientSocket;       // other user
    private static ObjectOutputStream clientOut;
    private static ObjectInputStream serverIn;

    public static Runnable onOpen = () -> { System.out.println("SERVER: (default callback) server opened"); };
    public static Runnable onClose = () -> { System.out.println("SERVER: (default callback) server closed"); };
    public static Runnable onReceived = () -> {};

    public static Runnable onClientConnect = () -> { System.out.println("SERVER: (default callback) client connected"); };
    public static Runnable onClientDisconnect = () -> { System.out.println("SERVER: (default callback) client disconnected"); };
    public static Runnable onClientKick = () -> { System.out.println("SERVER: (default callback) client kicked"); };

    public static Object receivedObject;

    public static final String LOCAL_IP;

    static {
        String localIP;
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            localIP = null;
            e.printStackTrace();
        }
        LOCAL_IP = localIP;
    }

    public static void open(int port) {

        new Thread(() -> {
            
            try {
                // TODO: prevent two hosts on same port
                serverSocket = new ServerSocket(port);
                opened = true;
                // new Thread(onOpen).start();
                onOpen.run();

                while (true) {
                    System.out.println("SERVER: waiting for user to connect");
                    clientSocket = serverSocket.accept();
                    clientConnected = true;
                    
                    // new thread since onClientConnect may try to send
                    new Thread(onClientConnect).start();
                    listen();

                    // continues here when opponent disconnects --> loop, wait for new user
                }
            }
            catch (IOException e) {
                
                if (opened) {
                    opened = false;
                    System.out.println("SERVER error: couldn't open");
                    e.printStackTrace();

                    // new Thread(onClose).start();
                    onClose.run();
                }
                // else: was closed manually
            }
    
            System.out.println("SERVER: end");

        }).start();
    }

    public static void close() {
        try {
            serverSocket.close();
            opened = false;
            // new Thread(onClose).start();
            onClose.run();

            if (clientConnected) {
                clientSocket.close();

                clientConnected = false;
                // kick client

                if (kickCallsDisconnect) {
                    // new Thread(onClientDisconnect).start();
                    onClientDisconnect.run();
                }
                else {
                    // new Thread(onClientKick).start();
                    onClientKick.run();
                }
            }
        }
        catch (IOException e) {
            System.out.println("SERVER error: couldn't close");
            e.printStackTrace();
        }
    }

    public static void send(Object obj) {
        
        if (!clientConnected) { 
            System.out.println("SERVER: offline, not sending " + obj);
            return;
        }

        try {
            while (clientOut == null) {
                System.out.println("SERVER: waiting for clientOut to be created...");
                // TODO: remove print
            }  // wait for thread to start

            System.out.println("SERVER: sending " + obj);
            clientOut.writeObject(obj);
        }
        catch (IOException e) {
            System.out.println("SERVER error: couldn't send " + obj);
            e.printStackTrace();
        }

    }

    private static void listen() throws IOException {

        clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
        serverIn = new ObjectInputStream(clientSocket.getInputStream());

        while (true) {

            try {
                System.out.println("SERVER: waiting for object...");
                receivedObject = serverIn.readObject();
                System.out.println("SERVER: received " + receivedObject);
                handledReceived = false;
                // new Thread(() -> {
                    onReceived.run();
                handledReceived = true;
                // }).start();           // TODO: try delay, then receive new, to see what receivedObject refers to
            }
            catch (ClassNotFoundException e) {
                System.out.println("SERVER error: couldn't read object " + receivedObject);
                e.printStackTrace();
            }
            catch (IOException e) {

                if (clientConnected) {

                    clientSocket.close();
                    clientConnected = false;
                    
                    if (kickCallsDisconnect) {
                        onClientDisconnect.run();
                    }
                    else {
                        onClientKick.run();
                    }
                }
                // else: client disconnect manually

                break;
            }
        }
    }

}
