import java.util.HashMap;
import java.util.HashSet;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

public class KeyListen extends KeyAdapter {
    
    private HashSet<Integer> keysPressed = new HashSet<>();     // stores key codes
    
    private HashMap<Integer, String> keyMap;        // keycode and corresponding action (like {87: "up"})
    private HashMap<String, Runnable> callbacks;    // action and corresponding callback

    public KeyListen() {
        
        keyMap = new HashMap<>();
        callbacks = new HashMap<>();

        /*
         * This defines keys and their corresponding action.
         * If a callback is never set in .addCallback(),
         * the key will still be listened to but no callback is executed.
         */
        
        keyMap.put(KeyEvent.VK_W, "up");
        keyMap.put(KeyEvent.VK_S, "down");
        keyMap.put(KeyEvent.VK_A, "left");
        keyMap.put(KeyEvent.VK_D, "right");

        keyMap.put(KeyEvent.VK_SPACE, "test");

        // set default callback for development purpose
        for (String action : keyMap.values()) {
            addCallback(action, () -> {System.out.println("not implemented: execute \"" + action + "\"");});
        }

    }

    public void addCallback(String action, Runnable callback) {
        // TODO: consider allowing multiple callbacks

        for (String possibleAction : keyMap.values()) {
            if (possibleAction == action) {
                callbacks.put(action, callback);
                return;
            }
        }

        throw new Error("Action \"" + action + "\" is not defined.");
    }

    public void removeCallback(String action) {
        callbacks.put(action, () -> {});
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keysPressed.contains(keyCode)) {
            // prevents spammed events (when holding down key)
            return;
        }

        String action = keyMap.get(keyCode);
        if (action != null) {               // prevents listening to unused keys
            keysPressed.add(keyCode);

            callbacks.get(action).run();    // executes corresponding action
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        String action = keyMap.get(keyCode);

        if (action != null) {
            keysPressed.remove(keyCode);
        }
    }

}
