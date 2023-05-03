/**
 * Represents a node in the maze.
 */
public class Node {

    public enum Type {
        WALL,
        GROUND,
        START,
        END,
        BLOCKED
    }

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
    
    @Override
    public String toString() {
        return "Node(" + x + "," + y + ")";
    }

    public boolean same(Node node) {
        return x == node.x && y == node.y;
    }

}
