import java.util.ArrayList;
import java.util.Collections;

/**
 * Class for saving info that is used to create mazes.
 */
public class MazeGen {

    private static int invalidPathsCount = 0;

    private static ArrayList<Node> triedDouble = new ArrayList<>();

    public static int width;
    public static int height;

    public static int pathLength;
    public static int pathLengthMax;   // used to move startNode if necessary and to limit length of hints
    public static int amountNodesAll;

    public static boolean endCanBeDouble = true;
    public static boolean endMustBeDouble = false;

    public static boolean tryChangeNodeType = true;
    public static boolean doublesArePlacedFirst = false;

    public enum Amount {
        GROUND,
        DOUBLES,
        WALLS;

        private int value;
        private int min;
        private int max;
        public ArrayList<Node> nodes = new ArrayList<>();

        public static ArrayList<Amount> priority = new ArrayList<>(){ {
            add(GROUND);
            add(DOUBLES);
            add(WALLS);
        }};

        public static final ArrayList<Amount> priorityDefault = new ArrayList<>(priority);

        // returns old priority of `this`
        public int setPriority(int v) {
            int oldPriority = priority.indexOf(this);
            Amount swapType = priority.get(v);

            priority.set(v, this);
            priority.set(oldPriority, swapType);

            MazeGen.update();
            
            return oldPriority;
        }

        public String getAmounts() {
            return String.format("%d (%d-%d)", value, min, max);
        }

        public static void printInfo() {
            StringBuilder sb = new StringBuilder();
            for (Amount aType : values()) {
                sb.append(aType.toString());
                sb.append(": ");
                sb.append(aType.getAmounts());
                sb.append("\n");
            }
            System.out.println(sb.toString());
        }

        public void setMin(int v) {
            min = Math.min(v, max);
        }
        public int getMin() {
            return min;
        }

        public void setMax(int v) {
            max = Math.max(v, max);
        }
        public int getMax() {
            return max;
        }
        
        public int get() {
            return value;
        }
        public void set(int v) {
            v = applyRangeTo(v);

            if (this == DOUBLES) {

                if (even(amountNodesAll) && v == max - 1 && !endCanBeDouble) {
                    // increment or decrement to avoid impossible value
                    if (v > value) {
                        v++;
                    }
                    else {
                        v--;
                    }
                }
                
                if (value == 0 && endMustBeDouble) {
                    endMustBeDouble = false;
                }
            }
               
            if (v != value) {
                value = v;
                MazeGen.update();
            }
        }

        private int applyRangeTo(int v) {
            return Math.max(min, Math.min(v, max));
        }


        public int update(int nodesLeft) {

            max = nodesLeft;

            if (priority.get(2) == this) {
                // remaining nodes must be this type
                min = nodesLeft;
            }
            else {
                min = 0;
            }


            if (this == DOUBLES) {
                if (even(nodesLeft)) {
                    
                    if (!endCanBeDouble) {
                        max -= 2;
                    }
                }
                else {
                    max--;
                }
                
                limitGroundMin();
                
            }

            max = Math.max(min, max);   // prevent values less than min

            value = applyRangeTo(value);
            
            return nodesLeft - value;
        }

        private void limitGroundMin() {

            if (DOUBLES.value == 0) {
                GROUND.min = 0;
            }
            else if (odd(DOUBLES.value)) {
                GROUND.min = 1;
                if (!endCanBeDouble) {
                    GROUND.min++;
                }
            }
            else if (!endCanBeDouble) {
                GROUND.min = 1;
            }
            else {
                GROUND.min = 0;
            }

            GROUND.value = Math.max(GROUND.min, GROUND.value);
        }

        public int remaining() {
            return value - nodes.size();
        }

    }

    // FIXME: occasionally, an invalid maze is generated (spam space a few times)
    
    private static void printInfo() {

        StringBuilder sb = new StringBuilder();
        sb.append("Current MazeGen config:\n");
        sb.append("size: ");
        sb.append(width);
        sb.append("x");
        sb.append(height);

        sb.append("\n");
        sb.append("pathLength: ");
        sb.append(pathLength);

        sb.append("\n");
        sb.append("endCanBeDouble: ");
        sb.append(endCanBeDouble);
        
        sb.append("\n");
        sb.append("endMustBeDouble: ");
        sb.append(endMustBeDouble);
        
        sb.append("\n");
        sb.append("tryChangeNodeType: ");
        sb.append(tryChangeNodeType);
        
        sb.append("\n");
        sb.append("doublesArePlacedFirst: ");
        sb.append(doublesArePlacedFirst);

        sb.append("\n");
        sb.append("GROUND: ");
        sb.append(Amount.GROUND.getAmounts());
        
        sb.append("\n");
        sb.append("DOUBLES: ");
        sb.append(Amount.DOUBLES.getAmounts());
        
        sb.append("\n");
        sb.append("WALLS: ");
        sb.append(Amount.WALLS.getAmounts());

        System.out.println(sb);
    }


