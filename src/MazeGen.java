/*

* maze ends only at edge of maze or when colliding with a path node
* no dead ends



*/
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

public class MazeGen {
 
    public Stack<Node> path = new Stack<>();

    // keep track of the user's path, in order to be able to backtrack
    private Stack<Node> userPath = new Stack<>();
    private Stack<Node> userUndos = new Stack<>();

    // keep track of doubles, only to know where they were when resetting
    private ArrayList<Node> doubles = new ArrayList<>();
    
    private Random rand = new Random();
    private Node.Type[][] maze;
    
    public int width;
    public int height;
    
    public Node startNode;
    public Node endNode;
    
    // used to generate the maze as well as keep track of the player
    public Node currentNode;

    private final int[] DIR = new int[] {1, -1};

    private int currentDirX = getRandomDirection();
    private int currentDirY = getRandomDirection();

    private final float CHANCE_NEXT_NODE_SAME_DIR = 0.5f;       // NOTE: moving right and then down, the chance of moving right is not 50/50 but the same as before moving down
    private final float CHANCE_NEXT_NODE_WALL = 0.10f;
    private final float CHANCE_NEXT_NODE_DOUBLE = 0.25f;

    private int minPathLength = 100;

    public static void main(String[] args) {
        MazeGen mg = new MazeGen(3, 1);
        mg.generate();
        mg.printMazeWithPath();
        mg.printMazeWithTypes();
    }

    public MazeGen(int w, int h) {
        width = w;
        height = h;

        // prevent maze generation from taking too long
        if (minPathLength > width * height * 0.9 ) {
            minPathLength = (int) (width * height * 0.5);
        }
    }

    // mark as walked
    public Node.Type leaveNode(Node node) {
        
        Node.Type type;

        if (get(node) == Node.Type.DOUBLE) {
            type = Node.Type.GROUND;

        }
        else {
            type = Node.Type.BLOCKED;
        }
        
        set(node, type);
        userPath.add(node);

        return type;
    }

    public void set(Node node, Node.Type type) {
        maze[node.y][node.x] = type;
    }
    
    public void set(int x, int y, Node.Type type) {
        maze[y][x] = type;
    }

    public Node.Type get(Node node) {
        return maze[node.y][node.x];
    }

    public Node.Type get(int x, int y) {
        return maze[y][x];
    }

    // TEMP
    public boolean pointOnGrid(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public void reset() {
        
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                
                if (get(x, y) == Node.Type.BLOCKED) {
                    set(x, y, Node.Type.GROUND);
                }
            }
        }

