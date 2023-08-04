import javax.swing.JLabel;

import java.awt.Component;
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

        OnlineServer.onClientConnect = () -> {
            updateMultiplayer();

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

        OnlineServer.onClientDisconnect = OnlineServer.onOpen = OnlineServer.onClose = OnlineClient.onConnect = OnlineClient.onDisconnect = () -> { 
            // not handled by UI since server is still open
            updateMultiplayer();
        };
        
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
            // TODO: obj.getClass() is not the same as HashMap.class
            UI.applyHostSettings((HashMap<String, Object>) obj);
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

    public static void updateMultiplayer() {
        
        Config.multiplayer = Config.multiplayerOffline || Config.multiplayerOnline;

        mazeRight.clearMaze();
        boolean enableHostSettings = true;
        
        if (Config.multiplayer) {
            Window.setSize(Window.mazeWidth * 2, Window.mazeHeight);
            
            if (Config.multiplayerOffline) {
                if (mazeLeft.status != MazeContainer.Status.WAITING_FOR_FIRST_MAZE) {
                    mazeLeft.setMaze(maze);
                    mazeRight.setStatus(MazeContainer.Status.COPYING);
                }
                mazeRight.setMaze(maze);
                mazeLeft.setUserText("Player 1");
                mazeRight.setUserText("Player 2");
            }
            else {
                mazeRight.panelStatus.setVisible(false);

                if (OnlineServer.opened) {
                    mazeLeft.setUserText("You (host)");

                    if (OnlineServer.clientConnected) {
                        mazeLeft.setMaze(maze);     // reset servers graphics
                        mazeRight.setUserText("Opponent");
                    }
                    else {
                        mazeLeft.setStatus(MazeContainer.Status.WAITING_FOR_OPPONENT);
                        mazeRight.setUserText("Opponent (searching...)");
                    }
                }
                else {
                    enableHostSettings = false;
                    if (OnlineClient.connected) {
                        mazeLeft.setUserText("You");
                        mazeRight.setUserText("Opponent (host)");
                    }
                    else {
                        mazeLeft.setStatus(MazeContainer.Status.WAITING_FOR_OPPONENT);
                        mazeLeft.setUserText("You");
                        mazeRight.setUserText("Opponent (searching...)");
                    }
                }
            }
        }
        else {
            if (mazeLeft.status == MazeContainer.Status.WAITING_FOR_OPPONENT) {
                if (maze.currentNode == null) {
                    mazeLeft.setStatus(MazeContainer.Status.WAITING_FOR_FIRST_MAZE);
                }
                else {
                    mazeLeft.setStatus(MazeContainer.Status.NOT_READY);
                }
            }
            Window.setSize(Window.mazeWidth, Window.mazeHeight);
            mazeLeft.setUserText("Singleplayer");
        }
        setupKeyCallbacks();
        textCountdown.setLocation(Window.getXCentered(textCountdown), textCountdown.getY());
        UI.buttons.setSize(Window.width, UI.buttons.getHeight());
        UI.buttons.revalidate();
        UI.setHostSettingsEnabled(enableHostSettings);
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
                
            KeyHandler.Action.P2_READY.setCallback(() -> {

                if (!mazeRight.status.allowsReady()) {
                    return;
                }
                mazeRight.setStatus(MazeContainer.Status.READY);

                if (!Config.multiplayerOffline || (Config.multiplayerOffline && mazeLeft.status == MazeContainer.Status.READY)) {
                    for (Component comp : UI.buttons.getComponents()) {
                        comp.setEnabled(false);
                    }
                }

                if (!Config.multiplayer || mazeLeft.status == MazeContainer.Status.READY) {
                    tcCountdown.start();
                }
            });

            KeyHandler.Action.P2_SURRENDER.setCallback(() -> {
                if (!mazeRight.status.allowsSurrender()) {
                    return;
                }

                for (Component comp : UI.buttons.getComponents()) {
                    comp.setEnabled(true);
                }
                mazeRight.setStatus(MazeContainer.Status.SURRENDERED);
                mazeLeft.setStatus(MazeContainer.Status.GAME_WON);
            });
        }

        KeyHandler.Action.P1_READY.setCallback(() -> {
            
            if (!mazeLeft.status.allowsReady()) {
                return;
            }

            mazeLeft.setStatus(MazeContainer.Status.READY);
            tryToSend(KeyHandler.Action.P2_READY);

            if (!Config.multiplayerOffline || (Config.multiplayerOffline && mazeRight.status == MazeContainer.Status.READY)) {
                for (Component comp : UI.buttons.getComponents()) {
                    comp.setEnabled(false);
                }
            }

            if (!Config.multiplayer || mazeRight.status == MazeContainer.Status.READY) {
                tcCountdown.start();
            }
        });

        KeyHandler.Action.P1_SURRENDER.setCallback(() -> {
            if (!mazeLeft.status.allowsSurrender()) {
                return;
            }
            for (Component comp : UI.buttons.getComponents()) {
                comp.setEnabled(true);
            }
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
        
        mazeLeft.setStatus(MazeContainer.Status.COPYING);

        if (maze.currentNode != null) {
            // prevents displaying "Generating..." when first is not generated
            mazeRight.setStatus(MazeContainer.Status.GENERATING);
            mazeRight.setMaze(maze);
        }
        
        Main.maze = maze;
        mazeLeft.setMaze(maze);
    }


}
