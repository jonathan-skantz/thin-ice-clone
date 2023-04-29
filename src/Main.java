import java.util.Hashtable;

import java.awt.Color;
import java.awt.Font;

public class Main {

    public static final int BLOCK_SIZE = 35;
    public static final int BORDER_WIDTH = 2;
    public static final Color BORDER_COLOR = new Color(0, 0, 0, 50);

    public static final int BLOCK_WALL = 0;
    public static final int BLOCK_ICE = 1;
    public static final int BLOCK_START = 2;
    public static final int BLOCK_END = 3;
    public static final int BLOCK_BLOCKED = 4;

    public static final int DIMENSION = 10;
    
    public static MazeGenerator mazeGenerator;
    
    public static int mazeStartX;
    public static int mazeStartY;

    public static Window window;
    public static Sprite player;
    public static Sprite textNextLevel;

    public static Node currentNode;

    public static boolean mazeCompleted = false;
    
    public static final Hashtable<Integer, Color> COLOR_TABLE = new Hashtable<>();

    public static Sprite[][] mazeSprites = new Sprite[DIMENSION][DIMENSION];

    public static void main(String[] args) {

        COLOR_TABLE.put(BLOCK_WALL, new Color(50, 50, 50));
        COLOR_TABLE.put(BLOCK_ICE, new Color(225, 225, 255));
        COLOR_TABLE.put(BLOCK_START, Color.GREEN);
        COLOR_TABLE.put(BLOCK_END, Color.MAGENTA);
        COLOR_TABLE.put(BLOCK_BLOCKED, new Color(0, 50, 255));

        window = new Window();
        
        // Allow sprites to reference the window, without having to
        // save the reference to every instance of Sprite.
        Sprite.window = window;
        
        // coordinates of topleft of maze
        mazeStartX = (Window.width - BLOCK_SIZE * DIMENSION) / 2;
        mazeStartY = (Window.height - BLOCK_SIZE * DIMENSION) / 2;
        
        // create player
        int size = BLOCK_SIZE - 2 * BORDER_WIDTH;
        player = new Sprite(size, size, Color.ORANGE, BLOCK_SIZE);
        player.setBorder(2, Color.BLACK, true);
        
        // create next-level-text
        Font font = new Font("arial", Font.PLAIN, 20);
        textNextLevel = new Sprite("Level complete", font, Color.BLACK);
        textNextLevel.moveTo(textNextLevel.rect.x, Window.height - textNextLevel.rect.height - 25);
        textNextLevel.setVisible(false);

        setupKeyCallbacks();

        generateNewMaze();
        
    }

    public static void testMazeCompleted() {

        Node endNode = mazeGenerator.getEndNode();

        if (currentNode.x == endNode.x && currentNode.y == endNode.y) {
            mazeCompleted = true;
            textNextLevel.setVisible(true);
        }
    }

    public static void tryMoveToNode(KeyHandler.ActionKey action) {

        if (mazeCompleted) {
            return;
        }

        int dx = 0;
        int dy = 0;

        switch (action) {
            case UP:
                dy = -1;
                break;
            
            case DOWN:
                dy = 1;
                break;
            
            case LEFT:
                dx = -1;
                break;
            
            case RIGHT:
                dx = 1;
                break;
            
            default:
                break;
        }

        Node newNode = new Node(currentNode.x + dx, currentNode.y + dy);

        if (mazeGenerator.pointOnGrid(newNode.x, newNode.y)) {
            
            int blockType = mazeGenerator.maze[newNode.y][newNode.x];
            
            if (blockType != BLOCK_WALL && blockType != BLOCK_BLOCKED) {
                // valid block to move to
                player.move(action);
                mazeGenerator.maze[currentNode.y][currentNode.x] = BLOCK_BLOCKED;
                mazeSprites[currentNode.y][currentNode.x].setBackgroundColor(COLOR_TABLE.get(BLOCK_BLOCKED));
                
                currentNode = newNode;

                testMazeCompleted();
            }
        }
    }

    public static void setupKeyCallbacks() {

        KeyHandler listener = new KeyHandler();
        window.addKeyListener(listener);
        
        KeyHandler.ActionKey.UP.setCallback(() -> { tryMoveToNode(KeyHandler.ActionKey.UP); });
        KeyHandler.ActionKey.DOWN.setCallback(() -> { tryMoveToNode(KeyHandler.ActionKey.DOWN); });
        KeyHandler.ActionKey.LEFT.setCallback(() -> { tryMoveToNode(KeyHandler.ActionKey.LEFT); });
        KeyHandler.ActionKey.RIGHT.setCallback(() -> { tryMoveToNode(KeyHandler.ActionKey.RIGHT); });
        
        KeyHandler.ActionKey.MAZE_NEW.setCallback(() -> { 
            
            // prevent making new mazes unless the current is solved
            if (mazeCompleted) {
                generateNewMaze();

            }
        });

        KeyHandler.ActionKey.MAZE_RESET.setCallback(() -> { resetMaze(); });
        
    }
    
