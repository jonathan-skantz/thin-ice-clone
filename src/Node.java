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
        TOUCHED("x"),   // represents a double that has been stepped on once
        END_DOUBLE("2E");

        private String strRep;

        private Type(String rep) {
            strRep = rep;
        }

        @Override
        public String toString() {
            return strRep;
        }
    }

    public final int X;
    public final int Y;

    public Node(int x, int y) {
        this.X = x;
        this.Y = y;
    }

    public Node getNeighbor(Maze.Direction direction) {
        return new Node(X+direction.dx, Y+direction.dy);
    }

    @Override
    public String toString() {
        return String.format("N(%d,%d)", X, Y);
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
        return X == node.X && Y == node.Y;
    }

}
