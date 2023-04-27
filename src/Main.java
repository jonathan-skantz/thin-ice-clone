import java.awt.Color;
import java.util.Hashtable;

public class Main {

    public static final int BLOCK_SIZE = 35;
    public static final int BORDER_WIDTH = 4;

    public static final int BLOCK_WALL = 0;
    public static final int BLOCK_ICE = 1;
    public static final int BLOCK_START = 2;
    public static final int BLOCK_END = 3;
    public static final int BLOCK_BLOCKED = 4;

    public static final int DIMENSION = 10;
    
    public static MazeGenerator mazeGenerator = new MazeGenerator(DIMENSION);
    
    public static int mazeStartX;
    public static int mazeStartY;

    public static Window window;
    public static Sprite player;

    public static Node currentNode;
    // public static Node nodeLast;
    
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
        mazeStartX = (Window.width - BLOCK_SIZE * mazeGenerator.maze[0].length) / 2;
        mazeStartY = (Window.height - BLOCK_SIZE * mazeGenerator.maze.length) / 2;

        player = new Sprite(BLOCK_SIZE-2*BORDER_WIDTH, BLOCK_SIZE-2*BORDER_WIDTH, Color.ORANGE, BLOCK_SIZE);
        player.setBorder(2, Color.BLACK, true);

        setupKeyCallbacks();

        drawNewMaze();
        
    }

    public static void tryMoveToNode(Node newNode, String action) {

        if (mazeGenerator.pointOnGrid(newNode.x, newNode.y)) {
            
            int blockType = mazeGenerator.maze[newNode.y][newNode.x];
            
            System.out.println(blockType);

            if (blockType != BLOCK_WALL && blockType != BLOCK_BLOCKED) {
                player.move(action);
                mazeGenerator.maze[currentNode.y][currentNode.x] = BLOCK_BLOCKED;
                mazeSprites[currentNode.y][currentNode.x].setColor(COLOR_TABLE.get(BLOCK_BLOCKED));
                
                currentNode = newNode;
                
            }
        }
    }

    public static void tryMoveHorizontally(String action) {
        int dx = action == "left" ? -1 : 1;

        Node newNode = new Node(currentNode.x + dx, currentNode.y);
        tryMoveToNode(newNode, action);
    }
    
    public static void tryMoveVertically(String action) {
        int dy = action == "up" ? -1 : 1;
        
        Node newNode = new Node(currentNode.x, currentNode.y + dy);
        tryMoveToNode(newNode, action);
    }

    public static void setupKeyCallbacks() {

        KeyHandler listener = new KeyHandler();
        window.addKeyListener(listener);
        
        KeyHandler.ActionKey.UP.setCallback(() -> { player.move(KeyHandler.ActionKey.UP); });
        KeyHandler.ActionKey.DOWN.setCallback(() -> { player.move(KeyHandler.ActionKey.DOWN); });
        KeyHandler.ActionKey.LEFT.setCallback(() -> { player.move(KeyHandler.ActionKey.LEFT); });
        KeyHandler.ActionKey.RIGHT.setCallback(() -> { player.move(KeyHandler.ActionKey.RIGHT); });

    }

    public static void drawNewMaze() {

        // remove all old sprites
        for (Sprite[] spriteRow : mazeSprites) {
            for (Sprite sprite : spriteRow) {
                window.removeSprite(sprite);
            }
        }

        mazeGenerator = new MazeGenerator(10);
        mazeGenerator.generateMaze();
        System.out.println(mazeGenerator.printRepresentation());

        // get solution
        Node startNode = mazeGenerator.getStartNode(); 
        Node endNode = mazeGenerator.getEndNode(); 
        CalculateSolution sol = new CalculateSolution(mazeGenerator.maze, startNode, endNode);

        System.out.println("Shortest path:");
        for (Node node : sol.findShortestPath()) {
            System.out.print("(" + node.x + "," + node.y + ") ");
        }

        // move player to center of start node
        int centeredX = mazeStartX + startNode.x * BLOCK_SIZE + (BLOCK_SIZE - player.canvas.getWidth()) / 2;
        int centeredY = mazeStartY + startNode.y * BLOCK_SIZE + (BLOCK_SIZE - player.canvas.getHeight()) / 2;
        player.moveTo(centeredX, centeredY);

        currentNode = startNode;

        // create a new sprite for every block in the maze
        for (int y=0; y<mazeGenerator.maze.length; y++) {
            for (int x=0; x<mazeGenerator.maze[y].length; x++) {
                
                Color color = COLOR_TABLE.get(mazeGenerator.maze[y][x]);
                
                // create block
                Sprite block = new Sprite(BLOCK_SIZE, BLOCK_SIZE, color, 0);
                block.setBorder(BORDER_WIDTH, Color.BLACK, true);
                mazeSprites[y][x] = block;

                // move block
                int xPos = mazeStartX + x * BLOCK_SIZE;
                int yPos = mazeStartY + y * BLOCK_SIZE;
                block.moveTo(xPos, yPos);

            }
        }
    }

}
