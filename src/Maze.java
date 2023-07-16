import java.util.ArrayList;
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

    public ArrayList<Node> getNeighborsOf(Node node) {
        
        ArrayList<Node> neighbors = new ArrayList<>(4);
        for (int[] change : MazeGen.dirChange) {
            Node neighbor = node.getNeighbor(change[0], change[1]);
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

    public void removeNode(Node node) {
        // Node.Type type = types[node.Y][node.X];
        types[node.Y][node.X] = null;
        // nodes.get(type).remove(node);
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

        int[] change = action.getMovement();
        Node newNode = currentNode.getNeighbor(change[0], change[1]);

        if (nodeWithinBounds(newNode) && nodeTypeWalkable(newNode)) {
            
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

                else {
                    set(lastNode, Node.Type.GROUND);
                }

                // adjust currentNode
                if (currentNode.equals(startNode)) {
                    set(currentNode, Node.Type.START);
                }
                else if (getOriginal(currentNode) == Node.Type.DOUBLE) {
                    set(currentNode, Node.Type.TOUCHED);
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

    private boolean nodeTypeWalkable(Node node) {
        return get(node) != Node.Type.WALL && get(node) != Node.Type.BLOCKED;
    }

}