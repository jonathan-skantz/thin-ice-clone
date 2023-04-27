import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Class for storing info about a sprite.
 * 
 * NOTE: this class does not create a new graphical component but rather
 * displays its content on the window, which is done in Sprite.draw().
 * 
 * TODO: potentially implement, padding, margins etc.
 */
public class Sprite {
    
    public static Window window;
    public BufferedImage canvas;
    private Graphics2D canvasGraphics;
    
    // create a duplicate canvas, in order to revert adding a border
    private BufferedImage canvasOriginal;
    
    private final int DEFAULT_WIDTH = 100;
    private final int DEFAULT_HEIGHT = 100;
    
    private Rectangle rect;
    private boolean visible = true;
    
    private int velocity;

    /**
     * Should be called internally after loading a canvas first time.
     * 
     * @param vel Velocity.
     */
    private void setupSprite(int vel) {
        velocity = vel;
        window.addSprite(this);
        moveToCenter();
    }

    /**
     * Create an image sprite.
     * 
     * @param filename Only the filename (located in "src/images/").
     * @param vel Velocity.
     */
    public Sprite(String filename, int vel) {
        
        String relPath = "src/images/" + filename;
        BufferedImage img;
        
        try {
            img = ImageIO.read(new File(relPath));
            setCanvas(img);
            
        } catch (IOException e) {
            System.out.println("IOException when loading \"" + relPath + "\": " + e.getMessage());
            
            // create default image (red square with border and cross)
            img = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            canvasStartDrawing(img);

            // draw bg
            canvasGraphics.setColor(new Color(255, 0, 0, 100));
            canvasGraphics.fillRect(0, 0, rect.width, rect.height);
            
            // draw border
            addBorder(6, Color.BLACK);
            
            // draw cross
            canvasGraphics.drawLine(0, 0, rect.width, rect.height);
            canvasGraphics.drawLine(0, rect.height, rect.width, 0);
            
            canvasStopDrawing();
        }
        
        setupSprite(vel);
    }

    /**
     * Create a rectangle sprite.
     * 
     * @param w Width
     * @param h Height
     * @param color Color
     * @param vel Velocity
     */
    public Sprite(int w, int h, Color color, int vel) {

        BufferedImage c = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        canvasStartDrawing(c);

        canvasGraphics = canvas.createGraphics();
        
        canvasGraphics.setColor(color);
        canvasGraphics.fillRect(0, 0, w, h);
        
        canvasStopDrawing();

        setupSprite(vel);
    }


    public void addBorder(int width, Color color) {

        canvasGraphics.setStroke(new BasicStroke(width));
        canvasGraphics.setColor(color);
        canvasGraphics.drawRect(0, 0, rect.width, rect.height);

        window.repaint();
    }

    /**
     * Resets the size of the sprite and its
     * canvas to its original appearance.
     */
    public void resetCanvas() {
        
        // set `canvas` to be copy of `canvasOriginal` (not just a reference)
        canvas = new BufferedImage(canvasOriginal.getWidth(), canvasOriginal.getHeight(), canvasOriginal.getType());
        canvasGraphics = canvas.createGraphics();
        
        // draw original canvas on copied (currently empty) canvas
        canvasGraphics.drawImage(canvasOriginal, 0, 0, null);

        window.repaint();
    }

    /**
     * Disposes the old graphics, creates a new empty canvas and a rect.
     * 
     * @param newCanvas A new canvas of any size (used to create the empty canvas).
     */
    private void canvasStartDrawing(BufferedImage newCanvas) {
        

        if (canvasGraphics != null) {
            canvasGraphics.dispose();       // NOTE: important to dispose the old graphics

            // the graphics may not have been created yet
            // because this may be the first call to this method
            // (which means the sprite has just been instantiated)
        }

        rect = new Rectangle(0, 0, newCanvas.getWidth(), newCanvas.getHeight());

        // set up empty canvas
        canvas = new BufferedImage(rect.width, rect.height, newCanvas.getType());
        canvasGraphics = canvas.createGraphics();
    }

    /**
     * Saves a copy of `canvas` and redraws onto `window`.
     */
    private void canvasStopDrawing() {

        // save a copy
        canvasOriginal = new BufferedImage(rect.width, rect.height, canvas.getType());
        Graphics2D g = canvasOriginal.createGraphics();
        g.drawImage(canvas, 0, 0, null);
        g.dispose();
    
        window.repaint();
    }

    /**
     * Replaces the canvas with a new one by drawing
     * `newCanvas` onto a new canvas, to prevent 
     * modifications to `newCanvas` outside of `Sprite`
     * to affect this sprite's canvas.
     * 
     * @param newCanvas A new canvas of any size.
     */
    public void setCanvas(BufferedImage newCanvas) {
        canvasStartDrawing(newCanvas);
        canvasGraphics.drawImage(newCanvas, 0, 0, null);        
        canvasStopDrawing();
    }

    public void setVisible(boolean x) {
        visible = x;
        window.repaint();
    }

    /**
     * This is called only in `Window.paint()` or `Window.repaint()`.
     */
    public void draw(Graphics2D g) {
        
        if (!visible) {
            return;
        }

        // NOTE: Since this image is drawn on `g`, which is the `bufferedCanvasG` from `Window`,
        // `Window.exactX(x)` and `Window.exactY(y)` is not needed.
        g.drawImage(canvas, rect.x, rect.y, null);
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

    /**
     * Moves the sprite to the center of the window.
     */
    public void moveToCenter() {
        rect.x = (Window.width - rect.width) / 2;
        rect.y = (Window.height - rect.height) / 2;
        window.repaint();
    }

    /**
     * Moves the sprite to fixed coordinates.
     * 
     * @param x x-coordinate.
     * @param y y-coordinate (NOTE: starts from top of the window).
     */
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
