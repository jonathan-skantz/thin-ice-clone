import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import javax.swing.JLabel;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;

/**
 * Main class for the game.
 */
public class Main {

    private static Maze maze;
    private static Maze oldMaze;        // keep track of old maze in order to animate change
    private static Maze mazeBeforeThread;
    
    // sprites
    public static Block player;
    public static JLabel textLevelComplete;
    private static JLabel textGenerating;
    private static JLabel textGameOver;
    public static Block[][] mazeBlocks;

    // animations
    private static final boolean ENABLE_ANIMATIONS = false;
    private static boolean animationsFinished = true;

    private static TimedCounter tcSpawnPlayer;
    private static TimedCounter tcNewMaze;
    private static TimedCounter tcReset;

    private static ArrayList<Node> nodesToChange = new ArrayList<>(0);
    
    private static LinkedHashMap<JLabel, Node> hints = new LinkedHashMap<>(Config.hintMax);
    private static Font hintFont = new Font("verdana", Font.BOLD, (int)(0.5*Config.blockSize));

    private static volatile boolean mazeGenThreadDone = true;
    private static boolean gameOver = false;

    public static void main(String[] args) {

        Window.setup();
        Config.apply();
        
        setupTimerSpawnPlayer();
        setupTimerNewMaze();
        setupTimerReset();

        setupKeyCallbacks();

        Window.sprites.setVisible(false);   // prevents redrawing while setting up
        
        // create player
        int size = Config.blockSize - 2 * Config.BLOCK_BORDER_WIDTH;
        player = new Block("src/textures/player.png", size);
        player.velocity = Config.blockSize;
        player.setVisible(false);
        
        setupText();

        UI.setupConfigs();

        // make first maze consist of walls only
        oldMaze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        maze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        mazeBeforeThread = oldMaze;
        createWallBlocks();
        Window.sprites.setVisible(true);

    }

    private static void setupText() {

        Font font = new Font("arial", Font.PLAIN, 20);

        // level complete
        textLevelComplete = new JLabel("Level complete");
        textLevelComplete.setForeground(Color.BLACK);
        
        // generating
        textGenerating = new JLabel("Generating...");
        textGenerating.setForeground(Color.DARK_GRAY);
        
        // game over
        textGameOver = new JLabel("Game over");
        textGameOver.setForeground(Color.RED);
        
        for (JLabel label : new JLabel[]{textLevelComplete, textGenerating, textGameOver}) {
            label.setVisible(false);
            label.setFont(font);
            label.setSize(label.getPreferredSize());
            label.setLocation(Window.getXCentered(label), 50);
            Window.sprites.add(label);
        }

    }

    private static void setupTimerSpawnPlayer() {
        tcSpawnPlayer = new TimedCounter(0.5f, 15) {
            @Override
            public void onStart() {
                player.setVisible(true);
                movePlayerGraphicsTo(maze.currentNode);
                mirrorPlayer(null);
            }

            @Override
            public void onTick() {
                // resize
                float progress = (float)tcSpawnPlayer.frame / tcSpawnPlayer.frames;
                int size = (int)((Config.blockSize - 2 * Config.BLOCK_BORDER_WIDTH) * progress);
                player.setSize(size, size);
            
                movePlayerGraphicsTo(maze.currentNode);
            }

            @Override
            public void onFinish() {
                animationsFinished = true;
            }
        };

    }
    
    private static void setupTimerNewMaze() {
        tcNewMaze = new TimedCounter(10) {
            public void onStart() {               
                animationsFinished = false;
                tcNewMaze.setFramesAndPreserveFPS(nodesToChange.size());
            }

            public void onTick() {
                Node node = nodesToChange.get(tcNewMaze.frame-1);
                
                if (tcNewMaze.frame > 1) {
                    // remove border from last node
                    Node lastNode = nodesToChange.get(tcNewMaze.frame-2);
                    mazeBlocks[lastNode.Y][lastNode.X].setBorder(null);
                }
                
                if (node.equals(oldMaze.currentNode)) {
                    player.setVisible(false);
                }

                refreshBlockGraphics(node);
                mazeBlocks[node.Y][node.X].setBorder(Config.BLOCK_BORDER_GEN);
            }

            @Override
            public void onFinish() {
                Node node = nodesToChange.get(tcNewMaze.frame-1);   // since last node should be changed twice
                mazeBlocks[node.Y][node.X].setBorder(null);
                
                textGenerating.setVisible(false);
                tcSpawnPlayer.start();
            }
        };

        // call onFinish() after one frame's delay
        tcNewMaze.onFinishDelay = (float)tcNewMaze.oneFrameInSeconds();
    }