    public static void update() {

        amountNodesAll = width * height;

        int nodesLeft = amountNodesAll - 1;     // excluding start

        for (Amount amountType : Amount.priority) {
            nodesLeft = amountType.update(nodesLeft);
        }
        
        pathLength = 1 + 2 * Amount.DOUBLES.get() + Amount.GROUND.get();
        pathLengthMax = amountNodesAll + Amount.DOUBLES.getMax();
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

    // returns true if accepted, otherwise false
    public static boolean setEndMustBeDouble(boolean v) {
        
        if (endCanBeDouble && MazeGen.Amount.DOUBLES.get() > 0) {
            endMustBeDouble = v;
            return true;
        }
        
        return false;
    }

    public static void setEndCanBeDouble(boolean v) {

        endCanBeDouble = v;

        if (!endCanBeDouble && endMustBeDouble) {
            endMustBeDouble = false;
        }
        // may have resulted in one more or less double due to change of `endCanBeDouble`
        MazeGen.update();
    }

    private static boolean odd(int v) {
        return v % 2 != 0;
    }

    private static boolean even(int v) {
        return v % 2 == 0;
    }

    private static boolean validStartNode(Maze maze, Node node) {
        // NOTE: xor (meaning: both must be even or both must be odd)
        return maze.nodeWithinBounds(node) && !(odd(node.X) ^ odd(node.Y));
    }


    public static boolean walkableType(Maze maze, Node node) {
        // walkable meaning either null (unwalked) or TOUCHED (after stepped on double)
        // (not END since end is not determined yet)
        return maze.get(node) == null || maze.get(node) == Node.Type.TOUCHED;
    }

    // returns list of neighbors of `node` in a random order
    private static ArrayList<Node> getWalkableNeighborsOf(Maze maze, Node node) {

        // NOTE: one of the neighbors will be the `node` itself if it is a double,
        // causing a step back to the last node

        ArrayList<Node> neighbors = new ArrayList<>(4);

        for (Maze.Direction dir : Maze.Direction.values()) {
            
            Node neighbor = node.getNeighbor(dir);
            
            if (maze.nodeWithinBounds(neighbor) && walkableType(maze, neighbor)) {
                neighbors.add(neighbor);
            }
        }

        /*
         * The shuffle randomizes the maze generation.
         * All neighbors will be visited until a path is found.
         * If no maze is found, all permutations of possible paths
         * will be traversed (theoretically, since a maze is always found).
         */
        Collections.shuffle(neighbors, Config.rand);

        return neighbors;
    }

    private static Node getRandomStartNode(Maze maze) {

        Node start = new Node(Config.rand.nextInt(maze.width), Config.rand.nextInt(maze.height));

        /*
         * If pathLength is the same as max length with all doubles,
         * and both dimensions are odd,
         * and exactly one of x and y of startNode are even:
         * 
         * the path will always be one node too short.
         * Therefore, a new startNode must be determined.
         */

        if ((maze.width == 1 || maze.height == 1) &&
            (start.X + 1 < pathLength || start.Y + 1 < pathLength)) {
            
            if (Config.rand.nextInt(2) == 0) {
                // set start at beginning
                start = new Node(0, 0);
            }
            else {
                // set start at end
                if (width == 1) {
                    start = new Node(0, maze.height-1);
                }
                else {
                    start = new Node(maze.width-1, 0);
                }
            }
        }

        // all nodes are stepped on
        else if (1 + Amount.GROUND.get() + Amount.DOUBLES.get() + Amount.WALLS.get() == amountNodesAll &&
                    odd(maze.width) && odd(maze.height)) {

            Node firstStart = start;

            // try move up, down, left, right once until startNode is valid
            for (Maze.Direction dir : Maze.Direction.values()) {
                if (validStartNode(maze, start)) {
                    break;
                }
                start = firstStart.getNeighbor(dir);
            }

        }

        return start;
    }

    private static void setTypesAfterGen(Maze maze) {
        
        for (Node node : Amount.DOUBLES.nodes) {
            maze.set(node, Node.Type.DOUBLE);
        }
        
        maze.endNode = maze.currentNode;
        maze.currentNode = maze.startNode;

        if (Amount.DOUBLES.nodes.contains(maze.endNode)) {
            maze.set(maze.endNode, Node.Type.END_DOUBLE);
        }
        else {
            maze.set(maze.endNode, Node.Type.END);
        }

        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                if (maze.get(x, y) == null) {
                    maze.set(x, y, Node.Type.WALL);
                }
            }
        }

