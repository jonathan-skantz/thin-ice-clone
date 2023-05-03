import java.util.HashSet;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

public class KeyHandler extends KeyAdapter {
    
    private HashSet<Integer> keysPressed = new HashSet<>();     // store key codes

    // define actions and their corresponding key
    public enum ActionKey {

        UP(KeyEvent.VK_W),
        DOWN(KeyEvent.VK_S),
        LEFT(KeyEvent.VK_A),
        RIGHT(KeyEvent.VK_D),

        MAZE_NEW(KeyEvent.VK_SPACE),
        MAZE_RESET(KeyEvent.VK_ESCAPE),
        MAZE_HINT(KeyEvent.VK_H);

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
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keysPressed.contains(keyCode)) {
            // prevents spammed events (when holding down key)
            return;
        }

        ActionKey action = ActionKey.getAction(keyCode);
        if (action != null) {               // prevents listening to unused keys
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
