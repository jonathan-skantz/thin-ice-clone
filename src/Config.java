import java.awt.Color;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

public class Config {
    
    public static MazeGen mazeGen = new MazeGen(Config.MAZE_DEFAULT_WIDTH, Config.MAZE_DEFAULT_HEIGHT);

    public static final int MAZE_DEFAULT_WIDTH = 5;
    public static final int MAZE_DEFAULT_HEIGHT = 5;
    
    // coordinates of topleft of maze
    public static int mazeStartX;
    public static int mazeStartY;

    // block graphics
    public static int blockSize = 30;

    public static final int BLOCK_BORDER_WIDTH = 1;
    public static final Color BLOCK_BORDER_COLOR = new Color(0, 0, 0, 50);
    public static final Border BLOCK_BORDER = BorderFactory.createLineBorder(BLOCK_BORDER_COLOR, BLOCK_BORDER_WIDTH);
    
    public static final Hashtable<Node.Type, Color> BLOCK_COLORS = new Hashtable<>() {
        {
            put(Node.Type.WALL, new Color(50, 50, 50));
            put(Node.Type.GROUND, new Color(225, 225, 255));
            put(Node.Type.START, Color.GREEN);
            put(Node.Type.END, Color.MAGENTA);
            put(Node.Type.BLOCKED, new Color(0, 50, 255));
            put(Node.Type.DOUBLE, new Color(0, 255, 255));
        }
    };

    // hints
    public static int hintMax = 3;
    public static final Color HINT_COLOR = new Color(150, 150, 255);
    public static boolean hintTypeLongest = true;   // either longest or shortest

    // flags for updates
    public static boolean newSize = false;
    public static boolean newHintMax = false;
    public static boolean newBlockColor = false;

    public static void setMazeWidth(int w) {
        mazeGen.width = w;
        newSize = true;
    }

    public static void setMazeHeight(int h) {
        mazeGen.height = h;
        newSize = true;
    }

    public static void setHintMax(int v) {
        hintMax = v;
        newHintMax = true;
    }

    public static void setMazeDesiredPathLength(int v) {
        mazeGen.desiredPathLength = v;
    }

    public static void setBlockColor(Node.Type block, Color color) {
        BLOCK_COLORS.replace(block, color);
        newBlockColor = true;
    }

}
