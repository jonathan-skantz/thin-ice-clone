import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;

public class MazeGen {

    private static int invalidPathsCount = 0;

    public static boolean complete = false;       // is set to true when currentNode == endNode

    public static LinkedList<Node> creationPath = new LinkedList<>();

    // keep track of the user's path, in order to be able to backtrack
    private static Stack<Node> pathHistory = new Stack<>();
    private static Stack<Node.Type> pathHistoryTypes = new Stack<>();
    
    private static Stack<Node> pathHistoryRedo = new Stack<>();
    // pathHistoryRedoTypes are not necessary to track since when redoing,
    // the system thinks the player is moving just like normal

    // keep track of doubles (used during creation and reset)
    private static ArrayList<Node> doubleNodes = new ArrayList<>();
    
    private static Random rand = new Random();
    public static Node.Type[][] maze;
    
    private static int width = Config.MAZE_DEFAULT_WIDTH;
    private static int height = Config.MAZE_DEFAULT_HEIGHT;
    
    public static Node startNode;
    public static Node endNode;
    public static Node currentNode;    // used to keep track of the player

    public static int pathLength = 10;
    public static int pathLengthMax = Config.MAZE_DEFAULT_WIDTH * Config.MAZE_DEFAULT_HEIGHT;

    public static float fractionDoubleNodes = 0.25f;    // used to set `amountDoubles`
    
    private static int amountDoubles;   // may be limited despite value of `fractionDoubleNodes`

    public static void main(String[] args) {

        // generate maze
        MazeGen.generate(5, 5);

        System.out.println("\ninvalid paths: " + invalidPathsCount);

        // print path and types
        System.out.println("creationPath: " + creationPath);
        System.out.println("Maze with path: ");
        MazePrinter.printMazeWithPath(creationPath);

        System.out.println("Maze with types:");
        MazePrinter.printMazeWithTypes();
    }

    // returns true if pathLength is decreased
    public static boolean setWidth(int w) {
        width = w;
        pathLengthMax = w * height;

        if (pathLength > pathLengthMax) {
            pathLength = pathLengthMax;
            return true;
        }
        return false;
    }
    
    // returns true if pathLength is decreased
    public static boolean setHeight(int h) {
        height = h;
        pathLengthMax = width * height;

        if (pathLength > pathLengthMax) {
            pathLength = pathLengthMax;
            return true;
        }
        return true;
    }
    
