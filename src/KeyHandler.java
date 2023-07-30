import java.util.HashSet;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

public class KeyHandler extends KeyAdapter {
    
    private HashSet<Integer> keysPressed = new HashSet<>();     // store key codes

    public static boolean allowContinuous = false;      // register held down key as multiple presses

    // define actions and their corresponding key
    public enum Action {

        P1_MOVE_UP(KeyEvent.VK_W),
        P1_MOVE_DOWN(KeyEvent.VK_S),
        P1_MOVE_LEFT(KeyEvent.VK_A),
        P1_MOVE_RIGHT(KeyEvent.VK_D),
        P1_MAZE_RESET(KeyEvent.VK_ESCAPE),
        P1_MAZE_HINT(KeyEvent.VK_H),
        P1_MAZE_STEP_UNDO(KeyEvent.VK_Q),
        P1_MAZE_STEP_REDO(KeyEvent.VK_E),
        
        P2_MOVE_UP(KeyEvent.VK_UP),
        P2_MOVE_DOWN(KeyEvent.VK_DOWN),
        P2_MOVE_LEFT(KeyEvent.VK_LEFT),
        P2_MOVE_RIGHT(KeyEvent.VK_RIGHT),
        P2_MAZE_RESET(KeyEvent.VK_NUMPAD0),
        P2_MAZE_HINT(KeyEvent.VK_NUMPAD1),
        P2_MAZE_STEP_UNDO(KeyEvent.VK_NUMPAD4),
        P2_MAZE_STEP_REDO(KeyEvent.VK_NUMPAD6),

        // not user-specific
        MAZE_NEW(KeyEvent.VK_SPACE),
        ZOOM_IN(KeyEvent.VK_PLUS),
        ZOOM_OUT(KeyEvent.VK_MINUS),
        START(KeyEvent.VK_ENTER);

        private Action(int keyCode) {
            this.keyCode = keyCode;
        }

        public int keyCode;
        private Runnable defaultCallback = () -> { System.out.println("not implemented: action \"" + this + "\""); };
        private Runnable callback = defaultCallback;
        
        /**
         * Gets an Action based on the key code.
         * 
         * @param keyCode Corresponding to an Action.
         * @return The Action which is bound to the key code, or null if none is bound.
         */
        public static Action getAction(int keyCode) {
            for (Action key : values()) {
                if (key.keyCode == keyCode) {
                    return key;
                }
            }
            return null;
        }
        
        public void setCallback(Runnable callback) {
            this.callback = callback;
        }
        
        public void removeCallback() {
            this.callback = defaultCallback;
        }

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (!allowContinuous && keysPressed.contains(keyCode)) {
            return;
        }

        Action action = Action.getAction(keyCode);

        // if the key has an action, or if (ctrl and +) or (ctrl and -) is pressed
        if (action != null ||
            (e.isControlDown() && (action == Action.ZOOM_IN || action == Action.ZOOM_OUT))) {
            keysPressed.add(keyCode);
            action.callback.run();          // executes corresponding action
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        Action action = Action.getAction(keyCode);

        if (action != null) {
            keysPressed.remove(keyCode);
        }
    }

}
