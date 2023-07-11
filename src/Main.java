import java.util.LinkedList;

import javax.swing.JLabel;
import javax.swing.border.Border;

import java.awt.Color;
import java.awt.Font;

/**
 * Main class for the game.
 */
public class Main {

    public static Window window = new Window();
    
    private static boolean zoomUpdate = false;

    private static Maze maze;
    
    // sprites
    public static Sprite player;
    public static JLabel textNextLevel;
    public static Sprite[][] mazeSprites;

    public static Node[] hintNodes = new Node[Config.hintMax];

    public static void main(String[] args) {

        Config.apply();

        setupKeyCallbacks();

        window.sprites.setVisible(false);   // prevents redrawing while setting up
        
        // create player
        int size = Config.blockSize - 2 * Config.BLOCK_BORDER_WIDTH;
        player = new Sprite(size, size, Color.ORANGE, Config.blockSize);
        player.setBorder(Config.BLOCK_BORDER);
        
        // setup next-level-text
        Font font = new Font("arial", Font.PLAIN, 20);
        textNextLevel = new JLabel("Level complete");
        textNextLevel.setFont(font);
        textNextLevel.setForeground(Color.BLACK);
        textNextLevel.setSize(textNextLevel.getPreferredSize());
        
        int y = 50;
        int x = (Window.width - textNextLevel.getWidth()) / 2;
        textNextLevel.setLocation(x, y);
        window.sprites.add(textNextLevel);
        
        UI.setupConfigs();

        // generate maze and reset graphics
        generateNewMaze();
    }

    public static void showHint() {

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

        if (maze.complete) {
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
        
        KeyHandler.ActionKey.MAZE_NEW.setCallback(() -> { 
            
            // prevent making new mazes unless the current is solved
            // if (maze.complete) {
                generateNewMaze();
                // }
            });
            
            KeyHandler.ActionKey.MAZE_RESET.setCallback(() -> { 
                maze.reset();
                resetMazeGraphics();
            });
            
        KeyHandler.ActionKey.MAZE_HINT.setCallback(() -> { showHint(); });
        
        KeyHandler.ActionKey.MAZE_STEP_UNDO.setCallback(() -> { step(-1); });
        KeyHandler.ActionKey.MAZE_STEP_REDO.setCallback(() -> { step(1); });
            
        KeyHandler.ActionKey.ZOOM_IN.setCallback(() -> { zoom(1); });
        KeyHandler.ActionKey.ZOOM_OUT.setCallback(() -> { zoom(-1); });
    }
    
    public static void zoom(int direction) {
        int ch = 5;
        if (direction == -1) {
            ch *= -1;
        }

        Config.blockSize += ch;
        player.setSize(player.getWidth() + ch, player.getHeight() + ch);
        player.velocity += ch;
        
        zoomUpdate = true;
        resetMazeGraphics();
        zoomUpdate = false;
    }

    public static void step(int direction) {
        
        if (maze.complete) {
            return;
        }

        Node lastNode = maze.currentNode;

        KeyHandler.ActionKey action = maze.step(direction);

        if (action != null) {
            player.move(action);

            if (direction == -1) {
                setNewBlockGraphics(maze.currentNode.X, maze.currentNode.Y);
            }

            setNewBlockGraphics(lastNode.X, lastNode.Y);
        }
        
    }

    public static void movePlayerToNode(Node node) {
        
        int blockX = Config.mazeStartX + node.X * Config.blockSize;
        int blockY = Config.mazeStartY + node.Y * Config.blockSize;
        
        int centeredX = blockX + (Config.blockSize - player.getWidth()) / 2;
        int centeredY = blockY + (Config.blockSize - player.getHeight()) / 2;
        
        player.setLocation(centeredX, centeredY);
    }

    private static void setNewBlockGraphics(int x, int y) {
        Sprite block = mazeSprites[y][x];
        Color color;
        Border border;

        Node node = new Node(x, y);
       
        if (maze.get(node) == Node.Type.DOUBLE) {
            color = Config.BLOCK_COLORS.get(Node.Type.GROUND);
            border = Config.BLOCK_DOUBLE_BORDER;
        }
        else if (maze.get(node) == Node.Type.END_DOUBLE) {
            color = Config.BLOCK_COLORS.get(Node.Type.END);
            border = Config.BLOCK_DOUBLE_BORDER;
        }
        else {
            color = Config.BLOCK_COLORS.get(maze.get(node));
            border = Config.BLOCK_BORDER;
        }

        block.setBackground(color);
        block.setBorder(border);
    }

    public static void newBlockColors() {

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                setNewBlockGraphics(x, y);
            }
        }
    }

    public static void resetMazeGraphics() {
        
        textNextLevel.setVisible(false);
        window.sprites.setVisible(false);
        
        boolean firstMaze = mazeSprites == null;

        if (firstMaze || maze.height != mazeSprites.length || maze.width != mazeSprites[0].length || zoomUpdate) {

            if (!firstMaze) {
                // remove old sprites from canvas
                for (Sprite[] row : mazeSprites) {
                    for (Sprite spr : row) {
                        window.sprites.remove(spr);
                    }
                }
            }
            Config.mazeStartX = (Window.width - Config.blockSize * maze.width) / 2;
            Config.mazeStartY = (Window.height - Config.blockSize * maze.height) / 2;
                
            mazeSprites = new Sprite[maze.height][maze.width];
            firstMaze = true;
        }

        // create a new sprite for every block in the maze
        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                
                // reset color and node value
                Color color = Config.BLOCK_COLORS.get(maze.get(x, y));
                
                if (firstMaze) {
                    makeBlock(x, y, color);
                }
                
                else {
                    // reaches here when generating new maze   
                    setNewBlockGraphics(x, y);
                }

            }
        }

        movePlayerToNode(maze.currentNode);

        if (!zoomUpdate) {
            // don't change on zoomUpdate
            Color color = Config.BLOCK_COLORS.get(Node.Type.START);
            mazeSprites[maze.startNode.Y][maze.startNode.X].setBackground(color);
        }

        // reset nodes (otherwise they refer to incorrect blocks)
        for (int i=0; i<hintNodes.length; i++) {
            hintNodes[i] = null;
        }
        
        window.sprites.setVisible(true);
    }

    public static void generateNewMaze() {
        
        // new maze in 2D-array-form
        maze = MazeGen.generate();
        System.out.println(maze.creationPath);
        MazePrinter.printMazeWithPath(maze, maze.creationPath);

        resetMazeGraphics();
    }

    public static void makeBlock(int x, int y, Color color) {

        // create block
        Sprite block = new Sprite(Config.blockSize, Config.blockSize, color, 0);
        mazeSprites[y][x] = block;

        setNewBlockGraphics(x, y);
        
        // move block
        int xPos = Config.mazeStartX + x * Config.blockSize;
        int yPos = Config.mazeStartY + y * Config.blockSize;
        block.setLocation(xPos, yPos);
    }
    
}
