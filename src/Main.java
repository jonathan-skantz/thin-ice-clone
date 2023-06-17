import java.util.LinkedList;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;

/**
 * Main class for the game.
 */
public class Main {

    public static Window window = new Window();
    
    private static boolean zoomUpdate = false;
    
    // sprites
    public static Sprite player;
    public static JLabel textNextLevel;
    public static Sprite[][] mazeSprites;

    public static Node[] hintNodes = new Node[Config.hintMax];

    public static void main(String[] args) {
        
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
                
                if (MazeGen.get(n) != Node.Type.BLOCKED && !n.same(MazeGen.currentNode)) {
                    mazeSprites[n.y][n.x].setBackground(Config.BLOCK_COLORS.get(Node.Type.GROUND));
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

            mazeSprites[step.y][step.x].setBackground(Config.HINT_COLOR);
            hintNodes[i] = step;
            i++;
        }

        window.sprites.setVisible(true);
        
    }

    public static void tryToMove(KeyHandler.ActionKey action) {

        if (MazeGen.complete) {
            return;
        }

        Node lastNode = MazeGen.currentNode;

        if (MazeGen.userMove(action)) {
            
            player.move(action);
            
            Node.Type lastNodeType = MazeGen.get(lastNode);
            Color color = Config.BLOCK_COLORS.get(lastNodeType);
            mazeSprites[lastNode.y][lastNode.x].setBackground(color);
            
            if (MazeGen.complete) {
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
            // if (mazeGen.complete) {
                generateNewMaze();
                // }
            });
            
            KeyHandler.ActionKey.MAZE_RESET.setCallback(() -> { 
                MazeGen.reset();
                resetMazeGraphics(true);
                resetPlayerGraphics();
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
        resetMazeGraphics(false);
        zoomUpdate = false;
        movePlayerToNode(MazeGen.currentNode);
    }

    public static void step(int direction) {
        
        if (MazeGen.complete) {
            return;
        }

        Node lastNode = MazeGen.currentNode;

        KeyHandler.ActionKey action = MazeGen.step(direction);

        if (action != null) {
            player.move(action);

            Node.Type typeBefore = MazeGen.get(lastNode);
            Color color = Config.BLOCK_COLORS.get(typeBefore);
            Sprite spr;
            if (direction == -1) {
                spr = mazeSprites[MazeGen.currentNode.y][MazeGen.currentNode.x];
            }
            else {
                spr = mazeSprites[lastNode.y][lastNode.x];
                
            }
            spr.setBackground(color);
        }

        
    }

    public static void movePlayerToNode(Node node) {
        
        int blockX = Config.mazeStartX + node.x * Config.blockSize;
        int blockY = Config.mazeStartY + node.y * Config.blockSize;
        
        int centeredX = blockX + (Config.blockSize - player.getWidth()) / 2;
        int centeredY = blockY + (Config.blockSize - player.getHeight()) / 2;
        
        player.setLocation(centeredX, centeredY);
    }

    public static void newBlockColors() {
        for (int y=0; y<MazeGen.getHeight(); y++) {
            for (int x=0; x<MazeGen.getWidth(); x++) {
                Node.Type type = MazeGen.get(x, y);

                mazeSprites[y][x].setBackground(Config.BLOCK_COLORS.get(type));
            }
        }
    }

    public static void resetPlayerGraphics() {
        movePlayerToNode(MazeGen.startNode);
        MazeGen.currentNode = MazeGen.startNode;
    }

    public static void resetMazeGraphics(boolean resetStartNode) {
        
        textNextLevel.setVisible(false);
        window.sprites.setVisible(false);
        
        boolean firstMaze = mazeSprites == null;

        if (firstMaze || MazeGen.getHeight() != mazeSprites.length || MazeGen.getWidth() != mazeSprites[0].length || zoomUpdate) {

            if (!firstMaze) {
                // remove old sprites from canvas
                for (Sprite[] row : mazeSprites) {
                    for (Sprite spr : row) {
                        window.sprites.remove(spr);
                    }
                }
            }
            Config.mazeStartX = (Window.width - Config.blockSize * MazeGen.getWidth()) / 2;
            Config.mazeStartY = (Window.height - Config.blockSize * MazeGen.getHeight()) / 2;
                
            mazeSprites = new Sprite[MazeGen.getHeight()][MazeGen.getWidth()];
            firstMaze = true;
        }

        // create a new sprite for every block in the maze
        for (int y=0; y<MazeGen.getHeight(); y++) {
            for (int x=0; x<MazeGen.getWidth(); x++) {
                
                // reset color and node value
                Color color = Config.BLOCK_COLORS.get(MazeGen.get(x, y));
                
                if (firstMaze) {
                    makeBlock(x, y, color);
                }
                
                else {
                    // reaches here when generating new maze
                    mazeSprites[y][x].setBackground(color);
                }

            }
        }

        if (resetStartNode) {
            // finally set the start node color
            Node startNode = MazeGen.startNode;
            mazeSprites[startNode.y][startNode.x].setBackground(Config.BLOCK_COLORS.get(Node.Type.START));
        }

        // // TODO: why is this needed?
        mazeSprites[MazeGen.endNode.y][MazeGen.endNode.x].setBackground(Config.BLOCK_COLORS.get(Node.Type.END));

        // reset nodes (otherwise they refer to incorrect blocks)
        for (int i=0; i<hintNodes.length; i++) {
            hintNodes[i] = null;
        }
        
        window.sprites.setVisible(true);
    }

    public static void generateNewMaze() {
        
        // new maze in 2D-array-form
        MazeGen.generate();
        MazePrinter.printMazeWithPath(MazeGen.creationPath);

        resetMazeGraphics(true);
        resetPlayerGraphics();
    }

    public static void makeBlock(int x, int y, Color color) {

        // create block
        Sprite block = new Sprite(Config.blockSize, Config.blockSize, color, 0);
        block.setBorder(Config.BLOCK_BORDER);
        mazeSprites[y][x] = block;
        
        // move block
        int xPos = Config.mazeStartX + x * Config.blockSize;
        int yPos = Config.mazeStartY + y * Config.blockSize;
        block.setLocation(xPos, yPos);
    }
    
}
