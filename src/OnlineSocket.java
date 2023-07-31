import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

public class OnlineSocket {

    // NOTE: either socket or server should be used, not both
    private static Socket socket;
    private static ServerSocket server;

    private static boolean handledReceived = true;       // prevents sending back same event to opponent
    
    private static final int RECONNECT_DELAY = 1000;    // in ms

    // host server
    public static void host(int port) {
        
        new Thread(() -> {
            
            try {
                server = new ServerSocket(port);
                System.out.println("SERVER: created, listening on port " + port);
                // TODO: prevent two hosts on same port

                while (true) {
                    System.out.println("SERVER: waiting for user");
                    socket = server.accept();   // NOTE: saves the opponent as `socket`
                    System.out.println("SERVER: user " + socket + " connected");
                    
                    listen();
                    // continues here when opponent disconnects --> loop, wait for new user
                }
            }
            catch (IOException e) {
                System.out.println("SERVER: closed (and set to null)");
                server = null;
                socket = null;
            }
    
            System.out.println("SERVER: end");
        }).start();
    }

   // join server
    public static void join(int port) {
        
        new Thread(() -> {
            
            while (true) {
                try {
                    socket = new Socket("localhost", port);
                    System.out.println("CLIENT: connected as " + socket);
                    
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
                        
                        if (!Config.multiplayerOnline) {
                            System.out.println("CLIENT: cancelled connection attempt (in UI)");
                            break;
                        }
                    }
                    catch (InterruptedException e1) {
                        e1.printStackTrace();
                        break;
                    }
                }
                catch (IOException e) {
                    System.out.println("CLIENT: disconnected (socket broken, set to null)");
                    socket = null;
                    break;
                }
            }
            
            // TODO: show that host right maze is offline
            System.out.println("CLIENT: end");
            

        }).start();

    }

    // disconnect user or close server
    public static void disconnect() {
        
        if (server != null) {

            try {
                server.close();
                socket.close();
                System.out.println("SERVER: closed (manually)");
            }
            catch (IOException e) {
                System.out.println("SERVER: error when closing:");
            }
        }

        else if (socket != null) {
            try {
                socket.close();
                System.out.println("CLIENT: disconnected (manually)");
            } catch (IOException e) {
                System.out.println("CLIENT: error when disconnecting:");
                e.printStackTrace();
            }
        }

    }

    public static void send(Object obj) {
        
        if (!(Config.multiplayerOnline && handledReceived)) {
            return;
        }

        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(obj);
        }
        catch (IOException e) {
            System.out.println("CLIENT/SERVER: couldn't send due to:");
            e.printStackTrace();
        }

    }

    private static void listen() {

        while (true) {

            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Object received = in.readObject();

                handledReceived = false;
                
                System.out.println("received: " + received);
                if (received.getClass() == Maze.Direction.class) {
                    Main.mazeRight.tryToMove((Maze.Direction)received);
                }
                else if (received.getClass() == Maze.class) {
                    Main.setMaze((Maze)received);
                }
                else if (received.getClass() == KeyHandler.Action.class) {
                    KeyHandler.Action casted = (KeyHandler.Action) received;
                    casted.callback.run();
                }
                // TODO: handle new maze config
                handledReceived = true;
                
            } catch (ClassNotFoundException e) {
                System.out.println("LISTEN: reading error:");
                e.printStackTrace();
                break;
            } catch (IOException e) {
                if (server == null) {
                    System.out.println("CLIENT: disconnected (LISTEN stopped)");
                }
                else {
                    System.out.println("SERVER: client disconnected (LISTEN stopped)");
                }
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                break;
            }
        }

    }



}
