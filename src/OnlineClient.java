import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;

public class OnlineClient {

    public static boolean connected;
    public static boolean handledReceived = true;       // prevents sending back same event to opponent
    public static boolean kickCallsDisconnect = true;

    private static Socket socket;
    private static ObjectOutputStream serverOut;
    private static ObjectInputStream clientIn;
    
    private static final int RECONNECT_DELAY = 1000;    // in ms
    public static boolean tryReconnecting = true;

    public static Runnable onConnect = () -> { System.out.println("CLIENT: (default callback) connected as " + socket); };
    public static Runnable onDisconnect = () -> { System.out.println("CLIENT: (default callback) disconnected"); };
    public static Runnable onReceived = () -> {};
    public static Runnable onKick = () -> { System.out.println("CLIENT: (default callback) kicked"); };
    public static Runnable onStartSearch = () -> {};
    public static Runnable onStopSearch = () -> {};     // manually stopped, not connection established

    public static Object receivedObject;

    public static void connect(int port) {
        
        new Thread(() -> {
            
            tryReconnecting = true;
            onStartSearch.run();

            while (true) {
                try {
                    socket = new Socket("localhost", port);
                    connected = true;

                    new Thread(onConnect).start();
                    listen();

                    if (socket.isClosed()) {
                        System.out.println("CLIENT: stopped searching for server");
                        break;
                    }
                    // else: server was closed, but client is still searching --> continue
                }
                catch (ConnectException e) {
                    
                    try {
                        System.out.println("CLIENT: attempting to connect to " + port);
                        Thread.sleep(RECONNECT_DELAY);
                        
                        if (!tryReconnecting) {
                            System.out.println("CLIENT: cancelled connection attempt");
                            break;
                        }
                    }
                    catch (InterruptedException e1) {
                        e1.printStackTrace();
                        break;
                    }
                }
                catch (IOException e) {
                    System.out.println("CLIENT error: couldn't create socket");
                    e.printStackTrace();
                    break;
                }
            }
            
            // TODO: show that host right maze is offline
            System.out.println("CLIENT: end");

        }).start();

    }

    public static void disconnect() {
        
        tryReconnecting = false;    // cancels connection attempts if not yet connected

        if (connected) {
            try {
                socket.close();
                connected = false;
                onDisconnect.run();
            }
            catch (IOException e) {
                System.out.println("CLIENT error: couldn't disconnect");
                e.printStackTrace();
            }
        }
        else {
            onStopSearch.run();
        }

    }

    public static void send(Object obj) {

        if (!connected) {
            System.out.println("CLIENT: offline, not sending " + obj);
            return;
        }

        try {
            while (serverOut == null) {
                System.out.println("CLIENT: waiting for serverOut to be created...");
                // TODO: remove print
            }  // wait for thread to start

            System.out.println("CLIENT: sending " + obj);
            serverOut.writeObject(obj);
        }
        catch (IOException e) {
            System.out.println("CLIENT error: couldn't send " + obj);
            e.printStackTrace();
        }

    }

    private static void listen() throws IOException {

        serverOut = new ObjectOutputStream(socket.getOutputStream());
        clientIn = new ObjectInputStream(socket.getInputStream());

        while (true) {

            try {
                System.out.println("CLIENT: waiting for object...");
                receivedObject = clientIn.readObject();
                System.out.println("CLIENT: received " + receivedObject);
                handledReceived = false;
                onReceived.run();
                handledReceived = true;
            }
            catch (ClassNotFoundException e) {
                System.out.println("CLIENT: couldn't read object " + receivedObject);
                e.printStackTrace();
            }
            catch (IOException e) {
                
                if (connected) {
                    connected = false;

                    if (kickCallsDisconnect) {
                        onDisconnect.run();
                    }
                    else {
                        onKick.run();
                    }
                }
                // else: disconnected manually
                
                break;
            }
        }

    }

}
