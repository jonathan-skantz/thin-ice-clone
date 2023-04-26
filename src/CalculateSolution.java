import java.util.*;

public class CalculateSolution {
    private static int[][] maze;
    private boolean[][] visited;
    private int dimension;
    private Node startNode;
    private Node endNode;

    /**
     * Constructor for CalculateSolution class.
     *
     * @param maze a 2D array representing the maze
     */
    CalculateSolution(int[][] maze, Node start, Node end) {
        CalculateSolution.maze = maze;
        this.dimension = maze.length;
        this.visited = new boolean[dimension][dimension];
        this.startNode = start;
        this.endNode = end;
    }

    /**
     * Calculates the path to solve the maze using depth-first search.
     *
     * @return a LinkedList representing the path to solve the maze
     */
    public LinkedList<Node> calculatePath() {
        Stack<Node> stack = new Stack<>();
        LinkedList<Node> path = new LinkedList<>();
        stack.push(startNode);
        visited[startNode.y][startNode.x] = true;

        while (!stack.empty()) {
            Node currNode = stack.pop();
            path.add(currNode);

            if (currNode.x == endNode.x && currNode.y == endNode.y) {
                return path;
            }

            for (Node neighbor : findNeighbors(currNode)) {
                if (!visited[neighbor.y][neighbor.x]) {
                    stack.push(neighbor);
                    visited[neighbor.y][neighbor.x] = true;
                }
            }
        }

        return path;
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

    /**
     * Finds the neighbors of a given node.
     *
     * @param node the node to find neighbors for
     * @return an ArrayList of neighboring nodes
     */
    private ArrayList<Node> findNeighbors(Node node) {
        ArrayList<Node> neighbors = new ArrayList<>();
        for (int y = node.y-1; y < node.y+2; y++) {
            for (int x = node.x-1; x < node.x+2; x++) {
                if (pointOnGrid(x, y) && pointNotCorner(node, x, y) && pointNotNode(node, x, y)) {
                    neighbors.add(new Node(x, y));
                }
            }
        }
        return neighbors;
    }

}
