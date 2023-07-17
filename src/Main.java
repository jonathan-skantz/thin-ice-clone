import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.JLabel;

import java.awt.Color;
import java.awt.Font;

/**
 * Main class for the game.
 */
public class Main {

    public static Window window = new Window();
    
    private static Maze maze;
    private static Maze oldMaze;        // keep track of old maze in order to animate change
    
    // sprites
    public static Block player;
    public static JLabel textNextLevel;
    public static Block[][] mazeBlocks;

    public static Node[] hintNodes = new Node[Config.hintMax];
    
    // animations
    private static final boolean ENABLE_ANIMATIONS = true;
    private static boolean animationsFinished = true;

    private static TimedCounter tcSpawnPlayer;
    private static TimedCounter tcNewMaze;
    private static TimedCounter tcReset;

    private static ArrayList<Node> nodesToChange = new ArrayList<>(0);

    public static void main(String[] args) {

        Config.apply();
        
        setupTimerSpawnPlayer();
        setupTimerNewMaze();
        setupTimerReset();

        setupKeyCallbacks();

        window.sprites.setVisible(false);   // prevents redrawing while setting up
        
        // create player
        int size = Config.blockSize - 2 * Config.BLOCK_BORDER_WIDTH;
        player = new Block("src/textures/player.png", size);
        player.velocity = Config.blockSize;
        player.setVisible(false);
        
        // setup next-level-text
        textNextLevel = new JLabel("Level complete");
        textNextLevel.setVisible(false);
        textNextLevel.setFont(new Font("arial", Font.PLAIN, 20));
        textNextLevel.setForeground(Color.BLACK);
        textNextLevel.setSize(textNextLevel.getPreferredSize());
        
        int y = 50;
        int x = (Window.width - textNextLevel.getWidth()) / 2;
        textNextLevel.setLocation(x, y);
        window.sprites.add(textNextLevel);
        
        UI.setupConfigs();

        // make first maze consist of walls only
        maze = new Maze(MazeGen.width, MazeGen.height, Node.Type.WALL);
        createWallBlocks();
        window.sprites.setVisible(true);

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
                
                tcSpawnPlayer.start();
                    
                // TODO: hints don't work anymore
                // // reset nodes (otherwise they refer to incorrect blocks)
                // for (int i=0; i<hintNodes.length; i++) {
                //     hintNodes[i] = null;
                // }
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

    private static void resetMazeGraphics() {
        
        if (!animationsFinished) {
            return;
        }
        
        textNextLevel.setVisible(false);

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

        if (!animationsFinished) {
            return;
        }

        window.sprites.setVisible(false);
        
        // reset old hint blocks
        for (Node n : hintNodes) {
            if (n != null) {
                
                if (maze.get(n) != Node.Type.BLOCKED && !n.equals(maze.currentNode)) {
                    mazeBlocks[n.Y][n.X].setBackground(Config.BLOCK_COLORS.get(Node.Type.GROUND));
                }
            }
        }

        if (Config.newHintMax) {
            hintNodes = new Node[Config.hintMax];
        }

        // gets solution based on current node
        LinkedList<Node> path;

        if (Config.hintTypeLongest) {
            path = MazeSolver.findLongestPath();
        }
        else {
            path = MazeSolver.findShortestPath();
        }

        int i = 0;
        for (int hint=1; hint<=Config.hintMax && hint<path.size()-1; hint++) {
            Node step = path.get(hint);

            mazeBlocks[step.Y][step.X].setBackground(Config.HINT_COLOR);
            hintNodes[i] = step;
            i++;
        }

        window.sprites.setVisible(true);
        
    }

    public static void tryToMove(KeyHandler.ActionKey action) {

        if (maze.complete || !animationsFinished) {
            return;
        }

        Node lastNode = maze.currentNode;

        if (maze.userMove(action)) {

            player.move(action);
            mirrorPlayer(action);
            refreshBlockGraphics(lastNode);

            if (maze.complete) {
                textNextLevel.setVisible(true);
            }
        }

    }

    public static void setupKeyCallbacks() {

        // TODO: add listener when creating new instance of Window
        KeyHandler listener = new KeyHandler();
        window.addKeyListener(listener);
        
        KeyHandler.ActionKey.MOVE_UP.setCallback(() -> { tryToMove(KeyHandler.ActionKey.MOVE_UP); });
        KeyHandler.ActionKey.MOVE_DOWN.setCallback(() -> { tryToMove(KeyHandler.ActionKey.MOVE_DOWN); });
        KeyHandler.ActionKey.MOVE_LEFT.setCallback(() -> { tryToMove(KeyHandler.ActionKey.MOVE_LEFT); });
        KeyHandler.ActionKey.MOVE_RIGHT.setCallback(() -> { tryToMove(KeyHandler.ActionKey.MOVE_RIGHT); });
        
        KeyHandler.ActionKey.MAZE_NEW.setCallback(() -> { generateNewMaze(); });
        KeyHandler.ActionKey.MAZE_RESET.setCallback(() -> { resetMazeGraphics(); });
        KeyHandler.ActionKey.MAZE_HINT.setCallback(() -> { showHint(); });
        
        KeyHandler.ActionKey.MAZE_STEP_UNDO.setCallback(() -> {
            if (tcReset.finished) {
               step(-1);
            }
        });

        KeyHandler.ActionKey.MAZE_STEP_REDO.setCallback(() -> {
            if (tcReset.finished) {
                step(1);
            }
        });
            
        KeyHandler.ActionKey.ZOOM_IN.setCallback(() -> { zoom(1); });
        KeyHandler.ActionKey.ZOOM_OUT.setCallback(() -> { zoom(-1); });
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
                refreshBlockPosition(x, y);
            }
        }
    }

    private static boolean playerMustMove(Maze.Direction dir) {

        if (maze.currentNeighborWalkable(dir)) {

            for (Maze.Direction d : Maze.Direction.values()) {
                if (d != dir && maze.currentNeighborWalkable(d)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static void mirrorPlayer(KeyHandler.ActionKey action) {

        if (action == null) {
            player.setMirrored(!maze.currentNeighborWalkable(Maze.Direction.LEFT));
        }

        else if (playerMustMove(Maze.Direction.LEFT)) {
            player.setMirrored(false);
        }
        else if (playerMustMove(Maze.Direction.RIGHT)) {
            player.setMirrored(true);
        }
        else if (action == KeyHandler.ActionKey.MOVE_LEFT) {
            player.setMirrored(false);
        }
        else if (action == KeyHandler.ActionKey.MOVE_RIGHT) {
            player.setMirrored(true);
        }
        
    }

    public static void step(int direction) {
        
        // TODO: doubles still sometimes disappear
        // 2x, 2x, stepback, stepback --> 2nd 2x disappears

        if (tcReset.finished && (maze.complete || !animationsFinished)) {
            return;
        }

        Node lastNode = maze.currentNode;

        KeyHandler.ActionKey action = maze.step(direction);

        if (action != null) {
            player.move(action);
            mirrorPlayer(action);
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

        for (Node neighbor : maze.getNeighborsOf(node)) {
            boolean nodeCausesFrost = maze.get(node) == Node.Type.DOUBLE || maze.get(node) == Node.Type.END_DOUBLE;
            mazeBlocks[neighbor.Y][neighbor.X].setFrost(neighbor, node, nodeCausesFrost);
        }
        
    }

    public static void newBlockColors() {

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                mazeBlocks[y][x].setBackground(Config.BLOCK_COLORS.get(maze.get(x, y)));
            }
        }
    }

    private static void setMazeStartCoords() {
        Config.mazeStartX = (Window.width - Config.blockSize * maze.width) / 2;
        Config.mazeStartY = (Window.height - Config.blockSize * maze.height) / 2;
    }

    public static void newMazeGraphics() {

        if (!animationsFinished) {
            return;
        }

        textNextLevel.setVisible(false);
        
        boolean newSize = maze.height != oldMaze.height || maze.width != oldMaze.width;

        if (newSize) {
            // remove old blocks from canvas
            for (Block[] row : mazeBlocks) {
                for (Block spr : row) {
                    window.sprites.remove(spr);
                }
            }

            oldMaze = new Maze(maze.width, maze.height, Node.Type.WALL);
            createWallBlocks();
            player.setVisible(false);
        }
        
        window.sprites.setVisible(true);

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
            player.setVisible(true);
            movePlayerGraphicsTo(maze.currentNode);
            mirrorPlayer(null);

            for (Node node : nodesToChange) {
                refreshBlockGraphics(node);
            }
        }

    }

    public static void generateNewMaze() {
        
        if (!animationsFinished) {
            return;
        }

        // new maze in 2D-array-form
        oldMaze = maze;
        maze = MazeGen.generate();

        System.out.println(maze.creationPath);
        MazePrinter.printMazeWithPath(maze, maze.creationPath);
       
        newMazeGraphics();
    }

    private static void refreshBlockPosition(int x, int y) {
        Block block = mazeBlocks[y][x];
        int posX = Config.mazeStartX + x * Config.blockSize;
        int posY = Config.mazeStartY + y * Config.blockSize;
        block.setLocation(posX, posY);
    }

    private static void createWallBlocks() {

        mazeBlocks = new Block[maze.height][maze.width];
        
        setMazeStartCoords();

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                Block block = new Block("src/textures/wall.png", Config.blockSize);
                mazeBlocks[y][x] = block;
                refreshBlockPosition(x, y);
            }
        }

    }

}
