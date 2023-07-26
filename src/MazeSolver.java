import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class MazeSolver {

    private Maze mazeCopy;
    private Maze maze;

    private Stack<Node> accumulatorPath = new Stack<>();

    public LinkedList<Node> longestPath = new LinkedList<>();
    public LinkedList<Node> shortestPath = new LinkedList<>();

    public static void main(String[] args) {

        // generate maze
        Config.apply();
        Maze maze = MazeGen.generate();

        // find solutions
        MazeSolver solver = new MazeSolver(maze);
        solver.findLongestPath();
        solver.findShortestPath();

        // print comparison
        System.out.println("Maze with creation path:");
        maze.printCreationPath();

        System.out.println("longest: ");
        System.out.println(solver.longestPath);
        maze.printPath(solver.longestPath);
        
        System.out.println("\nshortest:");
        System.out.println(solver.shortestPath);
        maze.printPath(solver.shortestPath);
    }

    public MazeSolver(Maze maze) {
        this.maze = maze;
        mazeCopy = new Maze(maze);
        reset();
    }

    private void reset() {

        for (int y=0; y<mazeCopy.height; y++) {
            for (int x=0; x<mazeCopy.width; x++) {
                // NOTE: copies maze.types, not mazeCopy.typesOriginal
                mazeCopy.types[y][x] = maze.types[y][x];
            }
        }

        accumulatorPath.clear();
    }

    /**
     * Find the shortest path from currentNode to endNode in the maze.
     *
     * @return a LinkedList of Nodes representing the shortest path from currentNode to endNode
     */
    public LinkedList<Node> findShortestPath() {

        reset();
        shortestPath.clear();

        // create a queue for BFS
        Queue<Node> queue = new LinkedList<>();

        // create an array to store the parent node of each node in the shortest path
        Node[][] parent = new Node[MazeGen.height][MazeGen.width];

        // initialize the parent array to null
        for (int y=0; y<MazeGen.height; y++) {
            for (int x=0; x<MazeGen.width; x++) {
                parent[y][x] = null;
            }
        }

        // mark the startNode as visited and add it to the queue
        boolean[][] visited = new boolean[MazeGen.height][MazeGen.width];
        visited[maze.currentNode.Y][maze.currentNode.X] = true;
        queue.add(maze.currentNode);

        // perform BFS
        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();

            // check if the current node is the endNode
            // (why .equals and not .same?)
            if (currentNode.equals(maze.endNode)) {
                break;
            }
            
            // explore the neighbors of the current node
            for (int y=currentNode.Y-1; y<=currentNode.Y+1; y++) {
                for (int x=currentNode.X-1; x<=currentNode.X+1; x++) {
                    // skip if the neighbor is out of bounds, or is not walkable

                    Node newNode = new Node(x, y);

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
        Node currentNode = maze.endNode;
        while (currentNode != null) {
            shortestPath.addFirst(currentNode);
            currentNode = parent[currentNode.Y][currentNode.X];
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
        return (x == node.X || y == node.Y);
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
        return !(x == node.X && y == node.Y);
    }

    public LinkedList<Node> findLongestPath() {

        reset();
        longestPath.clear();

        exploreNewNode();

        return longestPath;
    }

    private boolean walkable(Node node) {
        int x = node.X;
        int y = node.Y;

        if (x < 0 || x >= mazeCopy.width) {
            return false;
        }
        
        if (y < 0 || y >= mazeCopy.height) {
            return false;
        }
        
        return mazeCopy.get(x, y) != Node.Type.BLOCKED && mazeCopy.get(x, y) != Node.Type.WALL;
    }

    private boolean exploreNewNode() {


        accumulatorPath.add(mazeCopy.currentNode);

        if (mazeCopy.currentNode.equals(mazeCopy.endNode) && mazeCopy.get(mazeCopy.currentNode) == Node.Type.END) {
            // if end node: check if longer than last saved path

            if (accumulatorPath.size() > longestPath.size()) {
                longestPath = new LinkedList<>(accumulatorPath);

                if (mazeCopy.pathHistory.size() == mazeCopy.creationPath.size()) {
                    // longest possible path found (no need to continue searching)
                    return true;
                }
            }
        }
    
        else {

            // explore all possible directions from the current node
            for (Maze.Direction dir : Maze.Direction.values()) {
                
                if (mazeCopy.userMove(dir)) {
                    if (exploreNewNode()) {
                        return true;
                    }
                    mazeCopy.step(-1);
                }
            }
        }
        
        // finally: all possible directions from currentNode have been explored,
        // therefore: backtrack to the previous node and continue exploring from there
        accumulatorPath.pop();
        return false;
    }

}
