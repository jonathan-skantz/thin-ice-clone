import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class MazeSolver {

    private static Node.Type[][] mazeCopy;

    private static Stack<Node> accumulatorPath = new Stack<>();

    public static LinkedList<Node> longestPath = new LinkedList<>();
    public static LinkedList<Node> shortestPath = new LinkedList<>();

    public static void main(String[] args) {

        // generate maze
        MazeGen.generate();

        // find solutions
        MazeSolver.findLongestPath();
        MazeSolver.findShortestPath();       

        // print comparison
        System.out.println("Maze with creation path:");
        MazePrinter.printMazeWithPath(MazeGen.creationPath);

        System.out.println("longest: ");
        MazePrinter.printMazeWithPath(longestPath);

        System.out.println("\nshortest:");
        MazePrinter.printMazeWithPath(shortestPath);
    }

    private static void reset() {

        mazeCopy = new Node.Type[MazeGen.getHeight()][MazeGen.getWidth()];
        for (int y=0; y<MazeGen.getHeight(); y++) {
            for (int x=0; x<MazeGen.getWidth(); x++) {
                mazeCopy[y][x] = MazeGen.get(x, y);
            }
        }

        accumulatorPath.clear();
        longestPath.clear();
        shortestPath.clear();
    }

    /**
     * Find the shortest path from currentNode to endNode in the maze.
     *
     * @return a LinkedList of Nodes representing the shortest path from currentNode to endNode
     */
    public static LinkedList<Node> findShortestPath() {

        reset();

        // create a queue for BFS
        Queue<Node> queue = new LinkedList<>();

        // create an array to store the parent node of each node in the shortest path
        Node[][] parent = new Node[MazeGen.getHeight()][MazeGen.getWidth()];

        // initialize the parent array to null
        for (int y=0; y<MazeGen.getHeight(); y++) {
            for (int x=0; x<MazeGen.getWidth(); x++) {
                parent[y][x] = null;
            }
        }

        // mark the startNode as visited and add it to the queue
        boolean[][] visited = new boolean[MazeGen.getHeight()][MazeGen.getWidth()];
        visited[MazeGen.currentNode.y][MazeGen.currentNode.x] = true;
        queue.add(MazeGen.currentNode);

        // perform BFS
        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();

            // check if the current node is the endNode
            // (why .equals and not .same?)
            if (currentNode.equals(MazeGen.endNode)) {
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
        shortestPath.clear();
        Node currentNode = MazeGen.endNode;
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
    private static boolean pointNotCorner(Node node, int x, int y) {
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
    private static boolean pointNotNode(Node node, int x, int y) {
        return !(x == node.x && y == node.y);
    }

    public static LinkedList<Node> findLongestPath() {

        reset();

        exploreNewNodeFrom(MazeGen.currentNode);

        return longestPath;
    }

    private static boolean walkable(Node node) {
        int x = node.x;
        int y = node.y;

        if (x < 0 || x >= mazeCopy[0].length) {
            return false;
        }
        
        if (y < 0 || y >= mazeCopy.length) {
            return false;
        }
        
        Node.Type t = mazeCopy[y][x];

        return t != Node.Type.BLOCKED && t != Node.Type.WALL;
    }

    private static void exploreNewNodeFrom(Node currentNode) {

        // TODO: exit once a longest path is found

        accumulatorPath.add(currentNode);
        
        if (currentNode.same(MazeGen.endNode)) {
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
