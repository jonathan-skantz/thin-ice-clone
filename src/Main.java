import javax.swing.JLabel;

import java.awt.Font;
import java.util.HashMap;

/**
 * Main class for the game.
 */
public class Main {

    private static Maze maze;   // will be copied into mazeLeft (and potentially mazeRight)

    public static MazeContainer mazeLeft;
    public static MazeContainer mazeRight;

    public static Font font = new Font("arial", Font.PLAIN, 20);
    public static final Font fontInfo = new Font("roboto", Font.PLAIN, 17);
    public static Font hintFont = new Font("verdana", Font.BOLD, (int)(0.5*Config.blockSize));

    public static volatile boolean mazeGenThreadDone = true;
    public static TimedCounter tcCountdown;
    private static JLabel textCountdown;

    public static void main(String[] args) {

        Config.applyDefault();
        Window.setup();

        setupKeyCallbacks();
        setupTimerCountdown();

        Window.sprites.setVisible(false);   // prevents redrawing while setting up
        
        textCountdown = new JLabel();
        textCountdown.setFont(font);
        textCountdown.setLocation(Window.getXCentered(textCountdown), 50);
        textCountdown.setVisible(false);
        Window.sprites.add(textCountdown);

        UI.setupConfigs();

        // make first maze consist of walls only
        maze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        mazeLeft = new MazeContainer(0);
        mazeRight = new MazeContainer(Window.mazeWidth);
        Window.sprites.setVisible(true);
        
        setupOnlineServerCallbacks();
        setupOnlineClientCallbacks();

    }

    private static void setupOnlineServerCallbacks() {
        
        OnlineServer.onOpen = () -> {
            Window.setSize(Window.mazeWidth * 2, Window.mazeHeight);
            mazeLeft.setUserText("You (host)");
            mazeRight.panelStatus.setVisible(false);
            OnlineServer.onClientDisconnect.run();
        };

        OnlineServer.onClose = () -> {
            OnlineClient.onDisconnect.run();
            mazeRight.panelStatus.setVisible(true);
        };

        OnlineServer.onClientConnect = () -> {
            mazeRight.panelDisconnected.setVisible(false);
            mazeLeft.setMaze(maze);     // reset server graphics
            mazeRight.setUserText("Opponent");

            OnlineServer.send(Config.getHostSettings());     // send server settings
            
            if (mazeLeft.status != MazeContainer.Status.WAITING_FOR_FIRST_MAZE) {
                mazeRight.setStatus(MazeContainer.Status.COPYING);
                mazeRight.setMaze(maze);
            }
            OnlineServer.send(maze);    // server sends its maze
            
            if (mazeLeft.status == MazeContainer.Status.READY) {
                OnlineServer.send(KeyHandler.Action.P2_READY);
            }
        };

        OnlineServer.onClientDisconnect = () -> {
            mazeRight.panelDisconnected.setVisible(true);
            if (mazeLeft.status != MazeContainer.Status.WAITING_FOR_FIRST_MAZE) {
                mazeLeft.setStatus(MazeContainer.Status.WAITING_FOR_OPPONENT);
            }
            mazeRight.setUserText("Opponent (searching...)");
        };

        OnlineServer.onReceived = () -> {
            Object received = OnlineServer.receivedObject;
            handleReceived(received);
        };

    }

    private static void setupOnlineClientCallbacks() {

        OnlineClient.onStartSearch = () -> {
            Window.setSize(Window.mazeWidth * 2, Window.mazeHeight);
            mazeLeft.setUserText("You");
            mazeRight.panelStatus.setVisible(false);
            OnlineServer.onClientDisconnect.run();
        };
        
        OnlineClient.onStopSearch = () -> {
            OnlineClient.onDisconnect.run();
            mazeRight.panelStatus.setVisible(true);
        };

        OnlineClient.onConnect = () -> {
            UI.setHostSettingsEnabled(false);
            mazeRight.panelDisconnected.setVisible(false);
            mazeLeft.setUserText("You");
            mazeRight.setUserText("Opponent (host)");
        };

        OnlineClient.onDisconnect = () -> {
            Window.setSize(Window.mazeWidth, Window.mazeHeight);
            UI.setHostSettingsEnabled(true);

            if (maze.currentNode == null) {
                mazeLeft.setStatus(MazeContainer.Status.WAITING_FOR_FIRST_MAZE);
            }
            else {
                mazeLeft.setStatus(MazeContainer.Status.NOT_READY);
            }

            mazeLeft.setUserText("Singleplayer");
        };

        OnlineClient.onReceived = () -> {
            Object received = OnlineClient.receivedObject;
            handleReceived(received);
        };

    }

    public static void setToLocalGamemode() {
        Window.setSize(Window.mazeWidth * 2, Window.mazeHeight);
        mazeLeft.setUserText("Player 1");
        mazeRight.setUserText("Player 2");
        mazeRight.panelDisconnected.setVisible(false);

        if (!maze.equals(mazeRight.maze)) {
            mazeRight.setStatus(MazeContainer.Status.COPYING);
            mazeRight.setMaze(maze);
        }
    }

    private static void handleReceived(Object obj) {
        
        if (obj.getClass() == Maze.Direction.class) {
            mazeRight.tryToMove((Maze.Direction)obj);
        }
        else if (obj.getClass() == Maze.class) {
            copyOpponentMaze((Maze)obj);
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
        else if (obj instanceof HashMap) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> cast = (HashMap<String, Object>) obj;
            UI.applyHostSettings(cast);
        }
    }

