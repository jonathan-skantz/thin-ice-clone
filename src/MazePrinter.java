import java.util.LinkedList;

public class MazePrinter {

    private static String getFormatted(String str) {
        // the number in the format should be one larger than
        // the longest strRep of Node.Type

        // TODO: get length without looping through
        // Node.Type.values() every call.
        return String.format("%3s", str);
    }

    public static void printMazeWithPath(LinkedList<Node> path) {
        
        String[][] mazeOfStr = getMazeWithTypes();

        // set numbers that represent the order of steps of the (a) solution
        for (int i=0; i<path.size(); i++) {
            Node n = path.get(i);
            mazeOfStr[n.y][n.x] = getFormatted(String.valueOf(i));
        }

        System.out.println(mazeOfStrToStr(mazeOfStr));
    }

    public static void printMazeWithTypes() {
        System.out.println(mazeOfStrToStr(getMazeWithTypes()));
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
    private static String[][] getMazeWithTypes() {
        
        String[][] mazeOfStr = new String[MazeGen.getHeight()][MazeGen.getWidth()];

        for (int y=0; y<MazeGen.getHeight(); y++) {
            for (int x=0; x<MazeGen.getWidth(); x++) {
                mazeOfStr[y][x] = getFormatted(MazeGen.get(x, y).strRep);
            }
        }
    
        return mazeOfStr;
    }


}
