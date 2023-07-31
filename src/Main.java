import javax.swing.JLabel;
import java.awt.Font;

/**
 * Main class for the game.
 */
public class Main {

    private static boolean started;

    private static Maze maze;   // will be copied into mazeLeft (and potentially mazeRight)

    public static boolean firstMazeCreated = false;   // to prevent starting until first maze is created

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
                started = true;
            }
        };
    }

    public static void updateMultiplayer() {

        if (Config.multiplayer) {
            mazeRight.setMaze(maze);
            
            if (Config.multiplayerOffline) {
                mazeLeft.setUserText("Player 1");
                mazeRight.setUserText("Player 2");
            }
            else {
                mazeLeft.setUserText("You");
                mazeRight.setUserText("Opponent");
            }
        }
        else {
            mazeRight.freeze();
            mazeLeft.setUserText("Offline play");
        }
        setupKeyCallbacks();
        textStatus.setLocation(Window.getXCentered(textStatus), textStatus.getY());
        UI.buttons.setSize(Window.width, UI.buttons.getHeight());
    }

    public static void setupKeyCallbacks() {

        // NOTE: events are only sent if the action is allowed locally

        KeyHandler.Action.P1_MOVE_UP.setCallback(() -> {
            if (mazeLeft.tryToMove(Maze.Direction.UP)) {
                OnlineSocket.send(KeyHandler.Action.P2_MOVE_UP);
            }
        });
        KeyHandler.Action.P1_MOVE_DOWN.setCallback(() -> {
            if (mazeLeft.tryToMove(Maze.Direction.DOWN)) {
                OnlineSocket.send(KeyHandler.Action.P2_MOVE_DOWN);
            }
        });
        KeyHandler.Action.P1_MOVE_LEFT.setCallback(() -> {
            if (mazeLeft.tryToMove(Maze.Direction.LEFT)) {
                OnlineSocket.send(KeyHandler.Action.P2_MOVE_LEFT);
            }
        });
        KeyHandler.Action.P1_MOVE_RIGHT.setCallback(() -> {
            if (mazeLeft.tryToMove(Maze.Direction.RIGHT)) {
                OnlineSocket.send(KeyHandler.Action.P2_MOVE_RIGHT);
            }
        });

        KeyHandler.Action.P1_MAZE_RESET.setCallback(() -> {
            if (mazeLeft.resetMazeGraphics()) {
                OnlineSocket.send(KeyHandler.Action.P2_MAZE_RESET);
            }
        });
        KeyHandler.Action.P1_MAZE_HINT.setCallback(() -> {
            if (mazeLeft.showHint()) {
                OnlineSocket.send(KeyHandler.Action.P2_MAZE_HINT);
            }
        });

        KeyHandler.Action.P1_MAZE_STEP_UNDO.setCallback(() -> {
            if (mazeLeft.step(-1, true)) {
                OnlineSocket.send(KeyHandler.Action.P2_MAZE_STEP_UNDO);
            }
        });
        KeyHandler.Action.P1_MAZE_STEP_REDO.setCallback(() -> {
            if (mazeLeft.step(1, true)) {
                OnlineSocket.send(KeyHandler.Action.P2_MAZE_STEP_REDO);
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
            KeyHandler.Action.P2_MAZE_STEP_UNDO.setCallback(() -> { mazeRight.step(-1, true); });
            KeyHandler.Action.P2_MAZE_STEP_REDO.setCallback(() -> { mazeRight.step(1, true); });
        }

        KeyHandler.Action.MAZE_NEW.setCallback(() -> {
            generateNewMaze();
            // NOTE: does OnlineSocket.send() in the mazegen thread
        });

        // zoom shouldn't affect the online opponent's view
        KeyHandler.Action.ZOOM_IN.setCallback(() -> { zoom(1); });
        KeyHandler.Action.ZOOM_OUT.setCallback(() -> { zoom(-1); });
        
        KeyHandler.Action.START.setCallback(() -> {
            
            if (Config.multiplayer && !started && firstMazeCreated &&
                mazeLeft.animationsFinished() && mazeRight.animationsFinished()) {

                tcCountdown.start();
                OnlineSocket.send(KeyHandler.Action.START);
            }
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
        
        if (!mazeLeft.animationsFinished() || (Config.multiplayer && !mazeRight.animationsFinished())) {
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
                firstMazeCreated = true;

                System.out.println(maze.creationPath);
                maze.printCreationPath();

                mazeLeft.setMaze(maze);

                if (Config.multiplayer) {
                    started = false;
                    mazeRight.setMaze(maze);
                    OnlineSocket.send(maze);
                }
            }

            mazeGenThreadDone = true;

        }).start();

    }

    // used in OnlineSocket to set a maze without having to call generateNewMaze()
    public static void setMaze(Maze maze) {
        updateTextStatus("Generating...");
        firstMazeCreated = true;
        Main.maze = maze;
        mazeLeft.setMaze(maze);

        if (Config.multiplayer) {
            started = false;
            mazeRight.setMaze(maze);
        }
    }


}
