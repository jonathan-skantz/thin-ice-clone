import java.awt.Color;
import java.util.HashMap;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

public class Config {

    public static boolean multiplayer = false;  // either online or offline
    public static boolean multiplayerOnline = false;
    public static boolean multiplayerOffline = false;

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
    
    // separate border to visualize the new blocks during generation
    public static final Border BLOCK_BORDER_GEN = BorderFactory.createLineBorder(new Color(255, 255, 150), 2);

    // hints
    public static final Color HINT_COLOR = new Color(150, 150, 255);
    public static boolean hintTypeLongest = true;   // either longest or shortest

    // flags for updates
    public static boolean newHintMax = false;

    // host settings
    public static boolean hostMirrorOpponent = false;
    public static boolean hostShowUnsolvable = true;
    public static boolean hostShowAnimations = true;
    public static float hostHintLength = 0.05f;     // in percent

    // only used to sending settings through socket
    public static HashMap<String, Object> getHostSettings() {
        return new HashMap<>() {{
            put("hostMirrorOpponent", hostMirrorOpponent);
            put("hostShowUnsolvable", hostShowUnsolvable);
            put("hostShowAnimations", hostShowAnimations);
            put("hostHintLength", hostHintLength);
        }};
    }

    public static void applyDefault() {
        // setup maze config
        MazeGen.setSize(MAZE_DEFAULT_WIDTH, MAZE_DEFAULT_HEIGHT);
        MazeGen.Amount.GROUND.set(DEFAULT_AMOUNT_GROUND);
        MazeGen.Amount.DOUBLES.set(DEFAULT_AMOUNT_DOUBLES);
    }

}
