import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
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
    
    private int velocity;

    public static Window window;
    public BufferedImage canvas;
    private Graphics2D canvasGraphics;
    
    // create a duplicate canvas, in order to revert adding a border
    private BufferedImage canvasOriginal;

    private final int DEFAULT_WIDTH = 100;
    private final int DEFAULT_HEIGHT = 100;
    private Color backgroundColor = new Color(255, 0, 0, 100);

    public Rectangle rect;
    private boolean visible = true;

    // text config
    private String textString;
    private Font textFont;
    private Color textColor;

    // border config
    private boolean borderVisible = false;
    private Color borderColor = Color.BLACK;
    private int borderWidth = 4;
    private BasicStroke borderStroke;
    

    /**
     * Should be called internally after
     * loading a sprite for the first time.
     * 
     * @param vel Velocity.
     */
    private void setupSprite(int vel) {
        velocity = vel;
        window.addSprite(this);
        moveToCenter();
    }
    
    /**
     * Should be called internally after
     * loading a sprite for the first time.
     * 
     * @param vel Velocity.
     */
    private void setupTextSprite(int vel) {
        velocity = vel;
        window.addSpriteToFront(this);
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
            canvasStartDrawing(DEFAULT_WIDTH, DEFAULT_HEIGHT);

            // draw bg
            canvasGraphics.setColor(backgroundColor);
            canvasGraphics.fillRect(0, 0, rect.width, rect.height);
            
            // draw border
            setBorder(6, Color.BLACK, true);
            
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
    public Sprite(int w, int h, Color backgroundColor, int vel) {

        canvasStartDrawing(w, h);

        canvasGraphics = canvas.createGraphics();
        
        this.backgroundColor = backgroundColor;
        canvasGraphics.setColor(backgroundColor);
        canvasGraphics.fillRect(0, 0, w, h);
        
        canvasStopDrawing();

        setupSprite(vel);
    }

    /**
     * Create a text sprite.
     * 
     * @param text Text that should be rendered.
     * @param font Font object.
     * @param color Text color.
     */
    public Sprite(String text, Font font, Color color) {

        // NOTE: uses `window` instead of `canvasGraphics` since
        // the latter is not initialized yet.

        FontMetrics fm = window.getFontMetrics(font);
        canvasStartDrawing(fm.stringWidth(text), fm.getHeight());

        textString = text;
        textFont = font;
        textColor = color;
        applyText();

        canvasStopDrawing();
        setupTextSprite(0);
    }

    /**
     * Free up system resources by disposing this sprite.
     */
    public void dispose() {
        canvasGraphics.dispose();
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;

        canvasStartDrawing(rect.width, rect.height);
        canvasGraphics.setColor(color);
        canvasGraphics.fillRect(0, 0, rect.width, rect.height);
        
        canvasStopDrawing();

        window.repaint();
    }

    public void setText(String text) {
        textString = text;
        applyText();
    }

    /**
     * Draw string on canvas.
     */
    private void applyText() {
        if (textString != null) {
            canvasGraphics.setFont(textFont);
            canvasGraphics.setColor(textColor);

            // NOTE: y-coordinate starts at bottom of text
            canvasGraphics.drawString(textString, 0, textFont.getSize());

            window.repaint();
        }
    }

    public void setBorder(int width, Color color, boolean visible) {

        borderWidth = width;
        borderColor = color;
        borderVisible = visible;
        borderStroke = new BasicStroke(borderWidth);
        
        applyBorder();
    }

    // also repaints window
    private void applyBorder() {
        if (borderVisible) {
            canvasGraphics.setStroke(borderStroke);
            canvasGraphics.setColor(borderColor);
            canvasGraphics.drawRect(0, 0, rect.width, rect.height);

        }
        window.repaint();
    }

    public void setSize(int w, int h) {

        BufferedImage oldCanvas = canvas;

        canvasStartDrawing(w, h);
        canvasGraphics.drawImage(oldCanvas, 0, 0, w, h, null);
        canvasStopDrawing();
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
     * @param newW (Potentially) new width.
     * @param newH (Potentially) new height.
     */
    private void canvasStartDrawing(int newW, int newH) {
        
        int oldCenterX;
        int oldCenterY;
        
        if (canvasGraphics == null) {
            oldCenterX = Window.width / 2;
            oldCenterY = Window.height / 2;
        }
        else {
            // the graphics may not have been created yet
            // because this may be the first call to this method
            // (which means the sprite has just been instantiated)

            canvasGraphics.dispose();       // NOTE: important to dispose the old graphics
            
            oldCenterX = rect.x + rect.width / 2;
            oldCenterY = rect.y + rect.height / 2;
        }

        // preserve center coordinates
        int newX = oldCenterX - newW / 2;
        int newY = oldCenterY - newH / 2;
        rect = new Rectangle(newX, newY, newW, newH);

        // set up empty canvas
        canvas = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
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
    
        applyBorder();
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
        canvasStartDrawing(newCanvas.getWidth(), newCanvas.getHeight());
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
     * --- CURRENTLY UNUSED ---
     * Determines if this and other sprite collides.
     * Collides in this case means that the sprites'
     * rects are within each other, not only touching.
     * 
     * @param sprite The other sprite.
     * @return true if colliding, false if not.
     */
    // public boolean collidesWith(Sprite sprite) {
    //     return sprite.rect.intersects(rect);
    // }

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
    public void move(KeyHandler.ActionKey actionKey) {

        switch (actionKey) {

            case MOVE_UP:
                rect.y -= velocity;
                break;

            case MOVE_DOWN:
                rect.y += velocity;
                break;

            case MOVE_LEFT:
                rect.x -= velocity;
                break;

            case MOVE_RIGHT:
                rect.x += velocity;
                break;
            
            default:
                break;
        }

        window.repaint();
    }

}
