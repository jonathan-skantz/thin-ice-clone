import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;

public enum Status {

    MAZE_EMPTY(0, null),
    WAITING_FOR_HOST_TO_GENERATE(0),
    
    WAITING_FOR_OPPONENT_TO_CONNECT(0),
    WAITING_FOR_HOST_TO_OPEN(0),

    GAME_WON(1),
    GAME_LOST(3),
    SURRENDERED(3),

    INCOMPLETE(2),
    UNSOLVABLE(3),
    
    READY(1),
    NOT_READY(0),
    PLAYING(0, null),
    
    // info
    RESETTING(0, "Resetting..."),
    GENERATING(0, "Generating..."),
    MIRRORING(0, "Mirroring..."),
    COPYING(0, "Copying...");

    public static final HashSet<Status> ANIMATIONS = new HashSet<>() {{
        add(RESETTING);
        add(GENERATING);
        add(MIRRORING);
        add(COPYING);
    }};
    public Color color;     // should be final but `COLORS` cannot be accessed within an initializer
    private final int colorIndex;
    
    public String stringStatus;

    public ArrayList<String> stringsP1 = new ArrayList<>();
    public ArrayList<String> stringsP2 = new ArrayList<>();

    public static String stringNewMaze;     // only used by host (who is P1 if offline or local)
    public static String stringUndoP1;
    public static String stringRedoP1;
    
    public static String stringUndoP2;
    public static String stringRedoP2;

    private static final Color[] COLORS = new Color[]{
        new Color(50, 50, 50),      // dark gray (info)
        new Color(50, 150, 50),     // green
        new Color(200, 150, 50),    // orange
        new Color(200, 50, 50),     // red
    };

    public static final String HTML_START = "<html><div style='text-align: center;'>";
    public static final String HTML_END = "</div></html>";

    static {
        // after all values have been constructed
        onNewControls();

        for (Status status : values()) {
            status.color = COLORS[status.colorIndex];
        }

    }

    // default display is the capitalized name of the enum field
    private Status(int colorIndex) {
        this.colorIndex = colorIndex;

        String[] words = toString().toLowerCase().split("_");

        if (words.length == 1) {
            stringStatus = capitalized(words[0]);
        }
        else if (words.length == 2) {
            stringStatus = capitalized(words[0]) + " " + words[1];
        }
        else {
            StringBuilder sb = new StringBuilder();
            sb.append(capitalized(words[0]));
            for (int i=1; i<words.length; i++) {
                sb.append(' ');
                sb.append(words[i]);
            }
            sb.append("...");
            stringStatus = sb.toString();
        }
    }

    private Status(int colorIndex, String stringStatus) {
        this.colorIndex = colorIndex;
        this.stringStatus = stringStatus;
    }

    // str is assumed to be lowercase
    private static String capitalized(String str) {
        return String.valueOf(str.charAt(0)).toUpperCase() + str.substring(1);
    }

    public static void onNewControls() {

        for (Status status : values()) {
            status.stringsP1.clear();
            status.stringsP2.clear();
        }

        stringNewMaze = "Press " + KeyEvent.getKeyText(KeyHandler.Action.MAZE_NEW.keyCode) + " to generate new maze.";

        stringUndoP1 = "Press " + KeyEvent.getKeyText(KeyHandler.Action.P1_MAZE_STEP_UNDO.keyCode) + " to undo.";
        stringRedoP1 = "Press " + KeyEvent.getKeyText(KeyHandler.Action.P1_MAZE_STEP_REDO.keyCode) + " to redo.";
        
        stringUndoP2 = "Press " + KeyEvent.getKeyText(KeyHandler.Action.P2_MAZE_STEP_UNDO.keyCode) + " to undo.";
        stringRedoP2 = "Press " + KeyEvent.getKeyText(KeyHandler.Action.P2_MAZE_STEP_REDO.keyCode) + " to redo.";
            
        PLAYING.stringsP1.add("Press " + KeyEvent.getKeyText(KeyHandler.Action.P1_SURRENDER.keyCode) + " to surrender.");
        PLAYING.stringsP1.add(stringUndoP1);
        PLAYING.stringsP1.add(stringRedoP1);

        PLAYING.stringsP2.add("Press " + KeyEvent.getKeyText(KeyHandler.Action.P2_SURRENDER.keyCode) + " to surrender.");
        PLAYING.stringsP2.add(stringUndoP2);
        PLAYING.stringsP2.add(stringRedoP2);

        NOT_READY.stringsP1.add("Press " + KeyEvent.getKeyText(KeyHandler.Action.P1_READY.keyCode) + " to begin.");
        NOT_READY.stringsP1.add(stringNewMaze);
        
        NOT_READY.stringsP2.add("Press " + KeyEvent.getKeyText(KeyHandler.Action.P2_READY.keyCode) + " to begin.");

        MAZE_EMPTY.stringsP1.add(stringNewMaze);

        GAME_WON.stringsP1.add(stringNewMaze);
        GAME_LOST.stringsP1.add(stringNewMaze);
        SURRENDERED.stringsP1.add(stringNewMaze);

        for (Status status : new Status[]{INCOMPLETE, UNSOLVABLE}) {
            status.stringsP1.add("Press " + KeyEvent.getKeyText(KeyHandler.Action.P1_MAZE_RESET.keyCode) + " to reset.");
            status.stringsP1.add(stringUndoP1);
            status.stringsP1.add(stringRedoP1);

            status.stringsP2.add("Press " + KeyEvent.getKeyText(KeyHandler.Action.P2_MAZE_RESET.keyCode) + " to reset.");
            status.stringsP2.add(stringUndoP2);
            status.stringsP2.add(stringRedoP2);
        }

        if (Main.mazeLeft != null) {
            // potentially redraw labels
            Main.mazeLeft.setStatus(Main.mazeLeft.status);
            Main.mazeRight.setStatus(Main.mazeRight.status);
        }
    }

    public boolean allowsStep() {
        return this == RESETTING || this == PLAYING || this == INCOMPLETE || this == UNSOLVABLE;
    }

    public boolean allowsHints() {
        return this == PLAYING;
    }

    public boolean allowsReset() {
        return this == PLAYING || this == INCOMPLETE || this == UNSOLVABLE;
    }

    public boolean allowsReady() {
        return this == NOT_READY;
    }

    public boolean allowsSurrender() {
        return this == PLAYING || this == INCOMPLETE || this == UNSOLVABLE;
    }

    public boolean allowsNewMaze() {
        return this == NOT_READY || this == MAZE_EMPTY || this == GAME_WON || this == GAME_LOST || this == SURRENDERED;
    }

    public boolean allowsMove() {
        return this == PLAYING;
    }
}