import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Class for storing info about a sprite.
 * 
 * NOTE: this class does not create a new graphical component but rather
 * displays its content on the window, which is done in Sprite.draw().
 */
public class Sprite {
    
    private BufferedImage image;
    private int velocity;
    
    public int x;
    public int y;
    
    private int width;
    private int height;

    public Sprite(String filename, int velocity) {
        this.velocity = velocity;
        
        try {
            image = ImageIO.read(new File("src/images/" + filename));
            width = image.getWidth();
            height = image.getHeight();

        } catch (IOException e) {
            System.out.println("Error loading image: " + e.getMessage());
        }

        center();
    }
    
    /**
     * Moves the sprite to the center of the window.
     */
    public void center() {
        x = (Window.width - width) / 2;
        y = (Window.height - height) / 2;
    }

    public void draw(Graphics g) {
        // draw temporal bg, only for the purpose of visualing the hitbox
        g.setColor(Color.GREEN);
        g.fillRect(Window.exactX(x), Window.exactY(y), width, height);
        
        g.drawImage(image, Window.exactX(x), Window.exactY(y), null);
    }

    public void moveTo(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Moves the sprite according to its velocity and current direction.
     * 
     * @param key "up", "down", "left", or "right" currently implemented.
     */
    public void move(String key) {

        // TODO: collision detection

        switch (key) {

            case "up":
                y -= velocity;
                break;

            case "down":
                y += velocity;
                break;

            case "left":
                x -= velocity;
                break;

            case "right":
                x += velocity;
                break;
        }
        
    }

}
