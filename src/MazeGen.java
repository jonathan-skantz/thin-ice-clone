import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;

public class MazeGen {

    private static int invalidPathsCount = 0;

    public static boolean complete = false;       // is set to true when currentNode == endNode
    
    // true: usually slower, but will definitely result in a maze
    // false: usually faster, but may not result in a maze
    private static final boolean TRY_CHANGE_NODE_TYPE = true;

    // true: usually faster, but creates more stepbacks and possibly easy maze
    // false: usually slower, but possibly more spread out double nodes
    // NOTE: if false, doubles may still appear around start if needed
    // to reach the doublesAmount
    // NOTE: if DOUBLES_ARE_PLACED_FIRST is true, TRY_CHANGE_NODE_TYPE may not have any effect
    // (if the path is not cut off by itself during generation)
    private static final boolean DOUBLES_ARE_PLACED_FIRST = false;

    public static LinkedList<Node> creationPath = new LinkedList<>();

    // keep track of the user's path, in order to be able to backtrack
    private static Stack<Node> pathHistory = new Stack<>();
    private static Stack<Node> pathHistoryRedo = new Stack<>();

    // keep track of doubles (used during creation and reset)
    private static ArrayList<Node> doubleNodes = new ArrayList<>();
    private static ArrayList<Node> triedDouble = new ArrayList<>();
    
    private static Random rand = new Random(1);
    public static Node.Type[][] maze;
    
    private static int width = Config.MAZE_DEFAULT_WIDTH;
    private static int height = Config.MAZE_DEFAULT_HEIGHT;
    
    public static Node startNode;
    public static Node endNode;
    public static Node currentNode;    // used to keep track of the player
    
    // NOTE: the more doubles, the longer the generation time
    // NOTE: fractionDoubleNodes does not refer to how many of the nodes that
    // appear on screen are doubles, but rather how many of the possible doubles
    // that are set to double.
    // E.g.: 10 pathLength, 1f fractionDoubleNodes --> start + 4 doubles + 0 ground + end
    // but 10 pathLength, 0.5f fractionDoubleNodes --> start + 2 doubles + 4 ground + end
    // since the doubles are 50% of the max amount of doubles (which is determined by pathLength and the fraction)

    public static float fractionDoubleNodes = 0.5f;    // used to set `doublesAmount`
    public static int doublesAmount;
    public static int groundAmount;
    
    // calculated based on current `fractionDoubleNodes` and full width and height (max pathLength)
    private static int doublesAmountMax;
    
    public static int pathLength = width * height / 2;
    public static int pathLengthMax;

    static {
        updateDoublesAmount();
        updatePathLengthMax();
    }

    public static void main(String[] args) {

        // generate maze
        setWidth(5);
        setHeight(5);
        setFractionDoubleNodes(0.5f);
        setPathLength(999);

        System.out.println("pathLengthMax: " + pathLengthMax);
        System.out.println("doublesAmount: " + doublesAmount);
        System.out.println("pathLength: " + pathLength);
        MazeGen.generate();

        // print info
        System.out.println("doubleNodes (" + doubleNodes.size() + "): " + doubleNodes);
        System.out.println("creationPath (" + creationPath.size() + "): " + creationPath);

        System.out.println("\nMaze with path: ");
        MazePrinter.printMazeWithPath(creationPath);

        System.out.println("Maze with types:");
        MazePrinter.printMazeWithTypes();
    }

    public static void setFractionDoubleNodes(float f) {
        if (f < 0 || f > 1) {
            return;
        }

        fractionDoubleNodes = f;
        updateDoublesAmount();
        updatePathLengthMax();
    }

    // Should be called when `width`, `height`, or `fractionDoubleNodes` is changed.
    private static void updateDoublesAmount() {

        /*  pathLength including start and end:
             1 pL: 0 2x, 0 G --> doublesMax=0
             2 pL: 0 2x, 0 G --> doublesMax=(pL-2)/2
             3 pL: 0 2x, 1 G --> doublesMax=(pL-3)/2
             4 pL: 0 2x, 2 G --> doublesMax=(pL-4)/2
             5 pL: 1 2x, 1 G --> doublesMax=(pL-3)/2
             6 pL: 2 2x, 0 G --> doublesMax=(pL-2)/2
             7 pL: 2 2x, 1 G --> doublesMax=(pL-3)/2
             8 pl: 2 2x, 2 G --> doublesMax=(pL-4)/2
             9 pL: 3 2x, 1 G --> doublesMax=(pL-3)/2
            10 pL: 4 2x, 0 G --> doublesMax=(pL-2)/2
            11 pL: 4 2x, 1 G --> doublesMax=(pL-3)/2    
            12 pL: 4 2x, 2 G --> doublesMax=(pL-4)/2
            13 pL: 5 2x, 1 G --> doublesMax=(pL-3)/2
            14 pL: 6 2x, 0 G --> doublesMax=(pL-2)/2
            15 pL: 6 2x, 1 G --> doublesMax=(pL-3)/2
        */ 

        // calculations are made according to the table above
        // (note that a pathLength of 1 is an exception)

        if (pathLength == 1) {
            // too small maze for any doubles
            doublesAmount = 0;
            doublesAmountMax = 0;
            groundAmount = 0;
        }
        else {
            int decr;

            if (pathLength % 4 == 0) {
                decr = 4;
            }
            else if (pathLength % 2 == 0) {
                decr = 2;
            }
            else {
                decr = 3;
            }
            
            int doublesMax = (pathLength - decr) / 2;
            int nodesMax = width * height - decr;

            doublesAmount = (int)(doublesMax * fractionDoubleNodes);
            doublesAmountMax = (int)(nodesMax * fractionDoubleNodes);
            
            // if one dimension is 1 and odd amount of doubles,
            // at least one double will only be stepped on once (invalid path)
            if ((width == 1 || height == 1) && odd(doublesAmount)) {
                doublesAmount--;
            }
            groundAmount = pathLength - 2 - 2 * doublesAmount;
        }
    }

