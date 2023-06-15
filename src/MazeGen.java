/*

* maze ends only at edge of maze or when colliding with a path node
* no dead ends



*/
import java.util.ArrayList;
import java.util.Collections;
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

    // up, down, left, right
    private ArrayList<int[]> directions = new ArrayList<>(){{
        add(new int[] {0, -1});
        add(new int[] {0, 1});
        add(new int[] {-1, 0});
        add(new int[] {1, 0});
    }};

    public int minPathLength = 100;
    public float chanceDouble = 0.25f;

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

            // add startNode to path
            pathHistory.add(startNode);
            pathHistoryTypes.add(get(startNode));
            
            currentNode = startNode;
            boolean stuck = false;

            while (!stuck) {

                Node.Type nextType = getNextNodeType();
                set(currentNode, nextType);
                creationPath.add(currentNode);

                // try all directions randomly
                Collections.shuffle(directions, rand);
                
                stuck = true;
                for (int[] change : directions) {
                    Node nextNode = new Node(currentNode.x + change[0], currentNode.y + change[1]);
                    
                    if (nodeWithinBounds(nextNode) && get(nextNode) == null) {
                        currentNode = nextNode;
                        stuck = false;
                        break;
                    }
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
        
        double chance = rand.nextDouble();

        if (chance <= chanceDouble) {
            doubles.add(currentNode);

            return Node.Type.DOUBLE;
        }

        return Node.Type.GROUND;
    }


    public boolean nodeTypeWalkable(Node node) {
        return get(node) != Node.Type.WALL && get(node) != Node.Type.BLOCKED;
    }

    private boolean nodeWithinBounds(Node node) {
        return node.x >= 0 && node.x < width && node.y >= 0 && node.y < height;
    }

}
