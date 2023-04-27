import java.awt.Color;
import java.util.HashSet;
import java.util.Hashtable;

public class Main {

    public static final int BLOCK_SIZE = 35;
    public static final int BORDER_WIDTH = 4;

    public static final int BLOCK_WALL = 0;
    public static final int BLOCK_ICE = 1;
    public static final int BLOCK_START = 2;
    public static final int BLOCK_END = 3;

    public static MazeGenerator mazeGenerator = new MazeGenerator(10);
    
    public static int mazeStartX;
    public static int mazeStartY;

    public static Window window;
    public static Sprite player;

    public static Node currentNode;
    
    public static final Hashtable<Integer, Color> COLOR_TABLE = new Hashtable<>();

    public static HashSet<Sprite> mazeSprites = new HashSet<>();

    public static void main(String[] args) {

        COLOR_TABLE.put(BLOCK_WALL, new Color(50, 50, 50));
        COLOR_TABLE.put(BLOCK_ICE, new Color(225, 225, 255));
        COLOR_TABLE.put(BLOCK_START, Color.GREEN);
        COLOR_TABLE.put(BLOCK_END, Color.MAGENTA);

        window = new Window();
        
        // Allow sprites to reference the window, without having to
        // save the reference to every instance of Sprite.
        Sprite.window = window;

        // coordinates of topleft of maze
        mazeStartX = (Window.width - BLOCK_SIZE * mazeGenerator.maze[0].length) / 2;
        mazeStartY = (Window.height - BLOCK_SIZE * mazeGenerator.maze.length) / 2;

        player = new Sprite(BLOCK_SIZE-2*BORDER_WIDTH, BLOCK_SIZE-2*BORDER_WIDTH, Color.ORANGE, BLOCK_SIZE);
        player.addBorder(2, Color.BLACK);

        setupKeyCallbacks();

        drawNewMaze();
        
    }

    public static void tryMoveHorizontally(String action) {
        boolean moveLeft = false;
        boolean moveRight = false;
        int dx;

        if (action == "left") {
            dx = -1;
            moveLeft = currentNode.x > 0;
        }
        else {
            dx = 1;
            moveRight = currentNode.x < mazeGenerator.maze[0].length-1;
        }

        Node newNode = new Node(currentNode.x + dx, currentNode.y);

        if (moveLeft || moveRight) {

            if (mazeGenerator.maze[newNode.y][newNode.x] != BLOCK_WALL) {
                player.move(action);
                currentNode = newNode;
            }

        }
    }

    public static void tryMoveVertically(String action) {

        boolean moveUp = false;
        boolean moveDown = false;
        int dy;

        if (action == "up") {
            dy = -1;
            moveUp = currentNode.y > 0;
        }
        else {
            dy = 1;
            moveDown = currentNode.y < mazeGenerator.maze.length-1;
        }

        Node newNode = new Node(currentNode.x, currentNode.y + dy);

        if (moveUp || moveDown) {
    
            if (mazeGenerator.maze[newNode.y][newNode.x] != BLOCK_WALL) {
                player.move(action);
                currentNode = newNode;
            }
        }
    }

    public static void setupKeyCallbacks() {

        KeyListen listener = new KeyListen();
        window.addKeyListener(listener);
        
        listener.addCallback("up", () -> { tryMoveVertically("up"); });
        
        listener.addCallback("down", () -> { tryMoveVertically("down"); });

        listener.addCallback("left", () -> { tryMoveHorizontally("left"); });
        listener.addCallback("right", () -> { tryMoveHorizontally("right"); });
        
        
        listener.addCallback("test", () -> {drawNewMaze();});

    }

    public static void drawNewMaze() {

        // remove all old sprites
        for (Sprite sprite : mazeSprites) {
            window.removeSprite(sprite);
        }

        mazeGenerator = new MazeGenerator(10);
        mazeGenerator.generateMaze();
        System.out.println(mazeGenerator.printRepresentation());

        // get solution
        Node startNode = mazeGenerator.getStartNode(); 
        Node endNode = mazeGenerator.getEndNode(); 
        CalculateSolution sol = new CalculateSolution(mazeGenerator.maze, startNode, endNode);

        System.out.println("\nCorrect path:");
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
                block.addBorder(BORDER_WIDTH, Color.BLACK);
                mazeSprites.add(block);

                // move block
                int xPos = mazeStartX + x * BLOCK_SIZE;
                int yPos = mazeStartY + y * BLOCK_SIZE;
                block.moveTo(xPos, yPos);

            }
        }
    }

}
