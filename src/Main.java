import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.Border;

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
    public static Sprite player;
    public static JLabel textNextLevel;
    public static Sprite[][] mazeSprites;
    
    public static Node[] hintNodes = new Node[Config.hintMax];
    
    // animations
    private static final boolean ENABLE_ANIMATIONS = true;
    private static boolean animationsFinished = true;
    private static boolean resetting = false;

    private static TimedCounter tcSpawnPlayer = new TimedCounter(0.5f, 15);
    private static TimedCounter tcNewMaze = new TimedCounter(10);

    public static void main(String[] args) {

        Config.apply();
        
        setupKeyCallbacks();

        window.sprites.setVisible(false);   // prevents redrawing while setting up
        
        // create player
        int size = Config.blockSize - 2 * Config.BLOCK_BORDER_WIDTH;
        player = new Sprite(size, size, Color.ORANGE, Config.blockSize);
        player.setVisible(false);
        player.setBorder(Config.BLOCK_BORDER);
        
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

    public static void showHint() {

        if (!animationsFinished) {
            return;
        }

        window.sprites.setVisible(false);
        
        // reset old hint sprites
        for (Node n : hintNodes) {
            if (n != null) {
                
                if (maze.get(n) != Node.Type.BLOCKED && !n.equals(maze.currentNode)) {
                    mazeSprites[n.Y][n.X].setBackground(Config.BLOCK_COLORS.get(Node.Type.GROUND));
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

            mazeSprites[step.Y][step.X].setBackground(Config.HINT_COLOR);
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
            
            Color color = Config.BLOCK_COLORS.get(maze.get(lastNode));
            Sprite block = mazeSprites[lastNode.Y][lastNode.X];
            
            block.setBackground(color);
            
            if (maze.get(lastNode) == Node.Type.TOUCHED || maze.get(lastNode) == Node.Type.END) {
                block.setBorder(Config.BLOCK_BORDER);
            }
            
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
        
        KeyHandler.ActionKey.MAZE_STEP_UNDO.setCallback(() -> { step(-1); });
        KeyHandler.ActionKey.MAZE_STEP_REDO.setCallback(() -> { step(1); });
            
        KeyHandler.ActionKey.ZOOM_IN.setCallback(() -> { zoom(1); });
        KeyHandler.ActionKey.ZOOM_OUT.setCallback(() -> { zoom(-1); });
    }

    // sets up animation to auto stepback all steps in pathHistory
    private static void resetMazeGraphics() {
        
        if (!animationsFinished) {
            return;
        }

        animationsFinished = false;
        resetting = true;

        int frames = maze.pathHistory.size();

        tcNewMaze.setDuration((float)frames / tcNewMaze.fps);

        tcNewMaze.setCallback(() -> {
            step(-1);
            if (tcNewMaze.frame == tcNewMaze.frames) {
                animationsFinished = true;
                resetting = false;
            }
        });

        tcNewMaze.start();

    }

    public static void zoom(int direction) {

        int ch = 5 * direction;

        Config.blockSize += ch;
        player.velocity += ch;

        setMazeStartCoords();

        if (maze.currentNode != null) {
            player.setSize(player.getWidth() + ch, player.getHeight() + ch);
            movePlayerToNode(maze.currentNode);
        }

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                mazeSprites[y][x].setSize(Config.blockSize, Config.blockSize);
                refreshBlockPosition(x, y);
            }
        }
    }

    public static void step(int direction) {
        
        // TODO: doubles still sometimes disappear
        // 2x, 2x, stepback, stepback --> 2nd 2x disappears

        if (!resetting && (maze.complete || !animationsFinished)) {
            return;
        }

        Node lastNode = maze.currentNode;

        KeyHandler.ActionKey action = maze.step(direction);

        if (action != null) {
            player.move(action);

            if (direction == -1) {
                refreshBlockGraphics(maze.currentNode, false);
            }

            refreshBlockGraphics(lastNode, false);
        }
        
    }

    private static void movePlayerToNode(Node node) {
        
        int blockX = Config.mazeStartX + node.X * Config.blockSize;
        int blockY = Config.mazeStartY + node.Y * Config.blockSize;
    
        int centeredX = blockX + (Config.blockSize - player.getWidth()) / 2;
        int centeredY = blockY + (Config.blockSize - player.getHeight()) / 2;

        player.setLocation(centeredX, centeredY);
    }

    public static void beginPlayerSpawnAnimation(Node node) {

        player.setVisible(true);
        movePlayerToNode(node);

        tcSpawnPlayer.setCallback(() -> {

            // resize
            float progress = (float)tcSpawnPlayer.frame / tcSpawnPlayer.frames;
            int size = (int)((Config.blockSize - 2 * Config.BLOCK_BORDER_WIDTH) * progress);
            player.setSize(size, size);
        
            movePlayerToNode(node);

            if (tcSpawnPlayer.frame == tcSpawnPlayer.frames) {
                animationsFinished = true;
            }
        
        });
        tcSpawnPlayer.start();
    }

    private static Border getBorder(Node node) {
        if (maze.get(node) == Node.Type.DOUBLE || maze.get(node) == Node.Type.END_DOUBLE) {
            return Config.BLOCK_BORDER_DOUBLE;
        }
        return Config.BLOCK_BORDER;
    }

    private static void refreshBlockGraphics(Node node, boolean genBorder) {

        Color color = Config.BLOCK_COLORS.get(maze.get(node));
        Border border = getBorder(node);

        if (genBorder) {
            border = BorderFactory.createCompoundBorder(Config.BLOCK_BORDER_GEN, border);
        }

        mazeSprites[node.Y][node.X].setBackground(color);
        mazeSprites[node.Y][node.X].setBorder(border);

    }

    public static void newBlockColors() {

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                mazeSprites[y][x].setBackground(Config.BLOCK_COLORS.get(maze.get(x, y)));
            }
        }
    }

    private static void setMazeStartCoords() {
        Config.mazeStartX = (Window.width - Config.blockSize * maze.width) / 2;
        Config.mazeStartY = (Window.height - Config.blockSize * maze.height) / 2;
    }



    public static void newMazeGraphics() {
        
        textNextLevel.setVisible(false);
        
        boolean newSize = maze.height != oldMaze.height || maze.width != oldMaze.width;

        if (newSize) {
            // remove old sprites from canvas
            for (Sprite[] row : mazeSprites) {
                for (Sprite spr : row) {
                    window.sprites.remove(spr);
                }
            }

            oldMaze = new Maze(maze.width, maze.height, Node.Type.WALL);
            createWallBlocks();
            player.setVisible(false);
        }
        
        window.sprites.setVisible(true);

        // determine which nodes should change
        ArrayList<Node> nodesToChange = new ArrayList<>();
        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {

                if (maze.get(x, y) == oldMaze.get(x, y) && !(x == maze.currentNode.X && y == maze.currentNode.Y)) {
                    continue;
                }
                nodesToChange.add(new Node(x, y));
            }
        }

        beginMazeAnimation(nodesToChange);

    }

    private static void beginMazeAnimation(ArrayList<Node> nodesToChange) {

        if (!ENABLE_ANIMATIONS) {
            
            if (maze.currentNode != null) {
                player.setVisible(true);
                movePlayerToNode(maze.currentNode);
            }

            for (int y=0; y<maze.height; y++) {
                for (int x=0; x<maze.width; x++) {
                    refreshBlockGraphics(new Node(x, y), false);
                }
            }
            return;
        }

        animationsFinished = false;

        // NOTE: frames is one more in order to remove generation border of last node
        int frames = nodesToChange.size() + 1;

        tcNewMaze.setDuration((float)frames / tcNewMaze.fps);

        tcNewMaze.setCallback(() -> {

            Node node;

            if (tcNewMaze.frame < tcNewMaze.frames) {
                
                node = nodesToChange.get(tcNewMaze.frame-1);
                
                if (tcNewMaze.frame >= 2) {
                    Node lastNode = nodesToChange.get(tcNewMaze.frame-2);
                    refreshBlockGraphics(lastNode, false);
                }
                
                if (node.equals(oldMaze.currentNode)) {
                    player.setVisible(false);
                }

                refreshBlockGraphics(node, true);
            }

            else {

                node = nodesToChange.get(tcNewMaze.frame-2);   // since last node should be changed twice
                refreshBlockGraphics(node, false);
                
                beginPlayerSpawnAnimation(maze.currentNode);
                       
                // TODO: hints don't work anymore
                // // reset nodes (otherwise they refer to incorrect blocks)
                // for (int i=0; i<hintNodes.length; i++) {
                //     hintNodes[i] = null;
                // }
            }
        });

        tcNewMaze.start();
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
        Sprite block = mazeSprites[y][x];
        int posX = Config.mazeStartX + x * Config.blockSize;
        int posY = Config.mazeStartY + y * Config.blockSize;
        block.setLocation(posX, posY);
    }

    private static void createWallBlocks() {

        mazeSprites = new Sprite[maze.height][maze.width];
        
        setMazeStartCoords();

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                Color color = Config.BLOCK_COLORS.get(Node.Type.WALL);
                Sprite block = new Sprite(Config.blockSize, Config.blockSize, color, 0);
                mazeSprites[y][x] = block;
                block.setBorder(Config.BLOCK_BORDER);
                refreshBlockPosition(x, y);
            }
        }

    }

}
