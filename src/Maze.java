import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;

public class Maze {
    
    public boolean complete = false;

    // keep track of the user's path, in order to be able to backtrack
    public Stack<Node> pathHistory = new Stack<>();
    private Stack<Node> pathHistoryRedo = new Stack<>();

    public LinkedList<Node> creationPath = new LinkedList<>();

    public final int width;
    public final int height;
    
    public Node startNode;
    public Node endNode;
    public Node currentNode;    // used to keep track of the player

    public Node.Type[][] types;
    public Node.Type[][] typesOriginal;

    public enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);

        public final int dx;
        public final int dy;

        private Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    public Maze(int width, int height, Node.Type firstType) {
        this.width = width;
        this.height = height;
        
        types = new Node.Type[height][width];

        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                types[y][x] = firstType;
            }
        }

        typesOriginal = new Node.Type[height][width];
    }

    public void printTypes() {
        
        HashSet<Integer> wideCols = new HashSet<>();
        
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                if (get(x, y) == Node.Type.DOUBLE || get(x, y) == Node.Type.END_DOUBLE) {
                    wideCols.add(x);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                if (wideCols.contains(x)) {
                    sb.append(centerPad(get(x, y).toString(), 3));
                }
                else {
                    sb.append(centerPad(get(x, y).toString(), 2));
                }
            }
            sb.append('\n');
        }

        System.out.println(sb);
    }

    public void printCreationPath() {
        printPath(creationPath);
    }

    private static String centerPad(String str, int totalWidth) {
        int spacesToAdd = totalWidth - str.length();
        if (spacesToAdd <= 0) {
            return str;
        }

        int leftSpaces = spacesToAdd / 2;
        int rightSpaces = spacesToAdd - leftSpaces;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < leftSpaces; i++) {
            sb.append(' ');
        }
        sb.append(str);
        for (int i = 0; i < rightSpaces; i++) {
            sb.append(' ');
        }

        return sb.toString();
    }

    public void printPath(LinkedList<Node> path) {
        
        StringBuilder sb = new StringBuilder();

        int widest = 2;
        HashSet<Integer> wideCols = new HashSet<>();

        for (int i=0; i<path.size(); i++) {
            Node node = path.get(i);

            int i1 = path.indexOf(node);
            int i2 = path.lastIndexOf(node);

            if (i1 != i2) {
                widest = String.valueOf(i1 + "(" + i2 + ")").length() + 1;
                wideCols.add(node.X);
            }
        }

        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                Node node = new Node(x, y);
                int i1 = path.indexOf(node);
                int i2 = path.lastIndexOf(node);

                int w;
                if (wideCols.contains(x)) {
                    w = widest;
                }
                else if (wideCols.contains(x-1)) {
                    w = 2;
                }
                else {
                    w = 3;
                }

                if (i1 == -1) {
                    sb.append(centerPad(Node.Type.WALL.toString(), w));
                }
                else if (i1 != i2) {
                    sb.append(centerPad(creationPath.indexOf(node) + "(" + i2 + ")", w));
                }
                else {
                    sb.append(centerPad(String.valueOf(i1), w));
                }
            }
            sb.append('\n');
        }

        System.out.println(sb);
    }

    public ArrayList<Node> getNeighborsOf(Node node) {
        
        ArrayList<Node> neighbors = new ArrayList<>(4);
        for (Direction dir : Direction.values()) {
            Node neighbor = node.getNeighbor(dir);
            if (nodeWithinBounds(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    private Node.Type getOriginal(Node node) {
        return typesOriginal[node.Y][node.X];
    }

    public Node.Type get(Node node) {
        return types[node.Y][node.X];
    }

    public Node.Type get(int x, int y) {
        return types[y][x];
    }

    public void set(Node node, Node.Type type) {
        types[node.Y][node.X] = type;
    }

    public void set(int x, int y, Node.Type type) {
        types[y][x] = type;
    }

    public void setStartNode(Node node) {
        startNode = node;
        set(startNode, Node.Type.START);
        
        creationPath.add(startNode);
        pathHistory.add(startNode);

        currentNode = startNode;
    }

    // returns true if valid move, false if invalid move
    public boolean userMove(KeyHandler.ActionKey action) {

        Node newNode = currentNode.getNeighbor(action.toDirection());

        if (walkable(newNode)) {
            
            if (get(currentNode) == Node.Type.DOUBLE) {
                set(currentNode, Node.Type.TOUCHED);
            }
            else if (get(currentNode) == Node.Type.END_DOUBLE) {
                set(currentNode, Node.Type.END);
            }
            else {
                set(currentNode, Node.Type.BLOCKED);
            }
            
            pathHistory.add(newNode);

            if (pathHistoryRedo.size() > 0) {
                if (pathHistoryRedo.peek().equals(newNode)) {
                    pathHistoryRedo.pop();
                }
                else {
                    // new history is made, clear last record of redos
                    pathHistoryRedo.clear();
                }
            }
            
            if (newNode.equals(endNode) && get(newNode) == Node.Type.END) {
                complete = true;
            }

            currentNode = newNode;
            return true;
        }

        return false;
    }

    public boolean nodeWithinBounds(Node node) {
        return node.X >= 0 && node.X < width && node.Y >= 0 && node.Y < height;
    }

    public void reset() {
        
        currentNode = startNode;

        pathHistory.clear();
        pathHistoryRedo.clear();
        
        pathHistory.add(startNode);

        complete = false;

        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                types[y][x] = typesOriginal[y][x];
            }
        }
    }

    // returns ActionKey in which grid direction the step occured
    public KeyHandler.ActionKey step(int direction){
        
        if (direction == -1) {
            
            if (pathHistory.size() > 1) {
                
                Node lastNode = pathHistory.pop();      // pops currentNode
                pathHistoryRedo.add(lastNode);
                
                currentNode = pathHistory.peek();

                // adjust lastNode
                if (get(lastNode) == Node.Type.TOUCHED) {

                    // don't set to double since it was
                    // just about to be set to GROUND when left
                    if (!pathHistory.contains(lastNode)) {
                        if (lastNode.equals(endNode)) {
                            set(lastNode, Node.Type.END_DOUBLE);
                        }
                        else {
                            set(lastNode, Node.Type.DOUBLE);
                        }
                    }
                }

                else if (get(lastNode) == Node.Type.BLOCKED) {
                    set(lastNode, Node.Type.GROUND);
                }

                // adjust currentNode
                if (currentNode.equals(startNode)) {
                    set(currentNode, Node.Type.START);
                }
                else if (getOriginal(currentNode) == Node.Type.DOUBLE || getOriginal(currentNode) == Node.Type.END_DOUBLE) {
                    set(currentNode, getOriginal(currentNode));
                }
                else {
                    set(currentNode, Node.Type.GROUND);
                }
                
                return KeyHandler.ActionKey.getActionFromMovement(lastNode, currentNode);
            }
        }
        else {
            if (pathHistoryRedo.size() > 0) {
                
                Node lastNode = currentNode;
                Node newNode = pathHistoryRedo.peek();      // NOTE: doesn't pop, since that is done in userMove()

                KeyHandler.ActionKey action = KeyHandler.ActionKey.getActionFromMovement(lastNode, newNode);
                userMove(action);
                
                return action;
            }
        }
        
        return null;
    }

    public boolean walkable(Node node) {
        return nodeWithinBounds(node) && get(node) != Node.Type.WALL && get(node) != Node.Type.BLOCKED;
    }

}