import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.util.Set;
import java.util.HashSet;

public class GameKeyListener extends KeyAdapter {
    
    private Set<Integer> keysPressed = new HashSet<>();
    
    
    // controls (NOTE: no other keys will be listened to)
    private enum Key {
        UP(KeyEvent.VK_W),
        DOWN(KeyEvent.VK_S),
        LEFT(KeyEvent.VK_A),
        RIGHT(KeyEvent.VK_D);
        
        private final int keyCode;
        
        private Key(int keyCode) {
            this.keyCode = keyCode;
        }
    }
    
    // checks if a key exists with that keyCode (i.e. the key should be listened to)
    public Key getMappedKey(int keyCode) {
        for (Key key : Key.values()) {
            if (key.keyCode == keyCode) {
                return key;
            }
        }
        return null;
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keysPressed.contains(keyCode)) {
            // prevents spammed events (when holding down key)
            return;
        }

        Key key = getMappedKey(keyCode);
        if (key != null) {              // prevents listening to unused keys
            keysPressed.add(keyCode);
            handleKeyPress(key);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        Key key = getMappedKey(keyCode);

        if (key != null) {
            keysPressed.remove(keyCode);
        }
    }



    private void handleKeyPress(Key key) {
        
        // TODO: move player, check collisions
        System.out.println("(move " + key.name() + " once)");

        switch (key) {
            case UP:
                break;
            case DOWN:
                break;
            case LEFT:
                break;
            case RIGHT:    
                break;
        }
    }
    
}