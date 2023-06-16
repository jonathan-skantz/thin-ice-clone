import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;

public class MazeGen {

    private int invalidPathsCount = 0;

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
    
    private Random rand = new Random(0);
    public Node.Type[][] maze;
    
    public int width;
    public int height;
    
    public Node startNode;
    public Node endNode;
    public Node currentNode;    // used to keep track of the player

    public int desiredPathLength = 10;
    public float chanceDouble = 0.25f;

    public static void main(String[] args) {

        // generate maze
        MazeGen mg = new MazeGen(5, 5);
        mg.generate();

        System.out.println("invalid paths: " + mg.invalidPathsCount);

        // print path and types
        System.out.println("\nMaze with path: ");
        MazePrinter.printMazeWithPath(mg.maze, mg.creationPath);

        System.out.println("Maze with types:");
        MazePrinter.printMazeWithTypes(mg.maze);
    }

    public MazeGen(int w, int h) {
        width = w;
        height = h;

        // prevent desiredPathLength from being larger than possible
        if (desiredPathLength > width * height) {
            desiredPathLength = width * height;
        }
    }

    // returns true if valid move, false if invalid move
    public boolean userMove(KeyHandler.ActionKey action) {

        int[] change = action.getMovement();
        Node newNode = new Node(currentNode.x + change[0], currentNode.y + change[1]);

        if (pointOnGrid(newNode.x, newNode.y) && nodeTypeWalkable(newNode)) {
            
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
            
            if (newNode.same(endNode)) {
                complete = true;
            }
    
            currentNode = newNode;
            return true;
        }

        return false;
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
        pathHistoryTypes.add(Node.Type.START);

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

    // returns ActionKey in which grid direction the step occured
    public KeyHandler.ActionKey step(int direction){
        
        if (direction == -1) {
            
            if (pathHistory.size() > 1) {
                nodeReset(currentNode);
                
                Node lastNode = pathHistory.pop();      // pops currentNode
                pathHistoryTypes.pop();
                
                pathHistoryRedo.add(lastNode);
                
                currentNode = pathHistory.peek();
                
                return KeyHandler.ActionKey.getActionFromMovement(lastNode, currentNode);
            }
        }
        else {
            if (pathHistoryRedo.size() > 0) {
                
                Node lastNode = currentNode;
                Node newNode = pathHistoryRedo.peek();      // NOTE: doesn't pop, since that is done in userMove()

                KeyHandler.ActionKey action = KeyHandler.ActionKey.getActionFromMovement(lastNode, newNode);
                userMove(action);
                
                return action;
            }
        }
        
        return null;
    }


    // returns list of neighbors of `node` in a random order
    private ArrayList<Node> getUnsetNeighborsTo(Node node) {

        ArrayList<Node> neighbors = new ArrayList<>(4);

        // up
        Node newNode = new Node(node.x, node.y - 1);
        if (newNode.y >= 0 && get(newNode) == null) neighbors.add(newNode);

        // down
        newNode = new Node(node.x, node.y + 1);
        if (newNode.y < height && get(newNode) == null) neighbors.add(newNode);

        // left
        newNode = new Node(node.x - 1, node.y);
        if (newNode.x >= 0 && get(newNode) == null) neighbors.add(newNode);
        
        // right
        newNode = new Node(node.x + 1, node.y);
        if (newNode.x < width && get(newNode) == null) neighbors.add(newNode);
        
        /*
         * The shuffle randomizes the maze generation.
         * All neighbors will be visited until a path is found.
         * If no maze is found, all permutations of possible paths
         * will be traversed (theoretically, since a maze is always found).
         */
        Collections.shuffle(neighbors, rand);

        return neighbors;
    }

    private boolean odd(int v) {
        return v % 2 != 0;
    }

    private void setRandomStartNode() {

        startNode = new Node(rand.nextInt(width), rand.nextInt(height));

        /*
         * If desiredPathLength is the same as max path length, and
         * both dimensions are odd, and
         * both x and y of startNode are not odd:
         * 
         * the path will always be one node too short.
         * Therefore, a new startNode is determined (unless one the dimensions are like 1x1).
         */

        if (desiredPathLength == width * height && 
            odd(width) && odd(height) &&
            ((odd(startNode.x) && !odd(startNode.y)) || (!odd(startNode.x)  && odd(startNode.y)))) {

            Node oldStart = startNode;
            if (startNode.x - 1 > 0) {
                startNode = new Node(startNode.x - 1, startNode.y);
            }
            else if (startNode.x + 1 < width) {
                startNode = new Node(startNode.x + 1, startNode.y);
            }
            else if (startNode.y - 1 > 0) {
                startNode = new Node(startNode.x, startNode.y - 1);
            }
            else if (startNode.y + 1 < height) {
                startNode = new Node(startNode.x, startNode.y + 1);
            }

            System.out.println("start changed from " + oldStart + " to " + startNode);
        }
        set(startNode, Node.Type.START);
    }

    private void setNodeTypes() {

        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {

                Node.Type type = get(x, y);
                
                // change all nodes that are of type null to wall
                if (type == null) {
                    set(x, y, Node.Type.WALL);
                }

                // otherwise to either ground or 2x
                else {
                    Node.Type newType = getRandomNodeType();

                    if (newType == Node.Type.DOUBLE) {
                        doubles.add(new Node(x, y));
                    }

                    set(x, y, newType);
                }
            }
        }

        set(startNode, Node.Type.START);
        set(endNode, Node.Type.END);

    }

    // setup generation process and begin generating
    public void generate() {

        // clear all
        maze = new Node.Type[height][width];
        creationPath.clear();
        doubles.clear();
        invalidPathsCount = 0;
        complete = false;
        
        setRandomStartNode();

        // add to record
        creationPath.add(startNode);
        pathHistory.add(startNode);
        pathHistoryTypes.add(Node.Type.START);
        
        // start recursive generation
        generateHelper(startNode);
        
        // get endNode from creationPath
        endNode = creationPath.getLast();

        setNodeTypes();

        currentNode = startNode;
    }

    // generate with breadth-first-search by checking neighbors in a random order
    private boolean generateHelper(Node current) {

        if (creationPath.size() == desiredPathLength) {
            return true;    // signals to stop traversing
        }
        
        for (Node neighbor : getUnsetNeighborsTo(current)) {
        
            // add this neighbor
            creationPath.add(neighbor);
            set(neighbor, Node.Type.BLOCKED);
            
            // check neighbors of this neighbor
            boolean done = generateHelper(neighbor);
            if (done) {
                return true;
            }

            // remove this neighbor
            creationPath.removeLast();
            set(neighbor, null);
        }
        
        // All unset neighbors have been traversed but still no appropriate maze is found.
        // Therefore, increase counter and return false to continue searching.

        invalidPathsCount++;

        return false;
    }

    // TODO: prevent creating double in corner
    private Node.Type getRandomNodeType() {
        
        double chance = rand.nextDouble();

        if (chance <= chanceDouble) {
            return Node.Type.DOUBLE;
        }

        return Node.Type.GROUND;
    }

    public boolean nodeTypeWalkable(Node node) {
        return get(node) != Node.Type.WALL && get(node) != Node.Type.BLOCKED;
    }

}