        // account for nodes that were just set to ground
        // even though they should be double
        for (Node n : doubles) {
            set(n, Node.Type.DOUBLE);
        }
    }

    public void resetNode(Node node) {

        if (get(node) == Node.Type.BLOCKED) {
            set(node, Node.Type.GROUND);
        }

        else {

            for (Node n : doubles) {
                if (n.same(node)) {
                    set(node, Node.Type.DOUBLE);
                    break;
                }
            }
        }
    }

    public void step(int direction) {
        resetNode(currentNode);
        
        if (direction == -1) {
            currentNode = userPath.pop();
            userUndos.add(currentNode);
        }
        else {
            currentNode = userUndos.pop();
            userPath.add(currentNode);
        }
    }

    public void generate() {

        // generate a new maze until the path is >= minPathLength

        int count = 0;

        do {

            // reset maze
            maze = new Node.Type[height][width];
            path.clear();
            doubles.clear();

            // set random start
            int startX = rand.nextInt(width);
            int startY = rand.nextInt(height);
            startNode = new Node(startX, startY);

            // set start node and add to path
            set(startNode, Node.Type.START);
            path.add(startNode);        
            
            currentNode = startNode;

            while (getNextNode()) {
                Node.Type nextType = getNextNodeType();
                
                set(currentNode, nextType);
                
                if (nextType == Node.Type.WALL) {
                    // take a step back (note that currentNode it never added to path here)
                    currentNode = path.lastElement();
                }
                else {
                    // only add currentNode if it is walkable
                    path.add(currentNode);
                }
            }

            // change default from null to Node.Type.WALL
            for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    if (get(x, y) == null) {
                        set(x, y, Node.Type.WALL);
                    }
                }
            }

            // prevent endNode from ending up right next to startNode
            if (currentNode.nextTo(startNode) && path.size() > 2) {
                currentNode = path.pop();
            }
            
            endNode = currentNode;

            maze[startNode.y][startNode.x] = Node.Type.START;
            maze[endNode.y][endNode.x] = Node.Type.END;

            count++;
        }

        while (path.size() < minPathLength);

        System.out.println("mazes generated: " + count);
    }
    
    // TODO: prevent creating double in corner
    private Node.Type getNextNodeType() {
        
        if (path.size() > 1) {
            // this check prevents creating walls all around the first node

            double chance = rand.nextDouble();
            
            if (chance < CHANCE_NEXT_NODE_DOUBLE) {
                doubles.add(currentNode);
                return Node.Type.DOUBLE;
            }

            else if (chance < CHANCE_NEXT_NODE_WALL) {
                return Node.Type.WALL;
            }
        }
            
        return Node.Type.GROUND;
    }

    private boolean getNextNode() {

        boolean tryDxFirst = getRandomDirection() == 1;

        if (tryDxFirst) {
            if (!tryMoveDx()) {
                return tryMoveDy();
            }
            return true;
        }

        else {
            if (!tryMoveDy()) {
                return tryMoveDx();
            }
            return true;
        }
    }

    private int getRandomDirection() {
        return DIR[rand.nextInt(2)];
    }

    private boolean tryMoveDx() {
        
        int dx;
        
        double chance = rand.nextDouble();
        if (chance < CHANCE_NEXT_NODE_SAME_DIR) {
            dx = currentDirX;
        }
        else {
            dx = currentDirX * -1;
            dx = currentDirX;
        }
        
        if (!validMove(dx, 0)) {
            dx *= -1;
            currentDirX = dx;
            if (!validMove(dx, 0)) {
                return false;
            }
        }

        currentNode = new Node(currentNode.x + dx, currentNode.y);
        return true;
    }

    private boolean tryMoveDy() {
        
        int dy;
        
        double chance = rand.nextDouble();
        if (chance < CHANCE_NEXT_NODE_SAME_DIR) {
            dy = currentDirY;
        }
        else {
            dy = currentDirY * -1;
            currentDirY = dy;
        }

        if (!validMove(0, dy)) {
            dy *= -1;
            currentDirY = dy;
            if (!validMove(0, dy)) {
                return false;
            }
        }
        
        currentNode = new Node(currentNode.x, currentNode.y + dy);
        return true;
    }

    public boolean nodeWalkable(int x, int y) {
        return get(x, y) != Node.Type.WALL && get(x, y) != Node.Type.BLOCKED;
    }

    private boolean validMove(int dx, int dy) {
        
        // within horizontal width
        int newX = currentNode.x + dx;
        if (newX < 0 || newX >= width) {
            return false;
        }
        
        // within vertical width
        int newY = currentNode.y + dy;
        if (newY < 0 || newY >= height) {
            return false;
        }
        
        // node already set
        if (get(newX, newY) != null) {
            return false;
        }

        return true;
    }

    public Node.Type[][] getMaze() {
        return maze;
    }

    public String getFormatted(String str) {
        // the number in the format should be one larger than
        // the longest strRep of Node.Type
        // TODO: get length without looping through
        // Node.Type.values() every call.
        return String.format("%3s", str);
    }

    public void printMazeWithPath() {
        
        String[][] mazeOfStr = getMazeWithTypes();

        // set numbers that represent the order of steps of the (a) solution
        for (int i=0; i<path.size(); i++) {
            Node n = path.get(i);
            mazeOfStr[n.y][n.x] = getFormatted(String.valueOf(i));
        }

        System.out.println(mazeOfStrToStr(mazeOfStr));
    }

    public void printMazeWithTypes() {
        System.out.println(mazeOfStrToStr(getMazeWithTypes()));
    }

    // convert a 2d-array to a string
    private String mazeOfStrToStr(String[][] m) {

        StringBuilder sb = new StringBuilder();

        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                sb.append(m[y][x]);
            }
            sb.append('\n');
        }
    
        return sb.toString();
    }
    
     // generate a maze of string representations of the nodes
    private String[][] getMazeWithTypes() {
        
        String[][] mazeOfStr = new String[height][width];

        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                mazeOfStr[y][x] = getFormatted(maze[y][x].strRep);
            }
        }
    
        return mazeOfStr;
    }

}
