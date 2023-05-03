import java.util.LinkedList;
import java.util.Queue;

public class CalculateSolution {

    private int[][] maze;
    private int dimension;
    private Node startNode;
    private Node endNode;

    CalculateSolution(int[][] maze, Node startNode, Node endNode) {
        this.maze = maze;
        this.dimension = maze.length;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    /**
     * Finds the shortest path from startNode to endNode in the maze.
     *
     * @return an ArrayList of Nodes representing the shortest path from startNode to endNode
     */
    public LinkedList<Node> findShortestPath() {
        // create a queue for BFS
        Queue<Node> queue = new LinkedList<>();

        // create an array to store the parent node of each node in the shortest path
        Node[][] parent = new Node[dimension][dimension];

        // initialize the parent array to null
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                parent[i][j] = null;
            }
        }

        // mark the startNode as visited and add it to the queue
        boolean[][] visited = new boolean[dimension][dimension];
        visited[startNode.y][startNode.x] = true;
        queue.add(startNode);

        // perform BFS
        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();

            // check if the current node is the endNode
            if (currentNode.equals(endNode)) {
                break;
            }

            // explore the neighbors of the current node
            for (int i = currentNode.y-1; i <= currentNode.y+1; i++) {
                for (int j = currentNode.x-1; j <= currentNode.x+1; j++) {
                    // skip if the neighbor is out of bounds, or is not walkable
                    if (!pointOnGrid(j, i) || !pointNotCorner(currentNode, j, i) || !pointNotNode(currentNode, j, i) || maze[i][j] == 0 || visited[i][j]) {
                        continue;
                    }

                    // mark the neighbor as visited, add it to the queue, and set its parent
                    visited[i][j] = true;
                    Node nextNode = new Node(j, i);
                    queue.add(nextNode);
                    parent[i][j] = currentNode;
                }
            }
        }

        // construct the shortest path from startNode to endNode
        LinkedList<Node> shortestPath = new LinkedList<>();
        Node currentNode = endNode;
        while (currentNode != null) {
            shortestPath.addFirst(currentNode);
            currentNode = parent[currentNode.y][currentNode.x];
        }

        return shortestPath;
    }

    /**
     * Helper method that checks if a given point is on the grid
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     * @return true if the point is on the grid, false otherwise
     */
    private Boolean pointOnGrid(int x, int y) {
        return x >= 0 && y >= 0 && x < dimension && y < dimension;
    }

    /**
     * Helper method that checks if a given point is not diagonally adjacent to a given node
     * 
     * @param node the node to compare against
     * @param x x-coordinate
     * @param y y-coordinate
     * @return true if the point is not diagonally adjacent to the given node, false otherwise
     */
    private Boolean pointNotCorner(Node node, int x, int y) {
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
    private Boolean pointNotNode(Node node, int x, int y) {
        return !(x == node.x && y == node.y);
    }

}