    // returns pathLengthMax.
    // Should be called when `width`, `height`, or `fractionDoubleNodes` is changed.
    private static int updatePathLengthMax() {      

        pathLengthMax = width * height + doublesAmountMax;

        if (pathLength > pathLengthMax) {
            pathLength = pathLengthMax;
        }

        return pathLengthMax;
    }

    // returns true if pathLength is decreased
    public static boolean setWidth(int w) {
        width = w;

        int pathLengthBefore = pathLength;

        updatePathLengthMax();
        updateDoublesAmount();
        updatePathLengthMax();

        return pathLength < pathLengthBefore;
    }
    
    // returns true if pathLength is decreased
    public static boolean setHeight(int h) {
        
        height = h;
        
        int pathLengthBefore = pathLength;
        
        updatePathLengthMax();
        updateDoublesAmount();
        updatePathLengthMax();
        
        return pathLength < pathLengthBefore;
    }
    
    // returns true if `v` is accepted (not too long)
    public static boolean setPathLength(int v) {

        if (v > pathLengthMax) {
            v = pathLengthMax;
        }

        pathLength = v;
        updateDoublesAmount();

        return v <= pathLengthMax;
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

        if (nodeWithinBounds(newNode) && nodeTypeWalkable(newNode)) {
            
            if (get(currentNode) == Node.Type.DOUBLE) {
                set(currentNode, Node.Type.TOUCHED);
            }
            else {
                set(currentNode, Node.Type.BLOCKED);
            }
            
            pathHistory.add(newNode);
    
            if (pathHistoryRedo.size() > 0) {
                if (pathHistoryRedo.peek().equals(newNode)) {
                    pathHistoryRedo.pop();
                }
                else {
                    // new history is made, clear last record of redos
                    pathHistoryRedo.clear();
                }
            }
            
            if (newNode.equals(endNode)) {
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

    public static boolean nodeWithinBounds(Node node) {
        return node.x >= 0 && node.x < width && node.y >= 0 && node.y < height;
    }

    public static void reset() {
        
        currentNode = startNode;

        pathHistory.clear();
        pathHistoryRedo.clear();
        
        pathHistory.add(startNode);

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

    // returns ActionKey in which grid direction the step occured
    public static KeyHandler.ActionKey step(int direction){
        
        if (direction == -1) {
            
            if (pathHistory.size() > 1) {
                
                Node lastNode = pathHistory.pop();      // pops currentNode
                pathHistoryRedo.add(lastNode);
                
                currentNode = pathHistory.peek();

                // adjust lastNode
                if (get(lastNode) == Node.Type.TOUCHED) {

                    // don't set to double since it was
                    // just about to be set to GROUND when left
                    if (!pathHistory.contains(lastNode)) {
                        set(lastNode, Node.Type.DOUBLE);
                    }
                }

                else if (get(lastNode) != Node.Type.DOUBLE) {
                    set(lastNode, Node.Type.GROUND);
                }

                // adjust currentNode
                if (currentNode.equals(startNode)) {
                    set(currentNode, Node.Type.START);
                }
                else if (doubleNodes.contains(currentNode)) {
                    set(currentNode, Node.Type.TOUCHED);
                }
                else {
                    set(currentNode, Node.Type.GROUND);
                }

                
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
        // walkable meaning either null (unwalked) or TOUCHED (after stepped on double)
        // (not END since end is not determined yet)
        return get(node) == null || get(node) == Node.Type.TOUCHED;
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

    private static boolean validStartNode() {
        // NOTE: xor (meaning: both must be even or both must be odd)
        return nodeWithinBounds(startNode) && !(odd(startNode.x) ^ odd(startNode.y));
    }

    private static void setRandomStartNode() {

        startNode = new Node(rand.nextInt(width), rand.nextInt(height));

        /*
         * If pathLength is the same as max length with all doubles,
         * and both dimensions are odd,
         * and exactly one of x and y of startNode are even:
         * 
         * the path will always be one node too short.
         * Therefore, a new startNode must be determined.
         */

        if (pathLength == pathLengthMax && odd(width) && odd(height)) {

            Node firstStartNode = startNode;
            
            // try move up, down, left, right once until startNode is valid

            if (!validStartNode()) {
                startNode = new Node(firstStartNode.x, firstStartNode.y - 1);
                
                if (!validStartNode()) {
                    startNode = new Node(firstStartNode.x, firstStartNode.y + 1);
                    
                    if (!validStartNode()) {
                        startNode = new Node(firstStartNode.x - 1, firstStartNode.y);
                        
                        if (!validStartNode()) {
                            startNode = new Node(firstStartNode.x + 1, firstStartNode.y);
                        }
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

        set(endNode, Node.Type.END);
        set(startNode, Node.Type.START);    // was just replaced during creationPath-loop
    }

    public static void generate(int width, int height) {

        setWidth(width);
        setHeight(height);

        generate();
    }

    // setup generation process and begin generating
    public static void generate() {

        System.out.println("generating maze size: " + width + "x" + height);
        System.out.println("pathLength: " + pathLength);
        System.out.println("doublesAmount: " + doublesAmount);

        // clear all
        maze = new Node.Type[height][width];
        creationPath.clear();
        doubleNodes.clear();
        pathHistory.clear();

        invalidPathsCount = 0;
        complete = false;

        setRandomStartNode();

        // add to record
        creationPath.add(startNode);
        pathHistory.add(startNode);
        
        // start recursive generation
        generateHelper(startNode);

        System.out.println("invalid paths: " + invalidPathsCount);
        
        // get endNode from creationPath
        endNode = creationPath.getLast();

        setNodeTypes();

        currentNode = startNode;
    }

    private static boolean validCreationPath() {

        // too few double nodes
        if (doubleNodes.size() < doublesAmount) {
            return false;
        }

        // endNode cannot be a double
        if (doubleNodes.contains(creationPath.getLast())) {
            return false;
        }

        // check for uncleared doubles
        for (Node node : doubleNodes) {
            if (get(node) == Node.Type.TOUCHED || get(node) == null) {
                // for some reason, some double nodes are left as null
                return false;
            }
        }

        return true;    // signals to stop traversing
    }

    // changes node type from ground to double or double to ground (if possible)
    private static boolean changeNodeType(Node node) {
        
        if (!TRY_CHANGE_NODE_TYPE) {
            return false;
        }

        int doublesLeft = doublesAmount - doubleNodes.size();

        if (get(node) == Node.Type.TOUCHED) {
            // change from double to ground
            set(node, Node.Type.GROUND);
            doubleNodes.remove(node);
        }
        else if (get(node) == Node.Type.GROUND) {
            
            if (doubleNodes.contains(node)) {
                // don't change from ground to double (is already double)
                return false;
            }

            if (triedDouble.contains(node)) {
                triedDouble.remove(node);
                // don't change from ground to double (already tried)
                return false;
            }

            if (doublesLeft == 0) {
                // don't change from ground to double (too many)
                return false;
            }

            // change from ground to double
            set(node, Node.Type.TOUCHED);
            doubleNodes.add(node);
            triedDouble.add(node);
        }

        return true;
    }

    // sets the first node type of a node
    private static void setNodeType(Node node) {

        int doublesLeft = doublesAmount - doubleNodes.size();

        if (get(node) == Node.Type.TOUCHED) {
            set(node, Node.Type.GROUND);
            return;
        }

        else if (doublesLeft > 0) {

            int stepsLeft = pathLength - 1 - creationPath.size();
            
            if ((doublesLeft * 2 == stepsLeft || doublesLeft * 2 == stepsLeft - 1)
                || DOUBLES_ARE_PLACED_FIRST
                || rand.nextFloat() <= 0.5) {

                // force a double, place at beginning, or 50% chance of being double
                set(node, Node.Type.TOUCHED);
                doubleNodes.add(node);
                triedDouble.add(node);
                return;
            }
        }

        set(node, Node.Type.GROUND);
    }

    // generate with breadth-first-search by checking neighbors in a random order
    private static boolean generateHelper(Node current) {

        if (creationPath.size() == pathLength) {
            return validCreationPath();
        }

        boolean next = true;
        Node neighbor = null;

        int i = 0;
        ArrayList<Node> neighbors = getWalkableNeighborsTo(current);

        while (i < neighbors.size()) {
            
            if (next) {
                neighbor = neighbors.get(i);
                setNodeType(neighbor);
                creationPath.add(neighbor);
            }

            if (generateHelper(neighbor)) {
                // test if valid
                return true;
            }
            else if (changeNodeType(neighbor)) {
                // try to change node type
                next = false;
                continue;
            }

            // else: invalid path, even after (potentially) changing node type

            Node node = creationPath.removeLast();

            if (creationPath.contains(node)) {
                set(neighbor, Node.Type.TOUCHED);
            }
            else {
                set(neighbor, null);
                doubleNodes.remove(neighbor);
            }

            next = true;
            i++;
        }
        
        // All unset neighbors have been traversed but still no appropriate maze is found.
        // Therefore, increase counter and return false to continue searching.

        invalidPathsCount++;

        return false;
    }

    public static boolean nodeTypeWalkable(Node node) {
        return get(node) != Node.Type.WALL && get(node) != Node.Type.BLOCKED;
    }

}
