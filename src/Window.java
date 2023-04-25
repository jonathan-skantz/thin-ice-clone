import java.util.HashSet;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JFrame;

public class Window extends JFrame {
    
    private final Color BG_COLOR = Color.LIGHT_GRAY;

    /*
     * The top, bottom, left, and right border of the window (like the title bar) is
     * for some reason included when setting the size.
     * Therefore the actual "playable" window size will be slightly smaller.
     */
    private final int WIDTH_INCLUDING_BORDER = 500;
    private final int HEIGHT_PLUS_BORDER = 500;

    public static int width;        // actual size, excluding border
    public static int height;

    private static int offsetX;     // size of left border
    private static int offsetY;     // size of title bar

    private HashSet<Sprite> sprites;   // where all sprites will be saved
    // TODO: reconsider saving as ArrayList to preserve order when drawing.

    public Window() {

        setTitle("Thin Ice"); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // set the default close operation
        
        setSize(WIDTH_INCLUDING_BORDER, HEIGHT_PLUS_BORDER);
        setResizable(false); 
        setVisible(true); 
        
        setLocationRelativeTo(null); // center the window on the screen

        // save border offsets
        Insets border = getInsets();
        offsetX = border.left;
        offsetY = border.top;
        width = WIDTH_INCLUDING_BORDER - border.left - border.right;
        height = HEIGHT_PLUS_BORDER - border.top - border.bottom;

        sprites = new HashSet<>();
    }

    public void addSprite(Sprite sprite) {
        // NOTE: duplicates will not be added since `sprites` is a set.
        sprites.add(sprite);
    }

    public void removeSprite(Sprite sprite) {
        sprites.remove(sprite);
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
        g.setColor(BG_COLOR);
        g.fillRect(Window.exactX(0), Window.exactY(0), width, height);

        for (Sprite sprite : sprites) {
            sprite.draw(g);
        }
    }

}
