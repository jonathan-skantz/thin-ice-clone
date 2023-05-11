/**
 * Class for creating rectangle sprites, which consist of
 * a background color and/or image.
 */
import java.io.File;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Sprite extends JComponent {
    
    public int velocity;

    public static Window window;

    private final int DEFAULT_WIDTH = 100;
    private final int DEFAULT_HEIGHT = 100;
    private BufferedImage bgImage;

    /**
     * Create an image sprite.
     * 
     * @param filename Only the filename (located in "src/images/").
     * @param vel Velocity.
     */
    public Sprite(String filename, int vel) {
        
        String relPath = "src/images/" + filename;
        
        try {
            bgImage = ImageIO.read(new File(relPath));
            setBounds(0, 0, bgImage.getWidth(), bgImage.getHeight());
            
        } catch (IOException e) {
            System.out.println("IOException when loading \"" + relPath + "\": " + e.getMessage());
            
            // create default image (red square with border)
            setBounds(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            setBackground(Color.RED);
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }
        
        velocity = vel;
        window.sprites.add(this);
    }

    /**
     * Create a rectangle sprite.
     * 
     * @param w Width
     * @param h Height
     * @param color Color
     * @param vel Velocity
     */
    public Sprite(int w, int h, Color backgroundColor, int vel) {

        setBounds(0, 0, w, h);
        setBackground(backgroundColor);

        velocity = vel;
        window.sprites.add(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        
        super.paintComponent(g);        // TODO: this is probably not necessary
        
        g.setColor(getBackground());
        
        // NOTE: fills at (0,0) since `g` is the graphics that refers to this sprite's canvas
        g.fillRect(0, 0, getWidth(), getHeight());

        // NOTE: bgImage may be null
        g.drawImage(bgImage, 0, 0, null);
    }

    /**
     * Move the sprite to the center of the window.
     */
    public void moveToCenter() {
        int x = (Window.width - getWidth()) / 2;
        int y = (Window.height - getHeight()) / 2;
        setLocation(x, y);
    }

    /**
     * Move the sprite according to its velocity and current direction.
     */
    public void move(KeyHandler.ActionKey actionKey) {

        int x = getX();
        int y = getY();

        switch (actionKey) {

            case MOVE_UP:
                y -= velocity;
                break;

            case MOVE_DOWN:
                y += velocity;
                break;

            case MOVE_LEFT:
                x -= velocity;
                break;

            case MOVE_RIGHT:
                x += velocity;
                break;
            
            default:
                break;
        }

        setLocation(x, y);
    }

}
