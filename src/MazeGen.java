/*

* maze ends only at edge of maze or when colliding with a path node
* no dead ends



*/
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;

public class MazeGen {
 
    public boolean complete = false;       // is set to true when currentNode == endNode

    public LinkedList<Node> creationPath = new LinkedList<>();

    // keep track of the user's path, in order to be able to backtrack
    private Stack<Node> pathHistory = new Stack<>();
    private Stack<Node.Type> pathHistoryTypes = new Stack<>();
    
    private Stack<Node> pathHistoryRedo = new Stack<>();
    // pathHistoryRedoTypes are not necessary to track since when redoing,
    // the system thinks the player is moving just like normal

    // keep track of doubles, only to know where they were when resetting
    private ArrayList<Node> doubles = new ArrayList<>();
    
    private Random rand = new Random();
    public Node.Type[][] maze;
    
    public int width;
    public int height;
    
    public Node startNode;
    public Node endNode;
    
    // used to generate the maze as well as keep track of the player
    public Node currentNode;

    private final int[] DIR = new int[] {1, -1};

    private int currentDirX = getRandomDirection();
    private int currentDirY = getRandomDirection();

    public int minPathLength = 100;
    
    public enum ChanceNextNode {

        SAME_DIR(0.50f),
        WALL(0.10f),
        DOUBLE(0.25f);

        public float chance;

        private ChanceNextNode(float chance) {
            this.chance = chance;
        }

        public boolean evaluate(double randChance) {
            return randChance <= chance;
        }

    }

    public static void main(String[] args) {

        // generate maze
        MazeGen mg = new MazeGen(3, 1);
        mg.generate();

        // print path and tpyes
        System.out.println("Maze with path: ");
        MazePrinter.printMazeWithPath(mg.maze, mg.creationPath);

        System.out.println("Maze with types:");
        MazePrinter.printMazeWithTypes(mg.maze);
    }

    public MazeGen(int w, int h) {
        width = w;
        height = h;

        // prevent maze generation from taking too long
        if (minPathLength > width * height * 0.9 ) {
            minPathLength = (int) (width * height * 0.5);
        }
    }

    public void userMove(int dx, int dy) {
        // NOTE: doesn't check move validity

        Node newNode = new Node(currentNode.x+dx, currentNode.y+dy);
        
        if (get(currentNode) == Node.Type.DOUBLE) {
            set(currentNode, Node.Type.GROUND);
        }
        else {
            set(currentNode, Node.Type.BLOCKED);
        }
        
        pathHistory.add(newNode);
        pathHistoryTypes.add(get(newNode));

        if (pathHistoryRedo.size() > 0) {
            if (pathHistoryRedo.peek().same(newNode)) {
                pathHistoryRedo.pop();
            }
            else {
                // new history is made, clear last record of redos
                pathHistoryRedo.clear();
            }
        }
        
        if ((get(newNode) == Node.Type.END)) {
            complete = true;
        }

        currentNode = newNode;
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
        
        pathHistory.clear();
        pathHistoryTypes.clear();
        pathHistoryRedo.clear();
        
        pathHistory.add(startNode);
        pathHistoryTypes.add(get(startNode));

        complete = false;

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

    public void nodeReset(Node node) {

        if (node.same(startNode)) {
            set(node, Node.Type.START);
        }

        else if (get(node) == Node.Type.BLOCKED) {
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

    // returns previous node's type
    public Node.Type step(int direction){
        
        if (direction == -1) {
            
            if (pathHistory.size() > 1) {
                nodeReset(currentNode);
                
                Node lastNode = pathHistory.pop();      // same as current
                pathHistoryTypes.pop();
                
                pathHistoryRedo.add(lastNode);
                
                currentNode = pathHistory.peek();
                Node.Type typeBefore = pathHistoryTypes.peek();
                
                return typeBefore;
            }
        }
        else {
            if (pathHistoryRedo.size() > 0) {
            
                Node oldNode = currentNode;
                
                Node newNode = pathHistoryRedo.peek();      // NOTE: doesn't pop, since that is done in userMove()
                userMove(newNode.x-currentNode.x, newNode.y-currentNode.y);
                
                return get(oldNode);
            }
        }
        
        return null;
    }

    public void generate() {

        complete = false;

        int count = 0;

        do {

            // reset maze
            maze = new Node.Type[height][width];
            creationPath.clear();
            doubles.clear();

            pathHistory.clear();
            pathHistoryTypes.clear();
            pathHistoryRedo.clear();

            // set random start
            int startX = rand.nextInt(width);
            int startY = rand.nextInt(height);
            startNode = new Node(startX, startY);

            // set start node and add to path
            set(startNode, Node.Type.START);
            creationPath.add(startNode);
            pathHistory.add(startNode);
            pathHistoryTypes.add(get(startNode));
            
            currentNode = startNode;

            while (getNextNode()) {
                Node.Type nextType = getNextNodeType();
                
                set(currentNode, nextType);
                
                if (nextType == Node.Type.WALL) {
                    // take a step back (note that currentNode is never added to path here)
                    currentNode = creationPath.getLast();
                }
                else {
                    // only add currentNode if it is walkable
                    creationPath.add(currentNode);
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
            if (currentNode.nextTo(startNode) && creationPath.size() > 2) {
                currentNode = creationPath.pop();
            }
            
            count++;
        }
        
        // prevent too short mazes, and prevent startNode from being endNode
        while (creationPath.size() < minPathLength || currentNode.same(startNode));

        endNode = currentNode;
        currentNode = startNode;

        maze[startNode.y][startNode.x] = Node.Type.START;
        maze[endNode.y][endNode.x] = Node.Type.END;

        System.out.println("mazes generated: " + count);
    }

    // TODO: prevent creating double in corner
    private Node.Type getNextNodeType() {
        
        if (creationPath.size() > 1) {
            // this check prevents creating walls all around the first node

            double chance = rand.nextDouble();
            
            if (ChanceNextNode.DOUBLE.evaluate(chance)) {
                doubles.add(currentNode);
                return Node.Type.DOUBLE;
            }

            else if (ChanceNextNode.WALL.evaluate(chance)) {
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

        if (ChanceNextNode.SAME_DIR.evaluate(chance)) {
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

        if (ChanceNextNode.SAME_DIR.evaluate(chance)) {
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

    public boolean nodeTypeWalkable(Node node) {
        return get(node) != Node.Type.WALL && get(node) != Node.Type.BLOCKED;
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

}
