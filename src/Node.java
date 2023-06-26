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
        return String.format("Node(%d,%d)", x, y);
    }

    // returns true if same x and y as `this`
    @Override
    public boolean equals(Object obj) {
        
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        Node node = (Node) obj;
        return x == node.x && y == node.y;
    }

}