    // returns true if `v` is accepted (not too long)
    public static boolean setPathLength(int v) {
        
        if (v > pathLengthMax) {
            return false;
        }

        pathLength = v;
        return true;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    // returns true if valid move, false if invalid move
    public static boolean userMove(KeyHandler.ActionKey action) {

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

    public static void set(Node node, Node.Type type) {
        maze[node.y][node.x] = type;
    }
    
    public static void set(int x, int y, Node.Type type) {
        maze[y][x] = type;
    }

    public static Node.Type get(Node node) {
        return maze[node.y][node.x];
    }

    public static Node.Type get(int x, int y) {
        return maze[y][x];
    }

    // TEMP
    public static boolean pointOnGrid(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public static void reset() {
        
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
        for (Node n : doubleNodes) {
            set(n, Node.Type.DOUBLE);
        }
    }

    public static void nodeReset(Node node) {

        if (node.same(startNode)) {
            set(node, Node.Type.START);
        }

        else if (get(node) == Node.Type.BLOCKED) {
            set(node, Node.Type.GROUND);
        }

        else {

            for (Node n : doubleNodes) {
                if (n.same(node)) {
                    set(node, Node.Type.DOUBLE);
                    break;
                }
            }
        }
    }

    // returns ActionKey in which grid direction the step occured
    public static KeyHandler.ActionKey step(int direction){
        
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

    private static boolean walkableType(Node node) {
        // walkable meaning either null (unwalked) or GROUND (after stepped on double)
        // (not END since end is not determined yet)
        return get(node) == null || get(node) == Node.Type.GROUND;
    }

    // returns list of neighbors of `node` in a random order
    private static ArrayList<Node> getWalkableNeighborsTo(Node node) {

        // NOTE: one of the neighbors will be the `node` itself if it is a double,
        // causing a step back to the last node

        ArrayList<Node> neighbors = new ArrayList<>(4);

        // up
        Node newNode = new Node(node.x, node.y - 1);
        if (newNode.y >= 0 && walkableType(newNode)) neighbors.add(newNode);

        // down
        newNode = new Node(node.x, node.y + 1);
        if (newNode.y < height && walkableType(newNode)) neighbors.add(newNode);

        // left
        newNode = new Node(node.x - 1, node.y);
        if (newNode.x >= 0 && walkableType(newNode)) neighbors.add(newNode);
        
        // right
        newNode = new Node(node.x + 1, node.y);
        if (newNode.x < width && walkableType(newNode)) neighbors.add(newNode);
        
        /*
         * The shuffle randomizes the maze generation.
         * All neighbors will be visited until a path is found.
         * If no maze is found, all permutations of possible paths
         * will be traversed (theoretically, since a maze is always found).
         */
        Collections.shuffle(neighbors, rand);

        return neighbors;
    }

    private static boolean odd(int v) {
        return v % 2 != 0;
    }

    private static void setRandomStartNode() {

        startNode = new Node(rand.nextInt(width), rand.nextInt(height));

        /*
         * If pathLength is the same as pathLengthMax, and
         * both dimensions are odd, and
         * both x and y of startNode are not odd:
         * 
         * the path will always be one node too short.
         * Therefore, a new startNode is determined.
         */

        if (pathLength == width * height && odd(width) && odd(height)) {

            while ((odd(startNode.x) && !odd(startNode.y)) || (!odd(startNode.x)  && odd(startNode.y))) {
              
                // sets startNode to the next possible node by going right, then
                // a row down, and moving to top left if hit bottom right

                if (startNode.x + 1 < width) {
                    // move right one step
                    startNode = new Node(startNode.x + 1, startNode.y);
                }

                else {

                    if (startNode.y + 1 < height) {
                        // new row, start from left
                        startNode = new Node(0, startNode.y + 1);
                    }
                    else {
                        // start from top left
                        startNode = new Node(0, 0);
                    }
                }
            }
        }
        set(startNode, Node.Type.START);
    }

    private static void setNodeTypes() {
        
        // change all nodes that are of type null to wall
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                if (get(x, y) == null) {
                    set(x, y, Node.Type.WALL);
                }
            }
        }

        for (Node node : creationPath) {
            set(node, Node.Type.GROUND);
        }

        for (Node node : doubleNodes) {
            set(node, Node.Type.DOUBLE);
        }

        set(startNode, Node.Type.START);    // was just replaced during creationPath-loop
        set(endNode, Node.Type.END);

    }

    public static void generate(int width, int height) {

        MazeGen.width = width;
        MazeGen.height = height;

        pathLengthMax = width * height;

        if (pathLength > pathLengthMax) {
            pathLength = pathLengthMax;
        }

        generate();
    }

    private static void setAmountDoubles() {
        amountDoubles = (int)(pathLength * fractionDoubleNodes);

        int maxDoubles;
        if (odd(pathLength)) {
            maxDoubles = pathLength / 2;
        }
        else {
            maxDoubles = pathLength / 2 - 1;
        }

        if (amountDoubles > maxDoubles) {
            amountDoubles = maxDoubles;
        }
    }

    // setup generation process and begin generating
    public static void generate() {

        // clear all
        maze = new Node.Type[height][width];
        creationPath.clear();
        doubleNodes.clear();
        invalidPathsCount = 0;
        complete = false;
        
        setAmountDoubles();
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
    private static boolean generateHelper(Node current) {

        if (creationPath.size() == pathLength) {

            // too few double nodes
            if (doubleNodes.size() < amountDoubles) {
                return false;
            }

            // endNode cannot be a double
            Node lastDouble = doubleNodes.get(doubleNodes.size()-1);
            if (lastDouble.same(creationPath.getLast())) {
                return false;
            }

            // check for uncleared doubles
            for (Node node : doubleNodes) {
                if (walkableType(node)) {   // TODO: for some reason some doubles are left as null
                    return false;
                }
            }

            return true;    // signals to stop traversing
        }
        
        for (Node neighbor : getWalkableNeighborsTo(current)) {
        
            // add this neighbor
            creationPath.add(neighbor);

            Node.Type type = getRandomNodeType();
            
            if (type == Node.Type.GROUND) {

                for (Node node : doubleNodes) {
                    if (node.same(neighbor)) {
                        // node already set as double
                        type = Node.Type.BLOCKED;
                        break;
                    }
                }
                
                if (type == Node.Type.GROUND) {
                    doubleNodes.add(neighbor);
                }
            }
            
            set(neighbor, type);
            
            // check neighbors of this neighbor
            boolean done = generateHelper(neighbor);
            if (done) {
                return true;
            }

            // remove this neighbor
            creationPath.removeLast();

            if (type == Node.Type.GROUND) {
                // doubleNodes.remove(neighbor);   // aka last element
                doubleNodes.remove(doubleNodes.size()-1);
                // set(neighbor, Node.Type.BLOCKED);
                // System.out.println("left " + neighbor + " as blocked");
            }
            set(neighbor, null);    // neighbor did not lead to a valid path
        }
        
        // All unset neighbors have been traversed but still no appropriate maze is found.
        // Therefore, increase counter and return false to continue searching.

        invalidPathsCount++;

        return false;
    }

    private static Node.Type getRandomNodeType() {
        // NOTE: GROUND is treated as a double that was just stepped on
        
        // TODO: backtrack and set to double if was ground before,
        // in order to reuse the path that is leading up to this neighbor
        
        if (doubleNodes.size() < amountDoubles) {
            if (rand.nextFloat() <= 0.5) {
                return Node.Type.GROUND;
            }
            else if (amountDoubles - doubleNodes.size() == pathLength - creationPath.size()) {
                // "force" a node to be double
                // TODO: "unforced" in generate() if already double
                return Node.Type.GROUND;
            }
        }

        return Node.Type.BLOCKED;
    }

    public static boolean nodeTypeWalkable(Node node) {
        return get(node) != Node.Type.WALL && get(node) != Node.Type.BLOCKED;
    }

}
