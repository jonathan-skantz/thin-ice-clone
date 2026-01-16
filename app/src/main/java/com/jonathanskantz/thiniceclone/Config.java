package com.jonathanskantz.thiniceclone;

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

    public static final int DEFAULT_AMOUNT_GROUND = 6;
    public static final int DEFAULT_AMOUNT_DOUBLES = 0;

    // block graphics
    public static int blockSize = 30;
    
    // separate border to visualize the new blocks during generation
    public static final Border BLOCK_BORDER_GEN = BorderFactory.createLineBorder(new Color(255, 255, 150), 2);

    // hints
    public static final Color HINT_COLOR = new Color(150, 150, 255);
    public static boolean hintTypeLongest = true;   // either longest or shortest

    // flags for updates
    public static boolean newHintMax = false;

    public enum Host {
        MIRROR_OPPONENT(false),
        SHOW_ANIMATIONS(true),
        SHOW_UNSOLVABLE(true),
        ALLOW_RESETTING(true),
        ALLOW_UNDO_AND_REDO(true),
        HINT_LENGTH(0.05f);

        private final boolean IS_TOGGLABLE;
        public boolean enabled;
        public float number;

        private Host(boolean enabled) {
            this.enabled = enabled;
            IS_TOGGLABLE = true;
        }

        private Host(float number) {
            this.number = number;
            IS_TOGGLABLE = false;
        }

        // only used to sending settings through socket
        public static HashMap<Host, Object> getSettings() {
            HashMap<Host, Object> map = new HashMap<>();

            for (Host setting : values()) {
                if (setting.IS_TOGGLABLE) {
                    map.put(setting, setting.enabled);
                }
                else {
                    map.put(setting, setting.number);
                }
            }

            return map;
        }

    }

    public static void applyDefault() {
        // setup maze config
        MazeGen.setSize(MAZE_DEFAULT_WIDTH, MAZE_DEFAULT_HEIGHT);
        MazeGen.Amount.GROUND.set(DEFAULT_AMOUNT_GROUND);
        MazeGen.Amount.DOUBLES.set(DEFAULT_AMOUNT_DOUBLES);
    }

}