    // sets up animation to auto-stepback all steps in pathHistory
    private static void setupTimerReset() {

        tcReset = new TimedCounter(5) {
            @Override
            public void onStart() {
                animationsFinished = false;
                tcReset.setFramesAndPreserveFPS(maze.pathHistory.size());
            }

            @Override
            public void onTick() {
                step(-1);
                movePlayerGraphicsTo(maze.currentNode);
            }

            @Override
            public void onFinish() {
                animationsFinished = true;
            }
        };

        tcReset.finished = true;    // used to allow manual stepping

    }

    private static void removeHintTexts() {
        for (JLabel hint : hints.keySet()) {
            Window.sprites.remove(hint);
        }
        hints.clear();

        Window.sprites.repaint();   // some hints are still visible
    }

    private static void resetMazeGraphics() {
        
        if (!mazeGenThreadDone || !animationsFinished) {
            return;
        }

        textGameOver.setVisible(false);
        textLevelComplete.setVisible(false);
        removeHintTexts();

        if (ENABLE_ANIMATIONS) {
            tcReset.start();
        }
        else {
            ArrayList<Node> changed = new ArrayList<>(maze.pathHistory);
            maze.reset();

            for (Node node : changed) {
                refreshBlockGraphics(node);
            }

            movePlayerGraphicsTo(maze.currentNode);
            mirrorPlayer(null);
        }
    }

    public static void showHint() {

        if (gameOver || !animationsFinished || maze.currentNode == null || maze.complete) {
            return;
        }

        Window.sprites.setVisible(false);
        
        // reset old hint blocks
        for (JLabel hintLabel : hints.keySet()) {
            Window.sprites.remove(hintLabel);
        }

        hints.clear();

        // gets solution based on current node
        MazeSolver solver = new MazeSolver(maze);
        LinkedList<Node> path = Config.hintTypeLongest ? solver.findLongestPath() : solver.findShortestPath();

        Node removedFirst = path.removeFirst();

        for (int hint=0; hint<Config.hintMax && hint<path.size(); hint++) {
            Node step = path.get(hint);
            
            JLabel label = new JLabel(String.valueOf(hint+1));
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setForeground(new Color(100, 100, 100));
            label.setFont(hintFont);
            
            label.setSize(Config.blockSize, Config.blockSize);
            int pathFirstAppearance = path.indexOf(step);
            if (pathFirstAppearance != hint) {
                // second step on double
                Node nodeBeforeFirstStep;
                JLabel labelFirstStep;

                Object[] labels = hints.keySet().toArray();
                if (pathFirstAppearance == 0) {
                    nodeBeforeFirstStep = removedFirst;
                    labelFirstStep = (JLabel) labels[pathFirstAppearance];
                }
                else {
                    nodeBeforeFirstStep = path.get(pathFirstAppearance-1);
                    labelFirstStep = (JLabel) labels[pathFirstAppearance];
                }
                if (nodeBeforeFirstStep.X < step.X) {
                    labelFirstStep.setHorizontalAlignment(JLabel.LEFT);
                    label.setHorizontalAlignment(JLabel.RIGHT);
                }
                else {
                    labelFirstStep.setHorizontalAlignment(JLabel.RIGHT);
                    label.setHorizontalAlignment(JLabel.LEFT);
                }

            }
            label.setLocation(getBlockPosition(step));
            
            Window.sprites.add(label);

            hints.put(label, step);
            Window.sprites.setComponentZOrder(label, 1);
        }

        Window.sprites.setVisible(true);
        
    }

