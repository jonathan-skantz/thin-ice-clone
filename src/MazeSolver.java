import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class MazeSolver {

    private Node.Type[][] mazeCopy;
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
        reset();
    }

    private void reset() {

        mazeCopy = new Node.Type[maze.height][maze.width];
        for (int y=0; y<maze.height; y++) {
            for (int x=0; x<maze.width; x++) {
                mazeCopy[y][x] = maze.types[y][x];
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

        exploreNewNodeFrom(maze.currentNode);

        return longestPath;
    }

    private boolean walkable(Node node) {
        int x = node.X;
        int y = node.Y;

        if (x < 0 || x >= mazeCopy[0].length) {
            return false;
        }
        
        if (y < 0 || y >= mazeCopy.length) {
            return false;
        }
        
        Node.Type t = mazeCopy[y][x];

        return t != Node.Type.BLOCKED && t != Node.Type.WALL;
    }

    private void markAsWalked(Node node) {
        
        if (mazeCopy[node.Y][node.X] == Node.Type.DOUBLE) {
            mazeCopy[node.Y][node.X] = Node.Type.TOUCHED;
        }
        else if (mazeCopy[node.Y][node.X] == Node.Type.END_DOUBLE) {
            mazeCopy[node.Y][node.X] = Node.Type.END;
        }
        else {
            mazeCopy[node.Y][node.X] = Node.Type.BLOCKED;
        }
    }

    private void unwalk(Node node) {
        if (mazeCopy[node.Y][node.X] == Node.Type.TOUCHED) {
            mazeCopy[node.Y][node.X] = Node.Type.DOUBLE;        // TODO: could also be set to END_DOUBLE
        }
        else if (mazeCopy[node.Y][node.X] == Node.Type.END) {
            mazeCopy[node.Y][node.X] = Node.Type.END_DOUBLE;
        }
        else {
            mazeCopy[node.Y][node.X] = Node.Type.GROUND;
        }
    }

    private void exploreNewNodeFrom(Node currentNode) {

        // TODO: exit once a longest path is found

        accumulatorPath.add(currentNode);
        
        if (currentNode.equals(maze.endNode) && mazeCopy[currentNode.Y][currentNode.X] == Node.Type.END) {
            // if end node: check if longer than last saved path

            if (accumulatorPath.size() > longestPath.size()) {
                // TODO: check only if size is same as remaining nodes --> return
                longestPath = new LinkedList<>(accumulatorPath);
            }
        }
    
        else {

            // explore all possible directions from the current node
            for (Maze.Direction dir : Maze.Direction.values()) {
                
                Node newNode = currentNode.getNeighbor(dir);
                
                if (walkable(newNode)) {
                    
                    markAsWalked(currentNode);
                    
                    exploreNewNodeFrom(newNode);
                    
                    unwalk(currentNode);
                }
            }
        }
        
        // finally: all possible directions from currentNode have been explored,
        // therefore: backtrack to the previous node and continue exploring from there
        accumulatorPath.pop();
    }

}
