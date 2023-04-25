import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
    
    public static Window window;

    private BufferedImage image;
    private Rectangle rect;
    private boolean visible;
    
    private int velocity;

    public Sprite(String filename, int velocity) {
        this.velocity = velocity;
        visible = true;
        
        int width;
        int height;
        
        String relPath = "src/images/" + filename;
        
        try {
            
            /*
             * BUG:
             * 
             * This causes a NullPointerException because
             * this.rect is apparently not defined:
             * 
             *      File f = new File(relPath);
             *      window.addSprite(this);
             *      image = ImageIO.read(f);
             * 
             * But this works:
             * 
             *      File f = new File(relPath);
             *      image = ImageIO.read(f);
             *      window.addSprite(this);
             * 
             * WORKAROUND:
             * window.addSprite(this) is called after the try-catch.
             */

            image = ImageIO.read(new File(relPath));

            width = image.getWidth();
            height = image.getHeight();
            
        } catch (IOException e) {
            System.out.println("Error loading image \"" + relPath + "\": " + e.getMessage());
            
            // create default image
            width = 100;
            height = 100;
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
         
            g2d.setColor(Color.RED);
            g2d.fillRect(0, 0, width, height);
            g2d.dispose();
        }
        
        window.addSprite(this);
        
        rect = new Rectangle(0, 0, width, height);
        
        moveToCenter();

    }
    
    public void setVisible(boolean x) {
        visible = x;
        window.repaint();
    }

    /**
     * Moves the sprite to the center of the window.
     */
    public void moveToCenter() {
        rect.x = (Window.width - rect.width) / 2;
        rect.y = (Window.height - rect.height) / 2;
    }

    public void draw(Graphics g) {
        
        if (!visible) {
            return;
        }

        // draw temporary bg, only for the purpose of visualing the hitbox
        g.setColor(Color.GREEN);
        g.fillRect(Window.exactX(rect.x), Window.exactY(rect.y), rect.width, rect.height);
        
        g.drawImage(image, Window.exactX(rect.x), Window.exactY(rect.y), null);
    }

    /**
     * Determines if this and other sprite collides.
     * Collides in this case means that the sprites'
     * rects are within each other, not only touching.
     * 
     * @param sprite The other sprite.
     * @return true if colliding, false if not.
     */
    public boolean collidesWith(Sprite sprite) {
        return sprite.rect.intersects(rect);
    }

    public void moveTo(int x, int y) {
        rect.x = x;
        rect.y = y;
        window.repaint();
    }

    /**
     * Moves the sprite according to its velocity and current direction.
     * 
     * @param key "up", "down", "left", or "right" currently implemented.
     */
    public void move(String action) {

        switch (action) {

            case "up":
                rect.y -= velocity;
                break;

            case "down":
                rect.y += velocity;
                break;

            case "left":
                rect.x -= velocity;
                break;

            case "right":
                rect.x += velocity;
                break;
        }

        System.out.println("moved to (" + rect.x + ", " + rect.y + ")");

        window.repaint();
        
    }

}
