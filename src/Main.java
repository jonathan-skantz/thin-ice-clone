import javax.swing.JLabel;
import java.awt.Font;

/**
 * Main class for the game.
 */
public class Main {

    private static Maze maze;   // will be copied into mazeLeft (and potentially mazeRight)

    public static MazeContainer mazeLeft;
    public static MazeContainer mazeRight;

    public static JLabel textStatus;
    public static final boolean ENABLE_ANIMATIONS = true;

    public static Font font = new Font("arial", Font.PLAIN, 20);
    public static Font hintFont = new Font("verdana", Font.BOLD, (int)(0.5*Config.blockSize));

    public static volatile boolean mazeGenThreadDone = true;
    public static TimedCounter tcCountdown;

    public static void main(String[] args) {

        Config.applyDefault();
        Window.setup();

        setupKeyCallbacks();
        setupCountdownTimer();

        Window.sprites.setVisible(false);   // prevents redrawing while setting up
        
        textStatus = new JLabel("Generating...");
        textStatus.setFont(font);
        textStatus.setSize(textStatus.getPreferredSize());
        textStatus.setLocation(Window.getXCentered(textStatus), 50);
        textStatus.setVisible(false);
        Window.sprites.add(textStatus);

        UI.setupConfigs();

        // make first maze consist of walls only
        maze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        mazeLeft = new MazeContainer(0);
        mazeRight = new MazeContainer(Window.mazeWidth);
        Window.sprites.setVisible(true);

        OnlineServer.onClientConnect = () -> {
            updateMultiplayer();
            mazeLeft.setMaze(maze);     // resets current maze and its graphics
            mazeRight.setMaze(maze);
            OnlineServer.send(maze);    // server sends its maze
        };

        OnlineServer.onClientDisconnect = () -> { 
            // not handled by UI since server is still open
            updateMultiplayer();
        };
        
        OnlineClient.onConnect = () -> { 
            updateMultiplayer();
            // maze is set in OnlineClient.listen() since OnlineServer sends it when client connects
        };
        OnlineClient.onDisconnect = OnlineServer.onClientDisconnect;

        OnlineServer.onReceived = () -> {
            Object received = OnlineServer.receivedObject;
            handleReceived(received);
        };

        OnlineClient.onReceived = () -> {
            Object received = OnlineClient.receivedObject;
            handleReceived(received);
        };

    }

    private static void handleReceived(Object obj) {
        
        if (obj.getClass() == Maze.Direction.class) {
            Main.mazeRight.tryToMove((Maze.Direction)obj);
        }
        else if (obj.getClass() == Maze.class) {
            Main.setMaze((Maze)obj);
        }
        else if (obj.getClass() == KeyHandler.Action.class) {
            KeyHandler.Action casted = (KeyHandler.Action) obj;

            if (mazeRight.isMirrored) {

                if (casted == KeyHandler.Action.P2_MOVE_LEFT) {
                    casted = KeyHandler.Action.P2_MOVE_RIGHT;
                }
                else if (casted == KeyHandler.Action.P2_MOVE_RIGHT) {
                    casted = KeyHandler.Action.P2_MOVE_LEFT;
                }
            }

            casted.callback.run();
        }
        // TODO: handle new maze config
    }

    public static void updateTextStatus(String status) {
        textStatus.setText(status);
        textStatus.setSize(textStatus.getPreferredSize());
        textStatus.setLocation(Window.getXCentered(textStatus), textStatus.getY());
        textStatus.setVisible(true);
    }

    private static void setupCountdownTimer() {
        tcCountdown = new TimedCounter(2, 2) {
            @Override
            public void onStart() {
                updateTextStatus("Starting in 3...");
            }
            
            @Override
            public void onTick() {
                textStatus.setText("Starting in " + String.valueOf(frames - frame) + "...");
            }

            @Override
            public void onFinish() {
                textStatus.setVisible(false);

                mazeLeft.allowMove = true;
                mazeLeft.allowReset = true;
                mazeLeft.allowStep = true;
                
                mazeRight.allowMove = true;     // NOTE: 2nd player might be controlled through socket, rendering these useless
                mazeRight.allowReset = true;
                mazeRight.allowStep = true;
            }
        };
    }

    public static void updateMultiplayer() {
        
        Config.multiplayer = Config.multiplayerOffline || Config.multiplayerOnline;

        mazeRight.clearMaze();

        if (Config.multiplayer) {
            Window.setSize(Window.mazeWidth * 2, Window.mazeHeight);
            
            if (Config.multiplayerOffline) {
                mazeRight.setMaze(maze);
                mazeLeft.setUserText("Player 1");
                mazeRight.setUserText("Player 2");
            }
            else {
                if (OnlineServer.opened) {
                    mazeLeft.setUserText("You (host)");

                    if (OnlineServer.clientConnected) {
                        mazeRight.setUserText("Opponent");
                    }
                    else {
                        mazeRight.setUserText("Opponent (searching...)");
                    }
                }
                else {
                    if (OnlineClient.connected) {
                        mazeLeft.setUserText("You");
                        mazeRight.setUserText("Opponent (host)");
                    }
                    else {
                        mazeLeft.setUserText("You");
                        mazeRight.setUserText("Opponent (searching...)");
                    }
                }
            }
        }
        else {
            Window.setSize(Window.mazeWidth, Window.mazeHeight);
            mazeLeft.setUserText("Singleplayer");
        }
        setupKeyCallbacks();
        textStatus.setLocation(Window.getXCentered(textStatus), textStatus.getY());
        UI.buttons.setSize(Window.width, UI.buttons.getHeight());
    }