    public static void tryToMove(Maze.Direction dir) {

        if (gameOver || !mazeGenThreadDone || maze.complete || !animationsFinished) {
            return;
        }

        Node lastNode = maze.currentNode;

        if (maze.userMove(dir)) {

            player.move(dir);
            mirrorPlayer(dir);
            refreshBlockGraphics(lastNode);

            if (hints.size() > 0) {

                Object[] labels = hints.keySet().toArray();
                
                if (maze.currentNode.equals(hints.get(labels[0]))) {
                    Window.sprites.remove((JLabel)labels[0]);
                    hints.remove(labels[0]);
                }
                else {
                    for (JLabel hintLabel : hints.keySet()) {
                        Window.sprites.remove(hintLabel);
                    }
                    hints.clear();
                    Window.sprites.repaint();   // some labels are still visible
                }
            }

            if (maze.complete) {
                textLevelComplete.setVisible(true);
            }

            else {
                MazeSolver solver = new MazeSolver(maze);
                if (solver.findShortestPath().size() == 0) {
                    textGameOver.setVisible(true);
                    gameOver = true;
                }
            }
        }

    }

    public static void setupKeyCallbacks() {

        KeyHandler.Action.MOVE_UP.setCallback(() -> { tryToMove(Maze.Direction.UP); });
        KeyHandler.Action.MOVE_DOWN.setCallback(() -> { tryToMove(Maze.Direction.DOWN); });
        KeyHandler.Action.MOVE_LEFT.setCallback(() -> { tryToMove(Maze.Direction.LEFT); });
        KeyHandler.Action.MOVE_RIGHT.setCallback(() -> { tryToMove(Maze.Direction.RIGHT); });
        
        KeyHandler.Action.MAZE_NEW.setCallback(() -> { generateNewMaze(); });
        KeyHandler.Action.MAZE_RESET.setCallback(() -> {
            gameOver = false;
            resetMazeGraphics();
        });
        KeyHandler.Action.MAZE_HINT.setCallback(() -> { showHint(); });
        
        KeyHandler.Action.MAZE_STEP_UNDO.setCallback(() -> {
            if (!gameOver && mazeGenThreadDone && tcReset.finished) {
               step(-1);
            }
        });

        KeyHandler.Action.MAZE_STEP_REDO.setCallback(() -> {
            if (!gameOver && mazeGenThreadDone && tcReset.finished) {
                step(1);
            }
        });
            
        KeyHandler.Action.ZOOM_IN.setCallback(() -> { zoom(1); });
        KeyHandler.Action.ZOOM_OUT.setCallback(() -> { zoom(-1); });
    }

