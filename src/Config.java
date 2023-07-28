import java.awt.Color;
import java.util.Hashtable;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

public class Config {
    
    public static boolean multiplayer = false;

    public static Random rand = new Random(1);

    public static final int MAZE_WIDTH_MIN = 1;
    public static final int MAZE_DEFAULT_WIDTH = 5;
    public static final int MAZE_WIDTH_MAX = 20;

    public static final int MAZE_HEIGHT_MIN = 1;
    public static final int MAZE_DEFAULT_HEIGHT = 5;
    public static final int MAZE_HEIGHT_MAX = 20;

    public static final int DEFAULT_AMOUNT_GROUND = 10;
    public static final int DEFAULT_AMOUNT_DOUBLES = 2;

    // block graphics
    public static int blockSize = 30;
    
    public static final Hashtable<Node.Type, Color> BLOCK_COLORS = new Hashtable<>() {{
        put(Node.Type.WALL, new Color(50, 50, 50));
        put(Node.Type.GROUND, new Color(225, 225, 255));
        put(Node.Type.START, Color.GREEN);
        put(Node.Type.END, Color.MAGENTA);
        put(Node.Type.BLOCKED, new Color(0, 50, 255));
        
        put(Node.Type.DOUBLE, get(Node.Type.GROUND));
        put(Node.Type.TOUCHED, get(Node.Type.GROUND));
        put(Node.Type.END_DOUBLE, get(Node.Type.END));
    }};
    
    // separate border to visualize the new blocks during generation
    public static final Border BLOCK_BORDER_GEN = BorderFactory.createLineBorder(new Color(255, 255, 150), 2);

    // hints
    public static int hintMax = 1;
    public static final Color HINT_COLOR = new Color(150, 150, 255);
    public static boolean hintTypeLongest = true;   // either longest or shortest
    public static boolean showUnsolvable = true;

    // flags for updates
    public static boolean newHintMax = false;
    public static boolean newBlockColor = false;

    public static void apply() {
        // setup maze config
        MazeGen.setSize(MAZE_DEFAULT_WIDTH, MAZE_DEFAULT_HEIGHT);
        MazeGen.Amount.GROUND.set(DEFAULT_AMOUNT_GROUND);
        MazeGen.Amount.DOUBLES.set(DEFAULT_AMOUNT_DOUBLES);
    }

    public static void setHintMax(int v) {
        hintMax = v;
        newHintMax = true;
    }

    public static void setBlockColor(Node.Type block, Color color) {
        BLOCK_COLORS.replace(block, color);
        newBlockColor = true;
    }

}