    public static void resetPlayer() {
        
        Node startNode = mazeGenerator.getStartNode(); 
        
        // move player to center of start node
        int centeredX = mazeStartX + startNode.x * BLOCK_SIZE + (BLOCK_SIZE - player.canvas.getWidth()) / 2;
        int centeredY = mazeStartY + startNode.y * BLOCK_SIZE + (BLOCK_SIZE - player.canvas.getHeight()) / 2;
        player.moveTo(centeredX, centeredY);
        
        currentNode = startNode;
    }
    
    public static void resetMaze() {
        
        mazeCompleted = false;
        textNextLevel.setVisible(false);
        
        boolean firstMaze = mazeSprites[0][0] == null;

        // create a new sprite for every block in the maze
        for (int y=0; y<mazeGenerator.maze.length; y++) {
            for (int x=0; x<mazeGenerator.maze[y].length; x++) {
                
                // reset color and node value
                Color color = COLOR_TABLE.get(mazeGenerator.maze[y][x]);

                if (firstMaze) {
                    makeBlock(x, y, color);
                }
                
                else if (color == COLOR_TABLE.get(BLOCK_BLOCKED)) {
                    // reaches here when resetting
                    mazeGenerator.maze[y][x] = BLOCK_ICE;
                    mazeSprites[y][x].setBackgroundColor(COLOR_TABLE.get(BLOCK_ICE));
                }

                else {
                    // reaches here when generating new maze
                    mazeSprites[y][x].setBackgroundColor(color);
                }

            }
        }
        Node startNode = mazeGenerator.getStartNode();
        mazeGenerator.maze[startNode.y][startNode.x] = BLOCK_START;
        mazeSprites[startNode.y][startNode.x].setBackgroundColor(COLOR_TABLE.get(BLOCK_START));

        resetPlayer();
    }

    public static void generateNewMaze() {
        
        // new maze in 2D-array-form
        mazeGenerator = new MazeGenerator(DIMENSION);
        mazeGenerator.generateMaze();
        System.out.println(mazeGenerator.printRepresentation());

        // get solution
        // Node startNode = mazeGenerator.getStartNode(); 
        // Node endNode = mazeGenerator.getEndNode(); 
        // CalculateSolution sol = new CalculateSolution(mazeGenerator.maze, startNode, endNode);

        // System.out.println("Shortest path:");
        // for (Node node : sol.findShortestPath()) {
        //     System.out.print("(" + node.x + "," + node.y + ") ");
        // }

        resetMaze();

    }

    public static void makeBlock(int x, int y, Color color) {

        // create block
        Sprite block = new Sprite(BLOCK_SIZE, BLOCK_SIZE, color, 0);
        block.setBorder(BORDER_WIDTH, BORDER_COLOR, true);
        mazeSprites[y][x] = block;
        
        // move block
        int xPos = mazeStartX + x * BLOCK_SIZE;
        int yPos = mazeStartY + y * BLOCK_SIZE;
        block.moveTo(xPos, yPos);
    }

    // public static void repaintBlocks() {

    //     boolean firstMaze = mazeSprites[0][0] == null;
        
    //     // create a new sprite for every block in the maze
    //     for (int y=0; y<mazeGenerator.maze.length; y++) {
    //         for (int x=0; x<mazeGenerator.maze[y].length; x++) {
                
    //             Color color = COLOR_TABLE.get(mazeGenerator.maze[y][x]);
                
    //             if (firstMaze) {
    //                 // create block
    //                 Sprite block = new Sprite(BLOCK_SIZE, BLOCK_SIZE, color, 0);
    //                 block.setBorder(BORDER_WIDTH, BORDER_COLOR, true);
    //                 mazeSprites[y][x] = block;
                    
    //                 // move block
    //                 int xPos = mazeStartX + x * BLOCK_SIZE;
    //                 int yPos = mazeStartY + y * BLOCK_SIZE;
    //                 block.moveTo(xPos, yPos);
    //             }
    //             else {
    //                 mazeSprites[y][x].setBackgroundColor(color);
    //             }
                
    //         }
    //     }

    //     resetPlayer();
    // }
    
}
