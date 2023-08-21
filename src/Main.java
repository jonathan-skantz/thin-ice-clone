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
        mazeLeft = new MazeContainer(0, MazeContainer.PlayerRole.P1_SINGLEPLAYER);
        mazeRight = new MazeContainer(Window.mazeWidth, MazeContainer.PlayerRole.P2_LOCAL);
        Window.sprites.setVisible(true);
        
        setupOnlineServerCallbacks();
        setupOnlineClientCallbacks();

    }

    private static void setupOnlineServerCallbacks() {
        
        OnlineServer.onOpen = () -> {
            onSwitchGamemode();

            Window.setSize(Window.mazeWidth * 2, Window.mazeHeight);
            
            mazeRight.panelDisconnected.setVisible(true);
            mazeRight.setUserText("Opponent (searching...)");
            mazeRight.playerRole = MazeContainer.PlayerRole.OPPONENT;
            
            mazeLeft.setUserText("You (host)");
            mazeLeft.playerRole = MazeContainer.PlayerRole.P1_HOST;
            mazeLeft.setStatus(Status.WAITING_FOR_OPPONENT_TO_CONNECT);
        };

        OnlineServer.onClose = () -> {
            setToSingleplayer();
        };

        OnlineServer.onClientConnect = () -> {
            OnlineServer.send(Config.Host.getSettings());     // send server settings

            mazeRight.resetAll();
            mazeRight.panelDisconnected.setVisible(false);
            mazeRight.setUserText("Opponent");
            
            mazeLeft.resetAll();
            mazeLeft.setStatus(Status.MAZE_EMPTY);
        };

        OnlineServer.onClientDisconnect = () -> {
            if (OnlineServer.opened) {
                mazeRight.panelDisconnected.setVisible(true);
                mazeRight.setUserText("Opponent (searching...)");

                mazeLeft.setStatus(Status.WAITING_FOR_OPPONENT_TO_CONNECT);
            }
            
        };

        OnlineServer.onReceived = () -> {
            handleReceived(OnlineServer.receivedObject);
        };

    }

    private static void setupOnlineClientCallbacks() {

        OnlineClient.onStartSearch = () -> {
            onSwitchGamemode();
            
            UI.setHostSettingsEnabled(false);
            UI.buttonMazeConfig.setEnabled(false);
            Window.setSize(Window.mazeWidth * 2, Window.mazeHeight);
            
            mazeRight.panelDisconnected.setVisible(true);
            mazeRight.setUserText("Opponent (searching...)");
            mazeRight.playerRole = MazeContainer.PlayerRole.OPPONENT;
            
            mazeLeft.setUserText("You");
            mazeLeft.playerRole = MazeContainer.PlayerRole.P1_CLIENT;
            mazeLeft.setStatus(Status.WAITING_FOR_HOST_TO_OPEN);
        };
        
        OnlineClient.onStopSearch = () -> {
            setToSingleplayer();
        };

        OnlineClient.onConnect = () -> {
            
            mazeRight.resetAll();
            mazeRight.panelDisconnected.setVisible(false);
            mazeRight.setUserText("Opponent (host)");
            mazeRight.setStatus(Status.WAITING_FOR_HOST_TO_GENERATE);
            
            mazeLeft.resetAll();
            mazeLeft.setUserText("You");
        };

        OnlineClient.onDisconnect = () -> {
            if (OnlineClient.tryReconnecting) {
                mazeRight.panelDisconnected.setVisible(true);
                mazeLeft.setStatus(Status.WAITING_FOR_HOST_TO_OPEN);
            }
            else {
                UI.buttonMazeConfig.setEnabled(true);
                setToSingleplayer();
            }
        };

        OnlineClient.onReceived = () -> {
            handleReceived(OnlineClient.receivedObject);
        };

    }

    public static void setToLocalGamemode() {
        onSwitchGamemode();
        Window.setSize(Window.mazeWidth * 2, Window.mazeHeight);
        
        mazeRight.panelDisconnected.setVisible(false);
        mazeRight.playerRole = MazeContainer.PlayerRole.P2_LOCAL;
        mazeRight.setStatus(Status.WAITING_FOR_HOST_TO_GENERATE);
        mazeRight.setUserText("Player 2");

        mazeLeft.playerRole = MazeContainer.PlayerRole.P1_LOCAL;
        mazeLeft.setStatus(Status.MAZE_EMPTY);
        mazeLeft.setUserText("Player 1");
    }

    public static void setToSingleplayer() {
        onSwitchGamemode();
        Window.setSize(Window.mazeWidth, Window.mazeHeight);

        mazeLeft.setUserText("Singleplayer");
        mazeLeft.playerRole = MazeContainer.PlayerRole.P1_SINGLEPLAYER;
        mazeLeft.setStatus(Status.MAZE_EMPTY);
    }

    private static void onSwitchGamemode() {
        MazeGen.cancel = true;
        mazeLeft.resetAll();
        mazeRight.resetAll();
        UI.setConfigsEnabled(true);
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
            HashMap<Config.Host, Object> cast = (HashMap<Config.Host, Object>) obj;
            UI.applyHostSettings(cast);
        }
    }

    private static void setupTimerCountdown() {

        tcCountdown = new TimedCounter(2, 2) {
            @Override
            public void onStart() {
                textCountdown.setVisible(true);
                UI.setConfigsEnabled(false);
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
                mazeLeft.setStatus(Status.PLAYING);
                mazeRight.setStatus(Status.PLAYING);
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

            mazeRight.setStatus(Status.READY);

            if (Config.multiplayer && mazeLeft.status == Status.READY) {
                UI.setConfigsEnabled(false);
            }

            if (!Config.multiplayer || mazeLeft.status == Status.READY) {
                tcCountdown.start();
            }
        });

        KeyHandler.Action.P2_SURRENDER.setCallback(() -> {
            if (!mazeRight.status.allowsSurrender()) {
                return;
            }

            UI.setConfigsEnabled(true);
            mazeRight.setStatus(Status.SURRENDERED);
            mazeLeft.setStatus(Status.GAME_WON);
            mazeLeft.gamesWon = 0;
            mazeLeft.updateLabelGamesWon();
        });

        KeyHandler.Action.P1_READY.setCallback(() -> {
            
            if (!mazeLeft.status.allowsReady()) {
                return;
            }

            mazeLeft.setStatus(Status.READY);
            tryToSend(KeyHandler.Action.P2_READY);

            if (!Config.multiplayer || mazeRight.status == Status.READY) {
                tcCountdown.start();
            }
        });

        KeyHandler.Action.P1_SURRENDER.setCallback(() -> {
            if (!mazeLeft.status.allowsSurrender()) {
                return;
            }
            
            UI.setConfigsEnabled(true);
            mazeLeft.setStatus(Status.SURRENDERED);
            mazeRight.setStatus(Status.GAME_WON);
            mazeRight.gamesWon = 0;
            mazeRight.updateLabelGamesWon();
            tryToSend(KeyHandler.Action.P2_SURRENDER);
        });

        KeyHandler.Action.MAZE_NEW.setCallback(() -> {
            if (Config.multiplayerOnline && !OnlineServer.clientConnected) {
                // prevent generating as client
                return;
            }
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

    // only used by server (or left maze)
    public static void generateNewMaze() {

        if (!mazeLeft.status.allowsNewMaze()) {
            // either both left and right or none are allowed
            return;
        }

        mazeLeft.setStatus(Status.GENERATING);

        MazeGen.cancel = true;
        while (!mazeGenThreadDone); {}

        new Thread(() -> {

            // new maze in 2D-array-form
            mazeGenThreadDone = false;
            maze = MazeGen.generate();

            if (!MazeGen.cancel) {

                System.out.println(maze.creationPath);
                maze.printCreationPath();

                mazeLeft.statusAfterAnimation = Status.NOT_READY;
                mazeLeft.setMaze(maze);

                if (Config.multiplayerOffline || tryToSend(maze)) {
                    mazeRight.setStatus(Status.COPYING);
                    
                    mazeRight.statusAfterAnimation = Status.NOT_READY;
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
        
        mazeLeft.setStatus(Status.COPYING);
        mazeLeft.statusAfterAnimation = Status.NOT_READY;
        mazeLeft.setMaze(maze);

        mazeRight.setStatus(Status.GENERATING);
        mazeRight.statusAfterAnimation = Status.NOT_READY;
        mazeRight.setMaze(maze);
        
        Main.maze = maze;
    }


}
