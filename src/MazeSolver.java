import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MazeSolver {

    private MazeGen mg;

    private Node.Type[][] mazeCopy;
    private Node nodeStart;

    private Stack<Node> accumulatorPath;
    public List<Node> longestPath;

    public static void main(String[] args) {
        MazeGen mg = new MazeGen(10, 10);

        mg.generate();
        MazeSolver s = new MazeSolver(mg, mg.startNode);
        s.findLongestPath();

        mg.printMazeWithPath();
        s.printMazeWithPath();
        System.out.println(s.longestPath);

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

    public void findLongestPath() {
        accumulatorPath = new Stack<>();
        longestPath = new ArrayList<>();

        exploreNewNodeFrom(nodeStart);
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
                longestPath = new ArrayList<>(accumulatorPath);
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
    

    public List<Node> getLongestPath() {
        return longestPath;
    }

    // TODO: reduce code duplication
    public String getFormatted(String str) {
        // the number in the format should be one larger than
        // the longest strRep of Node.Type
        // TODO: get length without looping through
        // Node.Type.values() every call.
        return String.format("%3s", str);
    }
    private String[][] getMazeWithTypes() {
        
        String[][] mazeOfStr = new String[mg.maze.length][mg.maze[0].length];

        for (int y=0; y<mg.maze.length; y++) {
            for (int x=0; x<mg.maze[y].length; x++) {
                mazeOfStr[y][x] = getFormatted(mg.maze[y][x].strRep);
            }
        }
    
        return mazeOfStr;
    }

    // convert a 2d-array to a string
    private String mazeOfStrToStr(String[][] m) {

        StringBuilder sb = new StringBuilder();

        for (int y=0; y<mg.maze.length; y++) {
            for (int x=0; x<mg.maze[y].length; x++) {
                sb.append(m[y][x]);
            }
            sb.append('\n');
        }
    
        return sb.toString();
    }

    public void printMazeWithPath() {
        
        String[][] mazeOfStr = getMazeWithTypes();

        // set numbers that represent the order of steps of the (a) solution
        for (int i=0; i<longestPath.size(); i++) {
            Node n = longestPath.get(i);
            mazeOfStr[n.y][n.x] = getFormatted(String.valueOf(i));
        }

        System.out.println(mazeOfStrToStr(mazeOfStr));
    }


}
