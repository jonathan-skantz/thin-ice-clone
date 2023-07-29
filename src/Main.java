import javax.swing.JLabel;
import java.awt.Font;

/**
 * Main class for the game.
 */
public class Main {

    private static Maze maze;   // will be copied into mazeLeft (and potentially mazeRight)

    public static boolean firstMazeCreated = false;   // to prevent starting until first maze is created

    public static MazeContainer mazeLeft;
    public static MazeContainer mazeRight;

    public static JLabel textMazeStatus;
    public static final boolean ENABLE_ANIMATIONS = true;

    public static Font font = new Font("arial", Font.PLAIN, 20);
    public static Font hintFont = new Font("verdana", Font.BOLD, (int)(0.5*Config.blockSize));

    public static volatile boolean mazeGenThreadDone = true;
    public static TimedCounter tcCountdown;

    public static void main(String[] args) {

        Window.setup();
        Config.apply();

        setupKeyCallbacks();
        setupCountdownTimer();

        Window.sprites.setVisible(false);   // prevents redrawing while setting up
        
        textMazeStatus = createLabel("Generating...");
        textMazeStatus.setLocation(Window.getXCentered(textMazeStatus), 50);

        UI.setupConfigs();

        // make first maze consist of walls only
        maze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        mazeLeft = new MazeContainer();
        Window.sprites.setVisible(true);

    }

    public static void updateTextStatus(String status) {
        textMazeStatus.setText(status);
        textMazeStatus.setSize(textMazeStatus.getPreferredSize());
        textMazeStatus.setLocation(Window.getXCentered(textMazeStatus), textMazeStatus.getY());
        textMazeStatus.setVisible(true);
    }

    private static void setupCountdownTimer() {
        tcCountdown = new TimedCounter(2, 2) {
            @Override
            public void onStart() {
                updateTextStatus("Starting in 3...");
            }
            
            @Override
            public void onTick() {
                textMazeStatus.setText("Starting in " + String.valueOf(frames - frame) + "...");
            }

            @Override
            public void onFinish() {
                textMazeStatus.setVisible(false);
            }
        };
    }

    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setVisible(false);
        label.setSize(label.getPreferredSize());
        Window.sprites.add(label);
        return label;
    }

    public static void toggleMultiplayer() {

        if (Config.multiplayer) {
            mazeRight = new MazeContainer();
            mazeRight.sprites.setLocation(Window.mazeWidth, 0);
            mazeRight.setMaze(maze);
        }
        else {
            mazeRight.dispose();
        }
        setupKeyCallbacks();
        textMazeStatus.setLocation(Window.getXCentered(textMazeStatus), textMazeStatus.getY());
        UI.buttons.setSize(Window.width, UI.buttons.getHeight());
    }

    public static void setupKeyCallbacks() {

        KeyHandler.Action.P1_MOVE_UP.setCallback(() -> { mazeLeft.tryToMove(Maze.Direction.UP); });
        KeyHandler.Action.P1_MOVE_DOWN.setCallback(() -> { mazeLeft.tryToMove(Maze.Direction.DOWN); });
        KeyHandler.Action.P1_MOVE_LEFT.setCallback(() -> { mazeLeft.tryToMove(Maze.Direction.LEFT); });
        KeyHandler.Action.P1_MOVE_RIGHT.setCallback(() -> { mazeLeft.tryToMove(Maze.Direction.RIGHT); });

        KeyHandler.Action.P1_MAZE_RESET.setCallback(() -> { mazeLeft.resetMazeGraphics(); });
        KeyHandler.Action.P1_MAZE_HINT.setCallback(() -> { mazeLeft.showHint(); });
        KeyHandler.Action.P1_MAZE_STEP_UNDO.setCallback(() -> { mazeLeft.step(-1, true); });
        KeyHandler.Action.P1_MAZE_STEP_REDO.setCallback(() -> { mazeLeft.step(1, true); });
        
        if (Config.multiplayer) {
            KeyHandler.Action.P2_MOVE_UP.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.UP); });
            KeyHandler.Action.P2_MOVE_DOWN.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.DOWN); });
            KeyHandler.Action.P2_MOVE_LEFT.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.LEFT); });
            KeyHandler.Action.P2_MOVE_RIGHT.setCallback(() -> { mazeRight.tryToMove(Maze.Direction.RIGHT); });

            KeyHandler.Action.P2_MAZE_RESET.setCallback(() -> { mazeRight.resetMazeGraphics(); });
            KeyHandler.Action.P2_MAZE_HINT.setCallback(() -> { mazeRight.showHint(); });
            KeyHandler.Action.P2_MAZE_STEP_UNDO.setCallback(() -> { mazeRight.step(-1, true); });
            KeyHandler.Action.P2_MAZE_STEP_REDO.setCallback(() -> { mazeRight.step(1, true); });
        }

        KeyHandler.Action.MAZE_NEW.setCallback(() -> { generateNewMaze(); });
        KeyHandler.Action.ZOOM_IN.setCallback(() -> { zoom(1); });
        KeyHandler.Action.ZOOM_OUT.setCallback(() -> { zoom(-1); });
        
        KeyHandler.Action.START.setCallback(() -> {
            if (firstMazeCreated && Config.multiplayer &&
                mazeLeft.animationsFinished() && mazeRight.animationsFinished()) {
                
                tcCountdown.start();
            }
        });
    }

    public static void zoom(int direction) {

        int ch = 5 * direction;
        Config.blockSize += ch;
        hintFont = new Font(hintFont.getName(), hintFont.getStyle(), (int)(0.5*Config.blockSize));

        mazeLeft.zoom(ch);
        if (Config.multiplayer) {
            mazeRight.zoom(ch);
        }

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
                    mazeRight.setMaze(maze);
                }

            }

            mazeGenThreadDone = true;

        }).start();

    }

}
