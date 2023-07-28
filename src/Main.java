import javax.swing.JLabel;
import java.awt.Font;

/**
 * Main class for the game.
 */
public class Main {

    private static Maze maze;   // will be copied into mazeLeft (and potentially mazeRight)

    private static MazeContainer mazeLeft;
    private static MazeContainer mazeRight;

    public static JLabel textGenerating;
    public static final boolean ENABLE_ANIMATIONS = true;

    public static Font font = new Font("arial", Font.PLAIN, 20);
    public static Font hintFont = new Font("verdana", Font.BOLD, (int)(0.5*Config.blockSize));

    public static volatile boolean mazeGenThreadDone = true;

    public static void main(String[] args) {

        Window.setup();
        Config.apply();

        setupKeyCallbacks();

        Window.sprites.setVisible(false);   // prevents redrawing while setting up
        
        textGenerating = createLabel("Generating...");
        textGenerating.setLocation(Window.getXCentered(textGenerating), 50);

        UI.setupConfigs();

        // make first maze consist of walls only
        maze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        mazeLeft = new MazeContainer(true);
        Window.sprites.setVisible(true);

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
            mazeRight = new MazeContainer(false);
            mazeRight.setMaze(new Maze(maze));
        }
        else {
            mazeRight.dispose();
            mazeRight = null;
        }
        setupKeyCallbacks();
        textGenerating.setLocation(Window.getXCentered(textGenerating), textGenerating.getY());
        // TODO: move mazeConfig button
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

        textGenerating.setVisible(true);

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

                if (Config.multiplayer) {
                    mazeRight.setMaze(new Maze(maze));
                }

            }

            mazeGenThreadDone = true;

        }).start();

    }

}
