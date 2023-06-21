/**
 * Represents a node in the maze.
 */
public class Node {

    public enum Type {
        WALL("-"),
        GROUND("G"),
        START("S"),
        END("E"),
        BLOCKED("B"),
        DOUBLE("2x"),
        TOUCHED("x");   // represents a double that has been stepped on once

        public String strRep;

        private Type(String rep) {
            strRep = rep;
        }
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

    public boolean nextTo(Node node) {
        boolean vertically = node.x == x && (node.y == y+1 || node.y == y-1);
        boolean horizontally = node.y == y && (node.x == x+1 || node.x == x-1);

        return vertically || horizontally;
    }

}
