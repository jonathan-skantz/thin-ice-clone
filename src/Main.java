import java.util.Hashtable;
import java.util.LinkedList;

import javax.swing.border.Border;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public class Main {

    public static Window window = new Window();

    // maze config
    public static final int BLOCK_SIZE = 30;
    public static final int DIMENSION_WIDTH = 4;
    public static final int DIMENSION_HEIGHT = 4;

    public static final Hashtable<Node.Type, Color> COLOR_TABLE = new Hashtable<>() {
        {
            put(Node.Type.WALL, new Color(50, 50, 50));
            put(Node.Type.GROUND, new Color(225, 225, 255));
            put(Node.Type.START, Color.GREEN);
            put(Node.Type.END, Color.MAGENTA);
            put(Node.Type.BLOCKED, new Color(0, 50, 255));
            put(Node.Type.DOUBLE, new Color(0, 255, 255));
        }
    };

    // border config
    public static final int BORDER_WIDTH = 1;
    public static final Color BORDER_COLOR = new Color(0, 0, 0, 50);
    public static final Border BORDER = BorderFactory.createLineBorder(BORDER_COLOR, BORDER_WIDTH);

    // hint config
    public static final int HINT_MAX = 3;
    public static final Color HINT_COLOR = new Color(150, 150, 255);

    public static MazeGen mazeGen = new MazeGen(DIMENSION_WIDTH, DIMENSION_HEIGHT);
    
    // coordinates of topleft of maze
    public static final int MAZE_START_X = (Window.width - BLOCK_SIZE * DIMENSION_WIDTH) / 2;
    public static final int MAZE_START_Y = (Window.height - BLOCK_SIZE * DIMENSION_HEIGHT) / 2;

    // sprites
    public static Sprite player;
    public static JLabel textNextLevel;
    public static Sprite[][] mazeSprites = new Sprite[DIMENSION_HEIGHT][DIMENSION_WIDTH];

    public static Node[] hintNodes = new Node[HINT_MAX];


    public static void main(String[] args) {

        setupKeyCallbacks();
        
        window.sprites.setVisible(false);

        // create player
        int size = BLOCK_SIZE - 2 * BORDER_WIDTH;
        player = new Sprite(size, size, Color.ORANGE, BLOCK_SIZE);
        player.setBorder(BORDER);
        
        // setup next-level-text
        Font font = new Font("arial", Font.PLAIN, 20);
        textNextLevel = new JLabel("Level complete");
        window.sprites.add(textNextLevel);

        textNextLevel.setForeground(Color.BLACK);
        textNextLevel.setFont(font);
        Dimension d = textNextLevel.getPreferredSize();
        
        int y = Window.height - 50;
        int x = (Window.width - d.width) / 2;
        textNextLevel.setBounds(x, y, d.width, d.height);
        textNextLevel.setVisible(false);
        

        generateNewMaze();
    }

    public static void showHint() {

        window.sprites.setVisible(false);

        // reset old hint sprites
        for (Node n : hintNodes) {
            if (n != null) {

                if (mazeGen.get(n) != Node.Type.BLOCKED && !n.same(mazeGen.currentNode)) {
                    mazeSprites[n.y][n.x].setBackground(COLOR_TABLE.get(Node.Type.GROUND));
                }
            }
        }

        // get solution based on current node
        CalculateSolution sol = new CalculateSolution(mazeGen);

        LinkedList<Node> path = sol.findShortestPath();
        
        int i = 0;
        for (int hint=1; hint<=HINT_MAX && hint<path.size()-1; hint++) {
            Node step = path.get(hint);

            mazeSprites[step.y][step.x].setBackground(HINT_COLOR);
            hintNodes[i] = step;
            i++;
        }

        window.sprites.setVisible(true);

    }

    public static void tryMoveToNode(int dx, int dy) {

        if (mazeGen.complete) {
            return;
        }

        Node newNode = new Node(mazeGen.currentNode.x + dx, mazeGen.currentNode.y + dy);
        
        // TODO: combine pointOnGrid and nodeTypeWalkable?
        if (mazeGen.pointOnGrid(newNode.x, newNode.y)) {
            
            if (mazeGen.nodeTypeWalkable(newNode)) {
                
                KeyHandler.ActionKey action;

                if (dx == 1) action = KeyHandler.ActionKey.MOVE_RIGHT;
                else if (dx == -1) action = KeyHandler.ActionKey.MOVE_LEFT;
                else if (dy == 1) action = KeyHandler.ActionKey.MOVE_DOWN;
                else action = KeyHandler.ActionKey.MOVE_UP;

                player.move(action);


                Node lastNode = mazeGen.currentNode;
                mazeGen.userMove(dx, dy);

                Node.Type lastNodeType = mazeGen.get(lastNode);
                Color color = COLOR_TABLE.get(lastNodeType);
                mazeSprites[lastNode.y][lastNode.x].setBackground(color);
                
                if (mazeGen.complete) {
                    textNextLevel.setVisible(true);
                }
            }
        }
    }

    public static void setupKeyCallbacks() {

        // TODO: add listener when creating new instance of Window
        KeyHandler listener = new KeyHandler();
        window.addKeyListener(listener);
        
        KeyHandler.ActionKey.MOVE_UP.setCallback(() -> { tryMoveToNode(0, -1); });
        KeyHandler.ActionKey.MOVE_DOWN.setCallback(() -> { tryMoveToNode(0, 1); });
        KeyHandler.ActionKey.MOVE_LEFT.setCallback(() -> { tryMoveToNode(-1, 0); });
        KeyHandler.ActionKey.MOVE_RIGHT.setCallback(() -> { tryMoveToNode(1, 0); });
        
        KeyHandler.ActionKey.MAZE_NEW.setCallback(() -> { 
            
            // prevent making new mazes unless the current is solved
            // if (mazeGen.complete) {
                generateNewMaze();
            // }
        });

        KeyHandler.ActionKey.MAZE_RESET.setCallback(() -> { 
            mazeGen.reset();
            resetGraphics();
        });

        KeyHandler.ActionKey.MAZE_HINT.setCallback(() -> { showHint(); });

        KeyHandler.ActionKey.MAZE_STEP_UNDO.setCallback(() -> { step(-1); });
        KeyHandler.ActionKey.MAZE_STEP_REDO.setCallback(() -> { step(1); });
        
    }
    
    public static void step(int direction) {
        
        if (mazeGen.complete) {
            return;
        }

        Node oldNode = mazeGen.currentNode;

        Node.Type typeBefore = mazeGen.step(direction);

        if (typeBefore == null) {
            return;
        }
        
        if (mazeGen.currentNode.x < oldNode.x) {
            player.move(KeyHandler.ActionKey.MOVE_LEFT);
        }
        else if (mazeGen.currentNode.x > oldNode.x) {
            player.move(KeyHandler.ActionKey.MOVE_RIGHT);
        }
        else if (mazeGen.currentNode.y < oldNode.y) {
            player.move(KeyHandler.ActionKey.MOVE_UP);
        }
        else if (mazeGen.currentNode.y > oldNode.y) {
            player.move(KeyHandler.ActionKey.MOVE_DOWN);
        }

        Color color = COLOR_TABLE.get(typeBefore);
        Sprite spr;
        if (direction == -1) {
            spr = mazeSprites[mazeGen.currentNode.y][mazeGen.currentNode.x];
        }
        else {
            spr = mazeSprites[oldNode.y][oldNode.x];
            
        }
        spr.setBackground(color);
        
    }

    public static void resetPlayer() {
        
        // move player to center of start node
        int blockX = MAZE_START_X + mazeGen.startNode.x * BLOCK_SIZE;
        int blockY = MAZE_START_Y + mazeGen.startNode.y * BLOCK_SIZE;
        
        int centeredX = blockX + (BLOCK_SIZE - player.getWidth()) / 2;
        int centeredY = blockY + (BLOCK_SIZE - player.getHeight()) / 2;
        
        player.setLocation(centeredX, centeredY);
        
        mazeGen.currentNode = mazeGen.startNode;
    }
    
    public static void resetGraphics() {

        textNextLevel.setVisible(false);
        
        boolean firstMaze = mazeSprites[0][0] == null;

        window.sprites.setVisible(false);

        // create a new sprite for every block in the maze
        for (int y=0; y<mazeGen.height; y++) {
            for (int x=0; x<mazeGen.width; x++) {
                
                // reset color and node value
                Color color = COLOR_TABLE.get(mazeGen.get(x, y));

                if (firstMaze) {
                    makeBlock(x, y, color);
                }
                
                else {
                    // reaches here when generating new maze
                    mazeSprites[y][x].setBackground(color);
                }

            }
        }

        // finally set the start node color
        Node startNode = mazeGen.startNode;
        mazeSprites[startNode.y][startNode.x].setBackground(COLOR_TABLE.get(Node.Type.START));

        // // TODO: why is this needed?
        mazeSprites[mazeGen.endNode.y][mazeGen.endNode.x].setBackground(COLOR_TABLE.get(Node.Type.END));

        // reset nodes (otherwise they refer to incorrect blocks)
        for (int i=0; i<hintNodes.length; i++) {
            hintNodes[i] = null;
        }
        resetPlayer();
        
        window.sprites.setVisible(true);
    }

    public static void generateNewMaze() {
        // new maze in 2D-array-form
        mazeGen.generate();
        mazeGen.printMazeWithPath();
        mazeGen.printMazeWithTypes();

        resetGraphics();
    }

    public static void makeBlock(int x, int y, Color color) {

        // create block
        Sprite block = new Sprite(BLOCK_SIZE, BLOCK_SIZE, color, 0);
        block.setBorder(BORDER);
        mazeSprites[y][x] = block;
        
        // move block
        int xPos = MAZE_START_X + x * BLOCK_SIZE;
        int yPos = MAZE_START_Y + y * BLOCK_SIZE;
        block.setLocation(xPos, yPos);
    }
    
}
