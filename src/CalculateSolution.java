import java.util.*;

public class CalculateSolution {
    private int[][] maze;
    private boolean[][] visited;
    private int dimension;
    private Node startNode;
    private Node endNode;

    /**
     * Constructor for CalculateSolution class.
     *
     * @param maze a 2D array representing the maze
     */
    CalculateSolution(int[][] maze, Node startNode, Node endNode) {
        this.maze = maze;
        this.dimension = maze.length;
        this.visited = new boolean[dimension][dimension];
        this.startNode = startNode;
        this.endNode = endNode;
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

            for (Node neighbor : getNeighbors(currNode)) {
                if (!visited[neighbor.y][neighbor.x]) {
                    stack.push(neighbor);
                    visited[neighbor.y][neighbor.x] = true;
                }
            }
        }

        return path;
    }

    /**
     * Returns an ArrayList of neighboring nodes that are valid next steps.
     *
     * @param node the current node
     * @return an ArrayList of neighboring nodes
     */
    private ArrayList<Node> getNeighbors(Node node) {
        ArrayList<Node> neighbors = new ArrayList<>();
        int x = node.x;
        int y = node.y;

        if (x > 0 && maze[y][x-1] == 1) {
            neighbors.add(new Node(x-1, y));
        }
        if (x < dimension-1 && maze[y][x+1] == 1) {
            neighbors.add(new Node(x+1, y));
        }
        if (y > 0 && maze[y-1][x] == 1) {
            neighbors.add(new Node(x, y-1));
        }
        if (y < dimension-1 && maze[y+1][x] == 1) {
            neighbors.add(new Node(x, y+1));
        }

        return neighbors;
    }
}