    public static void zoom(int direction) {

        int ch = 5 * direction;

        Config.blockSize += ch;
        player.velocity += ch;

        setMazeStartCoords();

        if (maze.currentNode != null) {
            player.setSize(player.getWidth() + ch, player.getHeight() + ch);
            movePlayerGraphicsTo(maze.currentNode);
        }

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                mazeBlocks[y][x].setSize(Config.blockSize, Config.blockSize);
                mazeBlocks[y][x].setLocation(getBlockPosition(new Node(x, y)));
            }
        }

        hintFont = new Font(hintFont.getName(), hintFont.getStyle(), (int)(0.5*Config.blockSize));
        for (JLabel label : hints.keySet()) {
            label.setSize(Config.blockSize, Config.blockSize);
            label.setFont(hintFont);
            label.setLocation(getBlockPosition(hints.get(label)));
        }

    }

    private static boolean playerMustMove(Maze.Direction dir) {

        if (maze.walkable(maze.currentNode.getNeighbor(dir))) {

            for (Maze.Direction d : Maze.Direction.values()) {
                if (d != dir && maze.walkable(maze.currentNode.getNeighbor(d))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static void mirrorPlayer(Maze.Direction dir) {

        if (dir == null) {
            player.setMirrored(!maze.walkable(maze.currentNode.getNeighbor(Maze.Direction.LEFT)));
        }

        else if (playerMustMove(Maze.Direction.LEFT)) {
            player.setMirrored(false);
        }
        else if (playerMustMove(Maze.Direction.RIGHT)) {
            player.setMirrored(true);
        }
        else if (dir == Maze.Direction.LEFT) {
            player.setMirrored(false);
        }
        else if (dir == Maze.Direction.RIGHT) {
            player.setMirrored(true);
        }
        
    }

    public static void step(int direction) {
        
        if (tcReset.finished && (maze.complete || !animationsFinished)) {
            return;
        }

        Node lastNode = maze.currentNode;

        Maze.Direction dir = maze.step(direction);

        if (dir != null) {
            player.move(dir);
            mirrorPlayer(dir);
            refreshBlockGraphics(lastNode);

            if (direction == -1) {
                refreshBlockGraphics(maze.currentNode);
            }
        }
        
    }

    private static void movePlayerGraphicsTo(Node node) {
        
        int blockX = Config.mazeStartX + node.X * Config.blockSize;
        int blockY = Config.mazeStartY + node.Y * Config.blockSize;
    
        int centeredX = blockX + (Config.blockSize - player.getWidth()) / 2;
        int centeredY = blockY + (Config.blockSize - player.getHeight()) / 2;

        player.setLocation(centeredX, centeredY);
    }

    private static void refreshBlockGraphics(Node node) {

        Block block = mazeBlocks[node.Y][node.X];
        Node.Type type = maze.get(node);

        block.setType(type);

        for (Node neighbor : maze.getNeighborsOf(node, false)) {
            boolean nodeCausesFrost = maze.get(node) == Node.Type.DOUBLE || maze.get(node) == Node.Type.END_DOUBLE;
            mazeBlocks[neighbor.Y][neighbor.X].setFrost(neighbor, node, nodeCausesFrost);
        }
        
    }

    private static void setMazeStartCoords() {
        Config.mazeStartX = (Window.width - Config.blockSize * MazeGen.width) / 2;
        Config.mazeStartY = (Window.height - Config.blockSize * MazeGen.height) / 2;
    }

    private static void updateMazeGraphicsSize() {
        boolean newSize = MazeGen.height != oldMaze.height || MazeGen.width != oldMaze.width;

        if (newSize) {
            // remove old blocks from canvas
            for (Block[] row : mazeBlocks) {
                for (Block spr : row) {
                    Window.sprites.remove(spr);
                }
            }

            oldMaze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
            createWallBlocks();
            player.setVisible(false);
        }

    }

    public static void newMazeGraphics() {

        removeHintTexts();
        
        Window.sprites.setVisible(true);

        // determine which nodes should change
        nodesToChange.clear();
        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {

                Node node = new Node(x, y);

                if (maze.get(node) == oldMaze.get(node) && !(node.equals(oldMaze.currentNode))) {
                    continue;
                }
                nodesToChange.add(node);
            }
        }

        if (ENABLE_ANIMATIONS) {
            tcNewMaze.start();
        }
        else {
            textGenerating.setVisible(false);
            player.setVisible(true);
            movePlayerGraphicsTo(maze.currentNode);
            mirrorPlayer(null);

            for (Node node : nodesToChange) {
                refreshBlockGraphics(node);
            }
        }

    }

    public static void generateNewMaze() {
        
        if (gameOver || !animationsFinished) {
            return;
        }

        textLevelComplete.setVisible(false);
        textGenerating.setVisible(true);

        MazeGen.cancel = mazeGenThreadDone != true;
        while (!mazeGenThreadDone); {}

        new Thread(() -> {

            // new maze in 2D-array-form
            mazeGenThreadDone = false;
            oldMaze = maze;
            updateMazeGraphicsSize();
            maze = MazeGen.generate();

            if (!MazeGen.cancel) {
                System.out.println(maze.creationPath);
                maze.printCreationPath();
                newMazeGraphics();
                mazeBeforeThread = maze;
            }
            else {
                maze = mazeBeforeThread;    // new maze was cancelled --> fallback to last maze
            }

            mazeGenThreadDone = true;

        }).start();

    }

    private static Point getBlockPosition(Node node) {
        int posX = Config.mazeStartX + node.X * Config.blockSize;
        int posY = Config.mazeStartY + node.Y * Config.blockSize;
        return new Point(posX, posY);
    }

    private static void createWallBlocks() {

        mazeBlocks = new Block[MazeGen.height][MazeGen.width];
        
        setMazeStartCoords();

        for (int y=0; y<MazeGen.height; y++) {
            for (int x=0; x<MazeGen.width; x++) {
                Block block = new Block("src/textures/wall.png", Config.blockSize);
                mazeBlocks[y][x] = block;
                block.setLocation(getBlockPosition(new Node(x, y)));
            }
        }

    }

}
