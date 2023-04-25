/**
 * A class that generates a maze using the depth-first search algorithm
 * @author Elias Hollstrand
 */

import java.util.ArrayList;
import java.util.Stack;
import java.util.Random;
import java.util.Arrays;

class MazeGenerator {

    /**
     * Represents a node in the maze.
     */
    public class Node {
        public final int x;
        public final int y;
    
        /**
         * Constructor for Node class.
         *
         * @param x x-coordinate of node
         * @param y y-coordinate of node
         */
        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    private Stack<Node> stack = new Stack<>();
    private Random rand = new Random();
    private int[][] maze;
    private int dimension;
    private static Node startNode;
    private static Node endNode;

    /**
     * Constructor for MazeGenerator class.
     *
     * @param dim dimension of the maze
     */
    MazeGenerator(int dim) {
        maze = new int[dim][dim];
        dimension = dim;
    }

    /**
     * Generates a random maze using a modified depth-first search algorithm.
     */
    public void generateMaze() {
        // Randomize starting position
        int startX = rand.nextInt(dimension);
        int startY = rand.nextInt(dimension);
        Node firstNode = new Node(startX, startY);
        stack.push(firstNode);
        startNode = firstNode; // Save the first node
        
        while (!stack.empty()) {
            Node next = stack.pop();
            if (isValidNextNode(next)) {
                maze[next.y][next.x] = 1;
                endNode = next;
                ArrayList<Node> neighbors = findNeighbors(next);
                addNodesToStack(neighbors);
            }
        }
    }

    /**
     * Determines whether a given node is a valid next node to visit.
     *
     * @param node the node to check
     * @return true if the node is valid, false otherwise
     */
    private boolean isValidNextNode(Node node) {
        int numNeighboringOnes = 0;
        for (int y = node.y-1; y < node.y+2; y++) {
            for (int x = node.x-1; x < node.x+2; x++) {
                if (pointOnGrid(x, y) && pointNotNode(node, x, y) && maze[y][x] == 1) {
                    numNeighboringOnes++;
                }
            }
        }
        return (numNeighboringOnes < 3) && maze[node.y][node.x] != 1;
    }

    /**
     * Adds the given nodes to the stack in random order.
     *
     * @param nodes the nodes to add to the stack
     */
    private void addNodesToStack(ArrayList<Node> nodes) {
        int targetIndex;
        while (!nodes.isEmpty()) {
            targetIndex = rand.nextInt(nodes.size());
            stack.push(nodes.remove(targetIndex));
        }
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
     * Creates a string representation of the maze
     * 
     * @return a string representation of the maze
     */
    public String printRepresentation() {
        StringBuilder sb = new StringBuilder();
        for (int[] row : maze) {
            sb.append(Arrays.toString(row) + "\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        MazeGenerator mazeGenerator = new MazeGenerator(10);
        mazeGenerator.generateMaze();

        System.out.println(mazeGenerator.printRepresentation());
        System.out.println("Start node: (" + startNode.x + "," + startNode.y + ")");
        System.out.println("End node: (" + endNode.x + "," + endNode.y + ")");
    }
}

