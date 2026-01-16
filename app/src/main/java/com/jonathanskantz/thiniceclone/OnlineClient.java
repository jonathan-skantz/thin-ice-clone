package com.jonathanskantz.thiniceclone;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

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

    private static Thread threadConnect;

    public static void connect(InetSocketAddress address) {
        
        if (threadConnect != null && threadConnect.isAlive()) {
            threadConnect.interrupt();
            System.out.println("CLIENT: overriding current connection attempt");
        }

        threadConnect = new Thread(() -> {

            tryReconnecting = true;
            onStartSearch.run();

            while (true) {
                try {
                    System.out.println("CLIENT: trying to connect to " + address);
                    socket = new Socket(address.getAddress(), address.getPort());
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
                    
                    if (!tryReconnecting) {
                        System.out.println("CLIENT: cancelled connection attempt");
                        break;
                    }

                    try {
                        System.out.println("CLIENT: still trying to connect to " + address);
                        Thread.sleep(RECONNECT_DELAY);
                    }
                    catch (InterruptedException e1) {
                        // interrupted because connect attempt was cancelled by user, then initiated again
                        break;
                    }
                }
                catch (UnknownHostException e) {
                    System.out.println("CLIENT error: host \"" + address.getAddress() + "\" cannot be found");
                    break;
                }
                catch (IOException e) {
                    System.out.println("CLIENT error: couldn't create socket");
                    break;
                }
            }
            
            System.out.println("CLIENT: thread finished");

        });

        threadConnect.start();

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

        while (serverOut == null) {}  // wait for thread to start

        try {
            System.out.println("CLIENT: sending " + obj);
            serverOut.writeObject(obj);
        }
        catch (IOException e) {
            System.out.println("CLIENT error: couldn't send " + obj);
            e.printStackTrace();
        }

    }

    private static void listen() {

        try {
            serverOut = new ObjectOutputStream(socket.getOutputStream());
            clientIn = new ObjectInputStream(socket.getInputStream());
        }
        catch (IOException e) {
            System.out.println("CLIENT error: couldn't get input and output stream from socket");
            return;
        }

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