        for (int y=0; y<height; y++) {
            maze.typesOriginal[y] = maze.types[y].clone();
        }

    }

    // setup generation process and begin generating
    public static Maze generate() {

        printInfo();

        for (Amount a : Amount.values()) {
            a.nodes.clear();
        }

        invalidPathsCount = 0;

        Maze maze = new Maze(width, height, null);

        Node startNode = getRandomStartNode(maze);
        maze.setStartNode(startNode);

        // start recursive generation
        generateHelper(maze);

        System.out.println("\ninvalid paths: " + invalidPathsCount);
        
        setTypesAfterGen(maze);

        return maze;
    }

    private static boolean validCreationPath(Maze maze) {

        // too few double nodes
        if (Amount.DOUBLES.remaining() > 0) {
            // TODO: place this check in setType() and
            // use return value as signal to (dis)continue branch
            return false;
        }

        // check if endNode can/must be a double
        if (Amount.DOUBLES.nodes.contains(maze.creationPath.getLast())) {
            if (!endCanBeDouble) {
                return false;
            }
        }
        else if (endMustBeDouble) {
            return false;
        }

        // check for uncleared doubles
        for (Node node : Amount.DOUBLES.nodes) {
            if (maze.get(node) == Node.Type.TOUCHED) {
                return false;
            }
        }

        return true;    // signals to stop traversing
    }

    // changes node type from ground to double or double to ground (if possible)
    private static boolean changeNodeType(Maze maze, Node node) {
        
        if (!tryChangeNodeType) {
            return false;
        }

        if (maze.get(node) == Node.Type.TOUCHED) {
            // change from double to ground
            maze.set(node, Node.Type.GROUND);
            Amount.DOUBLES.nodes.remove(node);
        }
        else if (maze.get(node) == Node.Type.GROUND) {
            
            if (Amount.DOUBLES.nodes.contains(node)) {
                // don't change from ground to double (is already double)
                return false;
            }

            if (triedDouble.contains(node)) {
                triedDouble.remove(node);
                // don't change from ground to double (already tried)
                return false;
            }

            if (Amount.DOUBLES.remaining() == 0) {
                // don't change from ground to double (too many)
                return false;
            }

            // change from ground to double
            maze.set(node, Node.Type.TOUCHED);
            Amount.DOUBLES.nodes.add(node);
            triedDouble.add(node);
        }

        return true;
    }

    // sets the first node type of a node
    private static void setType(Maze maze, Node node) {

        // int doublesLeft = Node.Type.DOUBLE.getAmount() - Node.Type.DOUBLE.nodes.size();

        if (maze.get(node) == Node.Type.TOUCHED) {
            maze.set(node, Node.Type.GROUND);
            Amount.GROUND.nodes.add(node);
            return;
        }

        else if (Amount.DOUBLES.remaining() > 0) {

            int nodesLeft = pathLength - 2 - maze.creationPath.size();
            int endCountsAsGround = endCanBeDouble ? 0 : 1;
            
            if ((nodesLeft - endCountsAsGround == Amount.DOUBLES.remaining())
                || doublesArePlacedFirst
                || Config.rand.nextFloat() <= 0.5) {

                // force a double, place at beginning, or 50% chance of being double
                maze.set(node, Node.Type.TOUCHED);
                Amount.DOUBLES.nodes.add(node);
                triedDouble.add(node);
                return;
            }
        }

        maze.set(node, Node.Type.GROUND);
        Amount.GROUND.nodes.add(node);
    }

    // generate with breadth-first-search by checking neighbors in a random order
    private static boolean generateHelper(Maze maze) {

        if (maze.creationPath.size() == pathLength) {
            return validCreationPath(maze);
        }

        boolean next = true;
        Node neighbor = null;

        int i = 0;
        ArrayList<Node> neighbors = getWalkableNeighborsOf(maze, maze.currentNode);

        while (i < neighbors.size()) {
            
            if (next) {
                neighbor = neighbors.get(i);
                maze.currentNode = neighbor;
                setType(maze, neighbor);
                maze.creationPath.add(neighbor);
            }

            if (generateHelper(maze)) {
                // test if valid
                return true;
            }
            else if (changeNodeType(maze, neighbor)) {
                // try to change node type
                next = false;
                continue;
            }

            // else: invalid path, even after (potentially) changing node type

            Node node = maze.creationPath.removeLast();

            if (Amount.DOUBLES.nodes.contains(node)) {
                maze.set(neighbor, Node.Type.TOUCHED);
            }
            else {
                maze.removeNode(neighbor);
            }

            next = true;
            i++;
        }
        
        // All unset neighbors have been traversed but still no appropriate maze is found.
        // Therefore, increase counter and return false to continue searching.

        invalidPathsCount++;

        return false;
    }

}
