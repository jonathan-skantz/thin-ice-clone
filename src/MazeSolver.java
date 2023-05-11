import java.util.LinkedList;
import java.util.Stack;

public class MazeSolver {

    private MazeGen mg;

    private Node.Type[][] mazeCopy;
    private Node nodeStart;

    private Stack<Node> accumulatorPath;
    public LinkedList<Node> longestPath;

    public static void main(String[] args) {

        // generate maze
        MazeGen mg = new MazeGen(5, 5);
        mg.generate();

        // find solution
        MazeSolver s = new MazeSolver(mg, mg.startNode);
        s.findLongestPath();

        // print comparison
        System.out.println("Maze with creation path:");
        MazePrinter.printMazeWithPath(mg.maze, mg.creationPath);

        System.out.println("longest: ");
        MazePrinter.printMazeWithPath(mg.maze, s.longestPath);
    }

    public MazeSolver(MazeGen mg, Node start) {

        this.mg = mg;
        this.mazeCopy = new Node.Type[mg.maze.length][mg.maze[0].length];
        for (int i = 0; i < mg.maze.length; i++) {
            for (int j = 0; j < mg.maze[0].length; j++) {
                mazeCopy[i][j] = mg.maze[i][j];
            }
        }
        this.nodeStart = start;
    }

    public LinkedList<Node> findLongestPath() {
        
        // reset paths
        accumulatorPath = new Stack<>();
        longestPath = new LinkedList<>();

        exploreNewNodeFrom(nodeStart);

        return longestPath;
    }

    private boolean walkable(Node node) {
        int x = node.x;
        int y = node.y;

        if (x < 0 || x >= mazeCopy[0].length) {
            return false;
        }
        
        if (y < 0 || y >= mazeCopy.length) {
            return false;
        }
        
        Node.Type t = mazeCopy[y][x];

        return t == Node.Type.GROUND || t == Node.Type.END;
    }


    private void exploreNewNodeFrom(Node currentNode) {
        
        accumulatorPath.add(currentNode);
        
        if (currentNode.same(mg.endNode)) {
            // if end node: check if longer than last saved path

            if (accumulatorPath.size() > longestPath.size()) {
                longestPath = new LinkedList<>(accumulatorPath);
            }
        }
    
        else {

            int[][] dir = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
            // explore all possible directions from the current node,
            // checking in the order: left, down, right, up
            for (int[] d : dir) {
                
                Node newNode = new Node(currentNode.x + d[0], currentNode.y + d[1]);
                
                if (walkable(newNode)) {
                    
                    mazeCopy[newNode.y][newNode.x] = Node.Type.BLOCKED;
                    
                    exploreNewNodeFrom(newNode);
                    
                    mazeCopy[newNode.y][newNode.x] = Node.Type.GROUND;
                }
            }
        }
        
        // finally: all possible directions from currentNode have been explored,
        // therefore: backtrack to the previous node and continue exploring from there
        accumulatorPath.pop();
    }

}
