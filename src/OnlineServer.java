import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

public class OnlineServer {

    private static final Random rand = new Random();    // used to find random port

    public static boolean opened;
    public static int port = -1;
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

    public static final int PORT_MIN = 49152;
    public static final int PORT_MAX = 65535;

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

    // port is chosen randomly
    public static boolean open() {
        int port = OnlineServer.PORT_MIN + rand.nextInt(OnlineServer.PORT_MAX + 1 - OnlineServer.PORT_MIN);

        int breakWhenReached = port;

        while (!open(port)) {
            
            if (port == OnlineServer.PORT_MAX) {
                port = OnlineServer.PORT_MIN;
            }
            else {
                if (++port == breakWhenReached) {
                    System.out.println("SERVER: all ports are busy, cannot open");
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean open(int port) {

        try {
            serverSocket = new ServerSocket(port);
            OnlineServer.port = port;
            opened = true;
            onOpen.run();
        }
        catch (BindException e) {
            System.out.println("SERVER: port " + port + " already in use");
            return false;
        }
        catch (IOException e) {
            System.out.println("SERVER error: couldn't create socket");
            e.printStackTrace();
        }

        new Thread(() -> {

            while (true) {
                System.out.println("SERVER: waiting for user to connect");
                try {
                    clientSocket = serverSocket.accept();
                    clientConnected = true;
                    // new thread since onClientConnect may try to send
                    new Thread(onClientConnect).start();
                    listen();
                }
                catch (IOException e) {
                    System.out.println("SERVER: closed");
                    break;
                }

                // continues here when opponent disconnects --> loop, wait for new user
            }
    
            System.out.println("SERVER: thread finished");
            
        }).start();

        return true;    // server successfully started
    }

    public static void close() {
        
        if (!opened) {
            return;
        }

        try {
            serverSocket.close();
            opened = false;
            onClose.run();

            if (clientConnected) {
                clientSocket.close();

                clientConnected = false;
                // kick client

                if (kickCallsDisconnect) {
                    onClientDisconnect.run();
                }
                else {
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

        while (clientOut == null) {}  // wait for thread to start
        
        try {
            System.out.println("SERVER: sending " + obj);
            clientOut.writeObject(obj);
        }
        catch (IOException e) {
            System.out.println("SERVER error: couldn't send " + obj);
            e.printStackTrace();
        }

    }

    private static void listen() {

        try {
            clientOut = new ObjectOutputStream(clientSocket.getOutputStream());
            serverIn = new ObjectInputStream(clientSocket.getInputStream());
        }
        catch (IOException e) {
            System.out.println("SERVER error: couldn't get input and output stream from socket");
            return;
        }

        while (true) {

            try {
                System.out.println("SERVER: waiting for object...");
                receivedObject = serverIn.readObject();
                System.out.println("SERVER: received " + receivedObject);
                handledReceived = false;
                onReceived.run();
                handledReceived = true;
            }
            catch (ClassNotFoundException e) {
                System.out.println("SERVER error: couldn't read object " + receivedObject);
                e.printStackTrace();
            }
            catch (IOException e) {

                if (clientConnected) {

                    try {
                        clientSocket.close();
                        clientConnected = false;
                        
                        if (kickCallsDisconnect) {
                            onClientDisconnect.run();
                        }
                        else {
                            onClientKick.run();
                        }
                    }
                    catch (IOException e1) {
                        System.out.println("CLIENT error: couldn't disconnect");
                        e1.printStackTrace();
                    }
                    
                }
                // else: client disconnect manually

                break;
            }
        }
    }

}
