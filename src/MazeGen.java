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
    public static boolean tryChangeNodeType = true;

    // true: usually faster, but creates more stepbacks and possibly easy maze
    // false: usually slower, but possibly more spread out double nodes
    // NOTE: if false, doubles may still appear around start if needed
    // to reach the amountDoubles
    // NOTE: if doublesArePlacedFirst is true, tryChangeNodeType may not have any effect
    // (if the path is not cut off by itself during generation)
    public static boolean doublesArePlacedFirst = false;

    public static boolean endCanBeDouble = true;
    public static boolean endMustBeDouble = false;

    public static LinkedList<Node> creationPath = new LinkedList<>();

    // keep track of the user's path, in order to be able to backtrack
    private static Stack<Node> pathHistory = new Stack<>();
    private static Stack<Node> pathHistoryRedo = new Stack<>();

    // keep track of doubles (used during creation and reset)
    public static ArrayList<Node> doubleNodes = new ArrayList<>();
    private static ArrayList<Node> triedDouble = new ArrayList<>();
    
    private static Random rand = new Random(1);
    public static Node.Type[][] maze;
    
    private static int width = Config.MAZE_DEFAULT_WIDTH;
    private static int height = Config.MAZE_DEFAULT_HEIGHT;
    
    public static Node startNode;
    public static Node endNode;
    public static Node currentNode;    // used to keep track of the player
    
    public static int amountDoubles = 0;
    public static int amountGround = width * height / 2;
    public static int amountWalls;

    public static int amountDoublesMax;
    public static int amountGroundMin;
    public static int amountGroundMax;

    public static int amountNodesAll = width * height;

    public static int pathLength;
    public static int pathLengthMax;   // used to move startNode if necessary and to limit length of hints

    static {
        update();
    }

    public static void main(String[] args) {

        setSize(5, 5);
        setAmountDoubles(5);
        generate();

        // print info
        System.out.println("doubleNodes (" + doubleNodes.size() + "): " + doubleNodes);
        System.out.println("creationPath (" + creationPath.size() + "): " + creationPath);

        System.out.println("\nMaze with path: ");
        MazePrinter.printMazeWithPath(creationPath);

        System.out.println("Maze with types:");
        MazePrinter.printMazeWithTypes();
    }

    private static void printInfo() {
        System.out.println("maze: " + width + "x" + height);
        System.out.println("amountDoublesMax: " + amountDoublesMax);
        System.out.println("amountDoubles: " + amountDoubles);
        System.out.println("amountGroundMax: " + amountGroundMax);
        System.out.println("amountGround: " + amountGround);

        String endNodeType;

        if (amountNodesAll == 1) {
            // end is start (1x1)
            endNodeType = "none";
        }

        else if (endNode == null) {
            endNodeType = "g or d";
        }
        else if (get(endNode) == Node.Type.END) {
            endNodeType = "g";
        }
        else {
            endNodeType = "d";
        }

        System.out.printf("types: s + %dd + %dg + %dw (end=%s)\n",
                            amountDoubles, amountGround, amountWalls, endNodeType);
        // }
        System.out.println("pathLength: " + pathLength);
        System.out.println("pathLengthMax: " + pathLengthMax);
        System.out.println();
    }

    private static int convertToValidDoubleAmount(int possiblyInvalid) {
        if (even(amountNodesAll) && possiblyInvalid == amountDoublesMax - 1 && !endCanBeDouble) {
            possiblyInvalid--;
        }
        return possiblyInvalid;
    }

    // returns true if accepted, otherwise false
    public static boolean setEndMustBeDouble(boolean v) {
        
        if (endCanBeDouble && amountDoubles > 0) {
            endMustBeDouble = v;
            return true;
        }
        
        return false;
    }

    public static void setEndCanBeDouble(boolean v) {

        endCanBeDouble = v;
        endMustBeDouble = false;
        
        // may have resulted in one less double due to change of `endCanBeDouble`
        amountDoubles = convertToValidDoubleAmount(amountDoubles);

        update();
    }

    public static int setAmountDoubles(int v) {

        v = Math.max(0, Math.min(v, amountDoublesMax));
        amountDoubles = convertToValidDoubleAmount(v);

        update();

        return amountDoubles;
    }

    public static void setAmountGround(int v) {
        amountGround = Math.max(0, Math.min(v, amountGroundMax));
        update();
    }

    private static void update() {

        amountNodesAll = width * height;

        if (amountNodesAll == 1) {
            amountGroundMax = 0;
            amountDoublesMax = 0;
        }
        else if (odd(amountNodesAll)) {
            
            if (endCanBeDouble) {
                amountDoublesMax = amountNodesAll - 1;
            }
            else {
                amountDoublesMax = amountNodesAll - 3;
            }
        }
        else {
            amountDoublesMax = amountNodesAll - 2;
        }

        amountDoubles = Math.min(amountDoubles, amountDoublesMax);

        // NOTE: amountGround is limited by amountDoubles
        amountGroundMax = amountNodesAll - 1 - amountDoubles;
        amountGround = Math.min(amountGround, amountGroundMax);

        amountWalls = amountNodesAll - 1 - amountDoubles - amountGround;

        pathLength = 1 + 2 * amountDoubles + amountGround;
        pathLengthMax = amountNodesAll + amountDoublesMax;
        
        if (amountDoubles == 0) {
            amountGroundMin = 0;
        }
        else if (even(amountDoubles)) {
            amountGroundMin = 1;
        }
        else if (endCanBeDouble) {
            amountGroundMin = 1;
        }
        else {
            amountGroundMin = 2;
        }
    }

    public static void setSize(int w, int h) {
        width = w;
        height = h;
        update();
    }

    public static void setWidth(int w) {
        setSize(w, height);
    }

    public static void setHeight(int h) {
        setSize(width, h);
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
            else if (get(currentNode) == Node.Type.END_DOUBLE) {
                set(currentNode, Node.Type.END);
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
            
            if (newNode.equals(endNode) && get(newNode) == Node.Type.END) {
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

        if (doubleNodes.contains(endNode)) {
            set(endNode, Node.Type.END_DOUBLE);
        }
        else {
            set(endNode, Node.Type.END);
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
                        if (lastNode.equals(endNode)) {
                            set(lastNode, Node.Type.END_DOUBLE);
                        }
                        else {
                            set(lastNode, Node.Type.DOUBLE);
                        }
                    }
                }

                else {
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

    private static boolean even(int v) {
        return v % 2 == 0;
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

        Node firstStartNode = startNode;
        
        if ((width == 1 || height == 1) && (startNode.x + 1 < pathLength || startNode.y + 1 < pathLength)) {
            
            if (rand.nextInt(2) == 0) {
                // set start at beginning
                startNode = new Node(0, 0);
            }
            else {
                // set start at end
                if (width == 1) {
                    startNode = new Node(0, height-1);
                }
                else {
                    startNode = new Node(width-1, 0);
                }
            }
        }

        else if (pathLength == pathLengthMax && odd(width) && odd(height)) {

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

        if (doubleNodes.contains(endNode)) {
            set(endNode, Node.Type.END_DOUBLE);
        }
        else {
            set(endNode, Node.Type.END);
        }

        set(startNode, Node.Type.START);    // was just replaced during creationPath-loop
    }

    // setup generation process and begin generating
    public static void generate() {

        System.out.println("generating:");
        printInfo();

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
        if (doubleNodes.size() < amountDoubles) {
            // TODO: place this check in setNodeType() and
            // use return value as signal to (dis)continue branch
            return false;
        }

        // check if endNode can/must be a double
        if (doubleNodes.contains(creationPath.getLast())) {
            if (!endCanBeDouble) {
                return false;
            }
        }
        else if (endMustBeDouble) {
            return false;
        }

        // check for uncleared doubles
        for (Node node : doubleNodes) {
            if (get(node) == Node.Type.TOUCHED) {
                return false;
            }
        }

        return true;    // signals to stop traversing
    }

    // changes node type from ground to double or double to ground (if possible)
    private static boolean changeNodeType(Node node) {
        
        if (!tryChangeNodeType) {
            return false;
        }

        int doublesLeft = amountDoubles - doubleNodes.size();

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

        int doublesLeft = amountDoubles - doubleNodes.size();

        if (get(node) == Node.Type.TOUCHED) {
            set(node, Node.Type.GROUND);
            return;
        }

        else if (doublesLeft > 0) {

            int nodesLeft = pathLength - 2 - creationPath.size();

            int endCountsAs = 1;
            if (endCanBeDouble) {
                endCountsAs = 0;
            }
            
            if ((nodesLeft - endCountsAs == doublesLeft)
                || doublesArePlacedFirst
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
