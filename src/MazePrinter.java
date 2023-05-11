import java.util.LinkedList;

public class MazePrinter {

    private static String getFormatted(String str) {
        // the number in the format should be one larger than
        // the longest strRep of Node.Type

        // TODO: get length without looping through
        // Node.Type.values() every call.
        return String.format("%3s", str);
    }

    public static void printMazeWithPath(Node.Type[][] maze, LinkedList<Node> path) {
        
        String[][] mazeOfStr = getMazeWithTypes(maze);

        // set numbers that represent the order of steps of the (a) solution
        for (int i=0; i<path.size(); i++) {
            Node n = path.get(i);
            mazeOfStr[n.y][n.x] = getFormatted(String.valueOf(i));
        }

        System.out.println(mazeOfStrToStr(mazeOfStr));
    }

    public static void printMazeWithTypes(Node.Type[][] maze) {
        System.out.println(mazeOfStrToStr(getMazeWithTypes(maze)));
    }

    // convert a 2d-array to a string
    private static String mazeOfStrToStr(String[][] m) {

        StringBuilder sb = new StringBuilder();

        for (int y=0; y<m.length; y++) {
            for (int x=0; x<m[0].length; x++) {
                sb.append(m[y][x]);
            }
            sb.append('\n');
        }
    
        return sb.toString();
    }
    
     // generate a maze of string representations of the nodes
    private static String[][] getMazeWithTypes(Node.Type[][] maze) {
        
        String[][] mazeOfStr = new String[maze.length][maze[0].length];

        for (int y=0; y<maze.length; y++) {
            for (int x=0; x<maze[0].length; x++) {
                mazeOfStr[y][x] = getFormatted(maze[y][x].strRep);
            }
        }
    
        return mazeOfStr;
    }


}
