import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class MazeSolver {

    private MazeGen mg;

    private Node.Type[][] mazeCopy;
    private Node nodeStart;

    private Stack<Node> accumulatorPath;
    
    public LinkedList<Node> longestPath;
    public LinkedList<Node> shortestPath;

    public static void main(String[] args) {

        // generate maze
        MazeGen mg = new MazeGen(5, 5);
        mg.generate();

        // find solutions
        MazeSolver s = new MazeSolver(mg, mg.startNode);
        s.findLongestPath();
        s.findShortestPath();       

        // print comparison
        System.out.println("Maze with creation path:");
        MazePrinter.printMazeWithPath(mg.maze, mg.creationPath);

        System.out.println("longest: ");
        MazePrinter.printMazeWithPath(mg.maze, s.longestPath);

        System.out.println("\nshortest:");
        MazePrinter.printMazeWithPath(mg.maze, s.shortestPath);
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

    /**
     * Find the shortest path from currentNode to endNode in the maze.
     *
     * @return a LinkedList of Nodes representing the shortest path from currentNode to endNode
     */
    public LinkedList<Node> findShortestPath() {
        
        // create a queue for BFS
        Queue<Node> queue = new LinkedList<>();

        // create an array to store the parent node of each node in the shortest path
        Node[][] parent = new Node[mg.height][mg.width];

        // initialize the parent array to null
        for (int y=0; y<mg.height; y++) {
            for (int x=0; x<mg.width; x++) {
                parent[y][x] = null;
            }
        }

        mg.currentNode = nodeStart;

        // mark the startNode as visited and add it to the queue
        boolean[][] visited = new boolean[mg.height][mg.width];
        visited[mg.currentNode.y][mg.currentNode.x] = true;
        queue.add(mg.currentNode);

        // perform BFS
        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();

            // check if the current node is the endNode
            if (currentNode.equals(mg.endNode)) {
                break;
            }
            
            // explore the neighbors of the current node
            for (int y=currentNode.y-1; y<=currentNode.y+1; y++) {
                for (int x=currentNode.x-1; x<=currentNode.x+1; x++) {
                    // skip if the neighbor is out of bounds, or is not walkable

                    Node newNode = new Node(x,  y);

                    if (!walkable(newNode) || !pointNotCorner(currentNode, x, y) || !pointNotNode(currentNode, x, y) || visited[y][x]) {
                        continue;
                    }

                    // mark the neighbor as visited, add it to the queue, and set its parent
                    visited[y][x] = true;
                    Node nextNode = new Node(x, y);
                    queue.add(nextNode);
                    parent[y][x] = currentNode;
                }
            }
        }

        // reset shortest path
        shortestPath = new LinkedList<>();
        Node currentNode = mg.endNode;
        while (currentNode != null) {
            shortestPath.addFirst(currentNode);
            currentNode = parent[currentNode.y][currentNode.x];
        }

        return shortestPath;
    }

    /**
     * Helper method that checks if a given point is not diagonally adjacent to a given node
     * 
     * @param node the node to compare against
     * @param x x-coordinate
     * @param y y-coordinate
     * @return true if the point is not diagonally adjacent to the given node, false otherwise
     */
    private boolean pointNotCorner(Node node, int x, int y) {
        return (x == node.x || y == node.y);
    }

    /**
     * Helper method that checks that a given node is not already part of the maze
     * 
     * @param node the node to compare against
     * @param x x-coordinate
     * @param y y-coordinate
     * @return true if the point is not the same as the given node, false otherwise
     */
    private boolean pointNotNode(Node node, int x, int y) {
        return !(x == node.x && y == node.y);
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