    private static void setupTimerCountdown() {

        tcCountdown = new TimedCounter(2, 2) {
            @Override
            public void onStart() {
                textCountdown.setVisible(true);
            }
            
            @Override
            public void onTick() {
                textCountdown.setText("Starting in " + String.valueOf(frames - frame) + "...");
                textCountdown.setSize(textCountdown.getPreferredSize());
                textCountdown.setLocation(Window.getXCentered(textCountdown), textCountdown.getY());
            }

            @Override
            public void onFinish() {
                textCountdown.setVisible(false);
                mazeLeft.setStatus(MazeContainer.Status.PLAYING);
                mazeRight.setStatus(MazeContainer.Status.PLAYING);
            }
        };
    }

    public static void onResize() {
        textCountdown.setLocation(Window.getXCentered(textCountdown), textCountdown.getY());
        UI.buttons.setSize(Window.width, UI.buttons.getHeight());
        UI.buttons.revalidate();
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
        
        KeyHandler.Action.P2_MOVE_UP.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.UP); });
        KeyHandler.Action.P2_MOVE_DOWN.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.DOWN); });
        KeyHandler.Action.P2_MOVE_LEFT.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.LEFT); });
        KeyHandler.Action.P2_MOVE_RIGHT.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.RIGHT); });

        KeyHandler.Action.P2_MAZE_RESET.setCallback(() -> { mazeRight.resetMazeGraphics(); });
        KeyHandler.Action.P2_MAZE_HINT.setCallback(() -> { mazeRight.showHint(); });
        KeyHandler.Action.P2_MAZE_STEP_UNDO.setCallback(() -> { mazeRight.step(-1); });
        KeyHandler.Action.P2_MAZE_STEP_REDO.setCallback(() -> { mazeRight.step(1); });
            
        KeyHandler.Action.P2_READY.setCallback(() -> {

            if (!mazeRight.status.allowsReady()) {
                if (mazeRight.status == MazeContainer.Status.MIRRORING) {
                    mazeRight.statusAfterMirror = MazeContainer.Status.READY;
                }
                return;
            }
            mazeRight.setStatus(MazeContainer.Status.READY);

            if (Config.multiplayer && mazeLeft.status == MazeContainer.Status.READY) {
                UI.setHostSettingsEnabled(false);
            }

            if (!Config.multiplayer || mazeLeft.status == MazeContainer.Status.READY) {
                tcCountdown.start();
            }
        });

        KeyHandler.Action.P2_SURRENDER.setCallback(() -> {
            if (!mazeRight.status.allowsSurrender()) {
                return;
            }

            UI.setHostSettingsEnabled(true);
            mazeRight.setStatus(MazeContainer.Status.SURRENDERED);
            mazeLeft.setStatus(MazeContainer.Status.GAME_WON);
        });

        KeyHandler.Action.P1_READY.setCallback(() -> {
            
            if (!mazeLeft.status.allowsReady()) {
                return;
            }

            mazeLeft.setStatus(MazeContainer.Status.READY);
            tryToSend(KeyHandler.Action.P2_READY);

            if (!Config.multiplayerOffline) {
                UI.setHostSettingsEnabled(false);
            }

            if (!Config.multiplayer || mazeRight.status == MazeContainer.Status.READY) {
                tcCountdown.start();
            }
        });

        KeyHandler.Action.P1_SURRENDER.setCallback(() -> {
            if (!mazeLeft.status.allowsSurrender()) {
                return;
            }
            UI.setHostSettingsEnabled(true);
            mazeLeft.setStatus(MazeContainer.Status.SURRENDERED);
            mazeRight.setStatus(MazeContainer.Status.GAME_WON);
            tryToSend(KeyHandler.Action.P2_SURRENDER);
        });

        KeyHandler.Action.MAZE_NEW.setCallback(() -> {
            generateNewMaze();
            // NOTE: does OnlineSocket.send() in the mazegen thread
        });

        // zoom shouldn't affect the online opponent's view
        KeyHandler.Action.ZOOM_IN.setCallback(() -> { zoom(1); });
        KeyHandler.Action.ZOOM_OUT.setCallback(() -> { zoom(-1); });
        
    }

    public static void zoom(int direction) {

        int ch = 5 * direction;
        Config.blockSize += ch;
        hintFont = new Font(hintFont.getName(), hintFont.getStyle(), (int)(0.5*Config.blockSize));

        mazeLeft.zoom(ch);
        mazeRight.zoom(ch);
    }

    // only used be P1
    public static void generateNewMaze() {

        if (!mazeLeft.status.allowsNewMaze()) {
            // either both left and right or none are allowed
            return;
        }

        mazeLeft.setStatus(MazeContainer.Status.GENERATING);

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
                    mazeRight.setStatus(MazeContainer.Status.COPYING);
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

    public static void copyOpponentMaze(Maze maze) {
        
        if (!Main.maze.equals(maze)) {
            mazeLeft.setStatus(MazeContainer.Status.COPYING);
        }

        if (maze.currentNode != null) {
            // prevents displaying "Generating..." when first is not generated
            mazeRight.setStatus(MazeContainer.Status.GENERATING);
            mazeRight.setMaze(maze);
        }
        
        Main.maze = maze;
        mazeLeft.setMaze(maze);
    }


}
