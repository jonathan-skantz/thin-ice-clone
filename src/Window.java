import java.util.ArrayList;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Insets;

import javax.swing.JFrame;

public class Window extends JFrame {
    
    private final Color BG_COLOR = Color.LIGHT_GRAY;
    private final String TITLE = "Thin Ice";
    
    public static int width = 500;        // actual canvas size, excluding border
    public static int height = 500;

    private static int offsetX;     // size of left border
    private static int offsetY;     // size of title bar
    
    private boolean allowRepaint = true;

    private ArrayList<Sprite> sprites = new ArrayList<>();  // where all sprites will be saved

    private BufferedImage canvas;       // actual canvas, excluding borders.
    private Graphics2D canvasGraphics;  // the drawable graphics of `canvas`

    public Window() {

        //  allow sprites to reference the window
        Sprite.window = this;

        setAllowRepaint(false);
        
        setTitle(TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // set the default close operation
        setResizable(false);
        setLocationRelativeTo(null); // center the window on the screen
        
        // NOTE: Only RGB (not ARGB), since this is the bottom layer that all other sprites are drawn on.
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        canvasGraphics = canvas.createGraphics();
        canvasGraphics.setBackground(BG_COLOR);
        
        setSize(width, height);
        
        // save border offsets (top, bottom, left, right border of window, like the title bar)
        // and set new size that excludes the border
        setVisible(true);
        Insets border = getInsets();
        offsetX = border.left;
        offsetY = border.top;
        
        int widthIncludingBorder = getWidth() + border.left + border.right;
        int heightIncludingBorder = getHeight() + border.top + border.bottom;
        
        setSize(widthIncludingBorder, heightIncludingBorder);
        
        setAllowRepaint(true);
    }

    public void setAllowRepaint(boolean x) {
        allowRepaint = x;
        repaint();
    }

    public void addSprite(Sprite sprite) {

        if (!sprites.contains(sprite)) {
            sprites.add(sprite);
            repaint();
        }
    }
    
    public void removeSprite(Sprite sprite) {
        sprites.remove(sprite);
        repaint();
    }

    /**
     * Converts the x-coordinate to exclude the borders of the window.
     * Use this function when drawing. All other logic is based on
     * the expected coordinates (not converted).
     * 
     * @param x Expected x-coordinate.
     * @return x-coordinate offset by the size of the left window border.
     */
    public static int exactX(int x) {
        return x + offsetX;
    }

    /**
     * Converts the y-coordinate to exclude the borders of the window.
     * Use this function when drawing. All other logic is based on
     * the expected coordinates (not converted).
     * 
     * @param x Expected y-coordinate.
     * @return y-coordinate offset by the size of the top window border.
     */
    public static int exactY(int y) {
        return y + offsetY;
    }

    /**
     * Paints the background and renders all sprites.
     */
    @Override
    public void paint(Graphics g) {
        
        if (!allowRepaint) {
            return;
        }
        
        // clear canvas
        canvasGraphics.clearRect(0, 0, width, height);

        // loop through backwards to preserve drawing order
        // (first elem should be drawn last)
        for (int i=sprites.size()-1; i>=0; i--) {
            sprites.get(i).draw(canvasGraphics);
        }

        // NOTE: This must be drawn using exactX and Y, because this is drawn
        // on the window, which includes invisible space underneath the borders.
        g.drawImage(canvas, Window.exactX(0), Window.exactY(0), null);
    }

}
