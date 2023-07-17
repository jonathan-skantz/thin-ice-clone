import java.util.HashSet;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

public class KeyHandler extends KeyAdapter {
    
    private HashSet<Integer> keysPressed = new HashSet<>();     // store key codes

    public static boolean allowContinuous = false;      // register held down key as multiple presses

    // define actions and their corresponding key
    public enum ActionKey {

        MOVE_UP(KeyEvent.VK_W),
        MOVE_DOWN(KeyEvent.VK_S),
        MOVE_LEFT(KeyEvent.VK_A),
        MOVE_RIGHT(KeyEvent.VK_D),

        MAZE_NEW(KeyEvent.VK_SPACE),
        MAZE_RESET(KeyEvent.VK_ESCAPE),
        MAZE_HINT(KeyEvent.VK_H),

        MAZE_STEP_UNDO(KeyEvent.VK_LEFT),
        MAZE_STEP_REDO(KeyEvent.VK_RIGHT),
        
        ZOOM_IN(KeyEvent.VK_PLUS),
        ZOOM_OUT(KeyEvent.VK_MINUS);

        private ActionKey(int keyCode) {
            this.keyCode = keyCode;
        }

        public int keyCode;
        private Runnable defaultCallback = () -> { System.out.println("not implemented: action \"" + this + "\""); };
        private Runnable callback = defaultCallback;
        
        /**
         * Gets an ActionKey based on the key code.
         * 
         * @param keyCode Corresponding to an ActionKey.
         * @return The ActionKey which is bound to the key code, or null if none is bound.
         */
        public static ActionKey getAction(int keyCode) {
            for (ActionKey key : ActionKey.values()) {
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

        // returns ActionKey based on the difference between two nodes
        public static ActionKey getActionFromMovement(Node lastNode, Node newNode) {
            if (newNode.X - lastNode.X < 0) return ActionKey.MOVE_LEFT;
            else if (newNode.X - lastNode.X > 0) return ActionKey.MOVE_RIGHT;
            else if (newNode.Y - lastNode.Y < 0) return ActionKey.MOVE_UP;
            return ActionKey.MOVE_DOWN;

        }

        // returns list of dx, dy
        public Maze.Direction toDirection() {
            if (this == MOVE_UP) return Maze.Direction.UP;
            else if (this == MOVE_DOWN) return Maze.Direction.DOWN;
            else if (this == MOVE_LEFT) return Maze.Direction.LEFT;
            else if (this == MOVE_RIGHT) return Maze.Direction.RIGHT;
            return null;
        }
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (!allowContinuous && keysPressed.contains(keyCode)) {
            return;
        }

        ActionKey action = ActionKey.getAction(keyCode);

        // if the key has an action, or if (ctrl and +) or (ctrl and -) is pressed
        if (action != null ||
            (e.isControlDown() && (action == ActionKey.ZOOM_IN || action == ActionKey.ZOOM_OUT))) {
            keysPressed.add(keyCode);
            action.callback.run();          // executes corresponding action
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        ActionKey action = ActionKey.getAction(keyCode);

        if (action != null) {
            keysPressed.remove(keyCode);
        }
    }

}