    public static void setupKeyCallbacks() {

        // NOTE: events are only sent if the action is allowed locally

        KeyHandler.Action.P1_MOVE_UP.setCallback(() -> {
            if (mazeLeft.tryToMove(Maze.Direction.UP)) {
                tryToSend(KeyHandler.Action.P2_MOVE_UP);
            }
        });
        KeyHandler.Action.P1_MOVE_DOWN.setCallback(() -> {
            if (mazeLeft.tryToMove(Maze.Direction.DOWN)) {
                tryToSend(KeyHandler.Action.P2_MOVE_DOWN);
            }
        });
        KeyHandler.Action.P1_MOVE_LEFT.setCallback(() -> {
            if (mazeLeft.tryToMove(Maze.Direction.LEFT)) {
                tryToSend(KeyHandler.Action.P2_MOVE_LEFT);
            }
        });
        KeyHandler.Action.P1_MOVE_RIGHT.setCallback(() -> {
            if (mazeLeft.tryToMove(Maze.Direction.RIGHT)) {
                tryToSend(KeyHandler.Action.P2_MOVE_RIGHT);
            }
        });

        KeyHandler.Action.P1_MAZE_RESET.setCallback(() -> {
            if (mazeLeft.resetMazeGraphics()) {
                tryToSend(KeyHandler.Action.P2_MAZE_RESET);
            }
        });
        KeyHandler.Action.P1_MAZE_HINT.setCallback(() -> {
            if (mazeLeft.showHint()) {
                tryToSend(KeyHandler.Action.P2_MAZE_HINT);
            }
        });

        KeyHandler.Action.P1_MAZE_STEP_UNDO.setCallback(() -> {
            if (mazeLeft.step(-1)) {
                tryToSend(KeyHandler.Action.P2_MAZE_STEP_UNDO);
            }
        });
        KeyHandler.Action.P1_MAZE_STEP_REDO.setCallback(() -> {
            if (mazeLeft.step(1)) {
                tryToSend(KeyHandler.Action.P2_MAZE_STEP_REDO);
            }});
        
        if (Config.multiplayer) {
            // p2 shouldn't send any events through OnlineSocket since
            // if p2 can click on these buttons, the gamemode must be local
            KeyHandler.Action.P2_MOVE_UP.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.UP); });
            KeyHandler.Action.P2_MOVE_DOWN.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.DOWN); });
            KeyHandler.Action.P2_MOVE_LEFT.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.LEFT); });
            KeyHandler.Action.P2_MOVE_RIGHT.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.RIGHT); });

            KeyHandler.Action.P2_MAZE_RESET.setCallback(() -> { mazeRight.resetMazeGraphics(); });
            KeyHandler.Action.P2_MAZE_HINT.setCallback(() -> { mazeRight.showHint(); });
            KeyHandler.Action.P2_MAZE_STEP_UNDO.setCallback(() -> { mazeRight.step(-1); });
            KeyHandler.Action.P2_MAZE_STEP_REDO.setCallback(() -> { mazeRight.step(1); });
        }

        KeyHandler.Action.MAZE_NEW.setCallback(() -> {
            generateNewMaze();
            // NOTE: does OnlineSocket.send() in the mazegen thread
        });

        // zoom shouldn't affect the online opponent's view
        KeyHandler.Action.ZOOM_IN.setCallback(() -> { zoom(1); });
        KeyHandler.Action.ZOOM_OUT.setCallback(() -> { zoom(-1); });
        
        KeyHandler.Action.START.setCallback(() -> {
            
            if (!Config.multiplayer) {
                return;
            }
            if (Config.multiplayerOnline) {
                if (!OnlineServer.clientConnected && ! OnlineClient.connected) {
                    return;
                }
            }
            if (!mazeLeft.allowStart && mazeRight.allowStart) {
                return;
            }

            mazeLeft.allowStart = false;
            mazeRight.allowStart = false;

            tcCountdown.start();
            tryToSend(KeyHandler.Action.START);
        });
    }

    public static void zoom(int direction) {

        int ch = 5 * direction;
        Config.blockSize += ch;
        hintFont = new Font(hintFont.getName(), hintFont.getStyle(), (int)(0.5*Config.blockSize));

        mazeLeft.zoom(ch);
        mazeRight.zoom(ch);
    }

    public static void generateNewMaze() {
        
        if (Config.multiplayer) {
            if (!mazeRight.animationsFinished()) {
                return;
            }
        }
        else if (!mazeLeft.animationsFinished()) {
            return;
        }

        updateTextStatus("Generating...");

        MazeGen.cancel = mazeGenThreadDone != true;
        while (!mazeGenThreadDone); {}

        new Thread(() -> {

            // new maze in 2D-array-form
            mazeGenThreadDone = false;
            maze = MazeGen.generate();

            if (!MazeGen.cancel) {

                System.out.println(maze.creationPath);
                maze.printCreationPath();

                mazeLeft.setMaze(maze);

                if (Config.multiplayerOffline || tryToSend(maze)) {
                    mazeRight.setMaze(maze);
                }
            }

            mazeGenThreadDone = true;

        }).start();

    }

    private static boolean tryToSend(Object obj) {
        if (OnlineClient.connected) {
            if (OnlineClient.handledReceived) {
                OnlineClient.send(obj);
                return true;
            }
        }
        else if (OnlineServer.clientConnected) {
            if (OnlineServer.handledReceived) {
                OnlineServer.send(obj);
                return true;
            }
        }
        return false;
    }

    // used in OnlineSocket to set a maze without having to call generateNewMaze()
    public static void setMaze(Maze maze) {
        updateTextStatus("Generating...");
        Main.maze = maze;
        mazeLeft.setMaze(maze);

        if (Config.multiplayer) {
            mazeRight.setMaze(maze);
        }
    }


}
