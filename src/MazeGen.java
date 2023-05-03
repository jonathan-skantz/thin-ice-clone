/*

TODO:

* prevent start and end next to each other
* minimum path length
* maze ends only at edge of maze or when colliding with a path node
* no dead ends


SOLUTIONS:
* start somewhere in the middle?
* if no up,down,left,right (or permutation): regenerate
* place random walls throughout the empty maze to begin with

*/
import java.util.ArrayList;
// import java.util.Arrays;
import java.util.Random;

public class MazeGen {
 
    private final String WALL_STR = "--";
    private final String STR_FORMAT = "%" + WALL_STR.length() + "s";

    public ArrayList<Node> path = new ArrayList<>();
    
    private Random rand = new Random();
    private Node.Type[][] maze;

    public int width;
    public int height;
    
    public Node startNode;
    public Node endNode;
    
    // used to generate the maze as well as 
    // keep track of the player
    public Node currentNode;

    private final int[] DIR = new int[] {1, -1};

    public static void main(String[] args) {
        MazeGen mg = new MazeGen(10, 10);
        mg.generate();
        System.out.println(mg);
    }

    public MazeGen(int w, int h) {
        width = w;
        height = h;
    }

    public void set(Node node, Node.Type type) {
        maze[node.y][node.x] = type;
    }
    
    public void set(int x, int y, Node.Type type) {
        maze[y][x] = type;
    }

    public Node.Type get(Node node) {
        return maze[node.y][node.x];
    }

    public Node.Type get(int x, int y) {
        return maze[y][x];
    }

    // TEMP
    public boolean pointOnGrid(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public void generate() {

        // reset maze
        maze = new Node.Type[height][width];
        path.clear();

        // set random start
        int startX = rand.nextInt(width);
        int startY = rand.nextInt(height);
        startNode = new Node(startX, startY);

        currentNode = startNode;

        do {
            path.add(currentNode);
            set(currentNode, Node.Type.GROUND);
        }

        while (getNextNode());

        // change default from null to Node.Type.WALL
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                if (get(x, y) == null) {
                    set(x, y, Node.Type.WALL);
                }
            }
        }

        endNode = currentNode;

        maze[startNode.y][startNode.x] = Node.Type.START;
        maze[endNode.y][endNode.x] = Node.Type.END;

    }
    
    
    // private void printPath() {
    //     // TODO: str builder

    //     System.out.println("nodes: " + path.size());
        
    //     // int i = 0;
    //     // for (Node n : path) {
    //     //     System.out.print(i + ":(" + n.x + "," + n.y + ") ");
    //     //     i++;
    //     // }

    //     System.out.println();
    //     int lastX = path.get(0).x;
    //     int lastY = path.get(0).y;
    //     int dx = 0;
    //     int dy = 0;

    //     path.remove(0);
    //     int i = 0;

    //     for (Node n : path) {
    //         System.out.print(i + ":");
    //         dx = n.x - lastX;
    //         dy = n.y - lastY;
    //         if (dx != 0) {
    //             lastX = n.x;
                
    //             if (dx == 1) {
    //                 System.out.print("r ");
    //             }
    //             else {
    //                 System.out.print("l ");
    //             }
    //         }
    //         else if (dy != 0) {
    //             lastY = n.y;

    //             if (dy == 1) {
    //                 System.out.print("d ");
    //             }
    //             else {
    //                 System.out.print("u ");
    //             }
    //         }
    //         i++;
    //     }
    //     System.out.println();
    // }

    private boolean getNextNode() {

        boolean tryDxFirst = getRandomDirection() == 1;

        if (tryDxFirst) {
            if (!tryMoveDx()) {
                return tryMoveDy();
            }
            return true;
        }

        else {
            if (!tryMoveDy()) {
                return tryMoveDx();
            }
            return true;
        }
    }

    private int getRandomDirection() {
        return DIR[rand.nextInt(2)];
    }

    private boolean tryMoveDx() {
        
        int dx = getRandomDirection();
        
        if (!validMove(dx, 0)) {
            dx *= -1;
            if (!validMove(dx, 0)) {
                return false;
            }
        }

        currentNode = new Node(currentNode.x + dx, currentNode.y);
        return true;
    }

    private boolean tryMoveDy() {
        
        int dy = getRandomDirection();
        
        if (!validMove(0, dy)) {
            dy *= -1;
            if (!validMove(0, dy)) {
                return false;
            }
        }
        
        currentNode = new Node(currentNode.x, currentNode.y + dy);
        return true;
    }

    public boolean nodeWalkable(int x, int y) {
        return get(x, y) != Node.Type.WALL && get(x, y) != Node.Type.BLOCKED;
    }

    private boolean validMove(int dx, int dy) {
        
        // within horizontal width
        int newX = currentNode.x + dx;
        if (newX < 0 || newX >= width) {
            return false;
        }
        
        // within vertical width
        int newY = currentNode.y + dy;
        if (newY < 0 || newY >= height) {
            return false;
        }
        
        // not visited before
        for (Node n : path) {
            if (n.x == newX && n.y == newY) {
                return false;
            }
        }

        return true;
    }

    public Node.Type[][] getMaze() {
        return maze;
    }

    public String toString() {
        
        ArrayList<Node.Type> vals = new ArrayList<>();

        for (Node.Type b : Node.Type.values()) {
            vals.add(b);
        }
        
        String[][] arrVisual = new String[height][width];

        int i = 0;
        for (int nodeCount=0; nodeCount < path.size(); nodeCount++) {
            Node n = path.get(i);
            arrVisual[n.y][n.x] = String.valueOf(i);
            i++;
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {

                String str = arrVisual[y][x];

                if (str == null) {
                    char s = maze[y][x].toString().charAt(0);

                    if (String.valueOf(s).compareTo("W") == 0) {
                        sb.append(WALL_STR);
                    }
                    else {
                        sb.append(String.format(STR_FORMAT, s));
                    }
                }
                else {
                    sb.append(String.format(STR_FORMAT, str));
                }

                sb.append(" ");

            }

            sb.append("\n");
        }
        


        return sb.toString();
    }


}
