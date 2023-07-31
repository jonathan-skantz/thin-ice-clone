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
        Config.applyDefault();
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

        mazeCopy.currentNode = maze.currentNode;
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
        Node[][] parent = new Node[mazeCopy.height][mazeCopy.width];

        // mark the startNode as visited and add it to the queue
        boolean[][] visited = new boolean[mazeCopy.height][mazeCopy.width];
        visited[mazeCopy.currentNode.Y][mazeCopy.currentNode.X] = true;
        queue.add(mazeCopy.currentNode);

        // perform BFS
        while (!queue.isEmpty()) {
            mazeCopy.currentNode = queue.poll();

            // check if the current node is the endNode
            if (mazeCopy.currentNode.equals(maze.endNode)) {
                break;
            }
            
            for (Node neighbor : mazeCopy.getNeighborsOf(mazeCopy.currentNode, true)) {
                if (!visited[neighbor.Y][neighbor.X]) {
                    visited[neighbor.Y][neighbor.X] = true;
                    queue.add(neighbor);
                    parent[neighbor.Y][neighbor.X] = mazeCopy.currentNode;
                }
            }
        }

        // build shortest path
        Node currentNode = maze.endNode;
        while (currentNode != null) {
            shortestPath.addFirst(currentNode);
            currentNode = parent[currentNode.Y][currentNode.X];
        }

        if (shortestPath.size() == 1 && maze.get(maze.currentNode) != Node.Type.END_DOUBLE) {
            shortestPath.clear();
        }

        // if end is double, two more steps must be added
        else if (mazeCopy.get(mazeCopy.endNode) == Node.Type.END_DOUBLE) {
            for (Node neighbor : mazeCopy.getNeighborsOf(mazeCopy.endNode, true)) {

                boolean touched = shortestPath.contains(neighbor);

                if (!touched || (touched && mazeCopy.get(neighbor) == Node.Type.DOUBLE)) {
                    shortestPath.add(neighbor);
                    shortestPath.add(mazeCopy.endNode);
                    break;
                }
            }
        }

        return shortestPath;
    }

    public LinkedList<Node> findLongestPath() {

        reset();
        longestPath.clear();

        exploreNewNode();

        if (longestPath.size() == 1) {
            longestPath.clear();
        }

        return longestPath;
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
