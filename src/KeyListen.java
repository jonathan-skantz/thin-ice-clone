import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;

import javax.swing.JFrame;

public class KeyListen extends KeyAdapter {
    
    private Set<Integer> keysPressed = new HashSet<>();     // stores key codes
    
    private Sprite player;
    private JFrame window;

    private HashMap<Integer, String> keyStatus = new HashMap<>();

    public KeyListen(JFrame window, Sprite player) {
        this.window = window;
        this.player = player;

        // add keys here, if needed
        keyStatus.put(KeyEvent.VK_W, "up");
        keyStatus.put(KeyEvent.VK_S, "down");
        keyStatus.put(KeyEvent.VK_A, "left");
        keyStatus.put(KeyEvent.VK_D, "right");
    }


    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keysPressed.contains(keyCode)) {
            // prevents spammed events (when holding down key)
            return;
        }

        String keyStr = keyStatus.get(keyCode);
        if (keyStr != null) {              // prevents listening to unused keys
            keysPressed.add(keyCode);
            handleKeyPress(keyStr);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        String keyStr = keyStatus.get(keyCode);

        if (keyStr != null) {
            keysPressed.remove(keyCode);
        }
    }

    private void handleKeyPress(String key) {
        player.move(key);
        window.repaint();       // NOTE: queues a redraw immediately on keypress
    }
    
}
