/**
 * Represents a node in the maze.
 */
public class Node {
   
    public final int x;
    public final int y;
    public Node prev;

    /**
     * Constructor for Node class.
     *
     * @param x x-coordinate of node
     * @param y y-coordinate of node
     */
    Node(int x, int y) {
        this.x = x;
        this.y = y;
        this.prev = null;
    }
    
}
