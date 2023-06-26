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
    
    public static int pathLength = width * height / 2;
    public static int pathLengthMax;
    private static int pathLengthMaxAllDoubles;

    static {
        updateDoublesAmount();
        updatePathLengthMax();
    }

    public static void main(String[] args) {

        // generate maze
        setWidth(5);
        setHeight(5);
        setFractionDoubleNodes(0.5f);
        setPathLength(25);
        
        MazeGen.generate();

        // print info
        System.out.println("\ninvalid paths: " + invalidPathsCount);
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

    // returns pathLengthMax
    private static int updatePathLengthMax() {
        pathLengthMax = (int)(width * height + fractionDoubleNodes * width * height);
        pathLengthMaxAllDoubles = width * height * 2;
        
        if (odd(width) && odd(height)) {
            pathLengthMax -= (int)(fractionDoubleNodes * 3);
            pathLengthMaxAllDoubles -= 3;
        }
        else {
            pathLengthMax -= (int)(fractionDoubleNodes * 2);
            pathLengthMaxAllDoubles -= 2;
        }
        return pathLengthMax;
    }

    // should only be called when setting fractionDoubleNodes or pathLength (not pathLengthMax)
    private static void updateDoublesAmount() {

        /*
            fractionDoubleNode = 1 yields (steps exlude start and end):
            0 steps --> 0 2x, 0 G
            1 steps --> 0 2x, 1 G
            2 steps --> 0 2x, 2 G
            3 steps --> 1 2x, 1 G
            4 steps --> 2 2x, 0 G (max for 2x2)
            5 steps --> 2 2x, 1 G
            6 steps --> 2 2x, 2 G
            7 steps --> 3 2x, 1 G
            8 steps --> 4 2x, 0 G (max for 2x3 or 3x2)
            9 steps --> 4 2x, 1 G
            10 steps --> 4 2x, 2 G
            11 steps --> 5 2x, 1 G
            12 steps --> 6 2x, 0 G
            13 steps --> 6 2x, 1 G (max for 3x3)
            14 steps --> 6 2x, 2 G (impossible for 3x3 since 2 nodes must be start and end)
            15 steps --> 7 2x, 1 G 
            16 steps --> 8 2x, 0 G 
            
            fractionDoubleNode = 0.5f yields:
            0 steps --> 0 2x, 0 G
            1 steps --> 0 2x, 1 G
            2 steps --> 0 2x, 2 G
            3 steps --> 0 2x, 1 G
            4 steps --> 1 2x, 2 G
            5 steps --> 1 2x, 3 G
            6 steps --> 1 2x, 4 G (visually not 50% but actually 50% of possible doubles)
        */

        // calculations are made according to the tables above

        int maxSteps = pathLength - 2;      // excluding start and end
        int doublesMax;

        if (odd(maxSteps)) {
            doublesMax = (maxSteps - 1) / 2;
        }
        else {
            if (maxSteps % 4 == 0) {
                doublesMax = maxSteps / 2;
            }
            else {
                doublesMax = (maxSteps - 2) / 2;
            }
        }

        doublesAmount = (int)(doublesMax * fractionDoubleNodes);
        groundAmount = pathLength - 2 * doublesAmount - 2;
    }

    // returns true if pathLength is decreased
    public static boolean setWidth(int w) {
        width = w;

        updatePathLengthMax();

        if (pathLength > pathLengthMax) {
            pathLength = pathLengthMax;
            return true;
        }
        return false;
    }
    
    // returns true if pathLength is decreased
    public static boolean setHeight(int h) {
        height = h;
        
        updatePathLengthMax();

        if (pathLength > pathLengthMax) {
            pathLength = pathLengthMax;
            return true;
        }
        return false;
    }
    
    // returns true if `v` is accepted (not too long)
    public static boolean setPathLength(int v) {
        
        if (v > pathLengthMax) {
            pathLength = pathLengthMax;
            updateDoublesAmount();
            return false;
        }

        pathLength = v;
        updateDoublesAmount();

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

        if (pathLength == pathLengthMaxAllDoubles && odd(width) && odd(height)) {

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

        set(startNode, Node.Type.START);    // was just replaced during creationPath-loop
        set(endNode, Node.Type.END);

    }

    public static void generate(int width, int height) {

        MazeGen.width = width;
        MazeGen.height = height;

        updatePathLengthMax();

        generate();
    }

    // setup generation process and begin generating
    public static void generate() {

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

    // generate with breadth-first-search by checking neighbors in a random order
    private static boolean generateHelper(Node current) {

        if (creationPath.size() == pathLength) {
            return validCreationPath();
        }

        boolean hasBeenBlocked = false;
        boolean hasBeenDouble = false;
        
        boolean next = true;
        
        Node neighbor = null;
        Node.Type type = Node.Type.GROUND;

        int i = 0;
        ArrayList<Node> neighbors = getWalkableNeighborsTo(current);
        
        while (i < neighbors.size()) {
            
            if (next) {

                neighbor = neighbors.get(i);
                creationPath.add(neighbor);    
                
                type = getRandomNodeType();
                
                if (type == Node.Type.TOUCHED) {
                    
                    if (!hasBeenDouble && !doubleNodes.contains(neighbor)) {
                        hasBeenDouble = true;
                        doubleNodes.add(neighbor);
                    }
                    else {
                        hasBeenBlocked = true;
                        type = Node.Type.GROUND;
                    }
                }

                else {
                    hasBeenBlocked = true;
                }
            }

            set(neighbor, type);

            if (generateHelper(neighbor)) {
                return true;
            }
            else {

                // try the same path but switch this neighbor's type
                if (TRY_CHANGE_NODE_TYPE && (!hasBeenBlocked || !hasBeenDouble)) {

                    if (type == Node.Type.GROUND) {

                        if (!hasBeenDouble && doubleNodes.size() < doublesAmount && !doubleNodes.contains(neighbor)) {
                            // "force" a double
                            hasBeenDouble = true;
                            type = Node.Type.TOUCHED;
                            doubleNodes.add(neighbor);
                            next = false;
                            continue;
                        }
                    }

                    else if (type == Node.Type.TOUCHED) {

                        if (!hasBeenBlocked) {
                            // "force" a normal ground
                            hasBeenBlocked = true;
                            type = Node.Type.GROUND;
                            doubleNodes.remove(neighbor);
                            next = false;
                            continue;
                        }
                    }
                }
                    
                // Finally, this neighbor has (potentially) tried both types but none worked.
                // Therefore, try building the path onto the next neighbor.

                creationPath.removeLast();
                
                if (hasBeenDouble) {
                    doubleNodes.remove(neighbor);
                }
                
                next = true;
                hasBeenBlocked = false;
                hasBeenDouble = false;
                
                set(neighbor, null);
                i++;
            }

        }
        
        // All unset neighbors have been traversed but still no appropriate maze is found.
        // Therefore, increase counter and return false to continue searching.

        invalidPathsCount++;

        return false;
    }

    private static Node.Type getRandomNodeType() {
        // NOTE: TOUCHED is treated as a double that was just stepped on
        
        if (doubleNodes.size() < doublesAmount) {
            if (DOUBLES_ARE_PLACED_FIRST) {
                return Node.Type.TOUCHED;
            }

            else if (doublesAmount - doubleNodes.size() == pathLength - creationPath.size() || rand.nextFloat() <= 0.5) {
                // "force" a node to be double if needed, otherwise 50% chance
                return Node.Type.TOUCHED;
            }
        }

        return Node.Type.GROUND;
    }

    public static boolean nodeTypeWalkable(Node node) {
        return get(node) != Node.Type.WALL && get(node) != Node.Type.BLOCKED;
    }

}
