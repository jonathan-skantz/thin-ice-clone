import java.util.HashSet;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

public class KeyHandler extends KeyAdapter {
    
    private HashSet<Integer> keysPressed = new HashSet<>();     // store key codes

    // define actions and their corresponding key
    public enum ActionKey {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        
        MAZE_NEW,
        MAZE_RESET;
        
        static {
            UP.keyCode = KeyEvent.VK_W;
            DOWN.keyCode = KeyEvent.VK_S;
            LEFT.keyCode = KeyEvent.VK_A;
            RIGHT.keyCode = KeyEvent.VK_D;
            
            MAZE_NEW.keyCode = KeyEvent.VK_SPACE;
            MAZE_RESET.keyCode = KeyEvent.VK_ESCAPE;
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
