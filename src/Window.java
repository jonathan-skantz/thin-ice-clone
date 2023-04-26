import java.util.ArrayList;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

public class Window extends JFrame {
    
    private final Color BG_COLOR = Color.LIGHT_GRAY;

    /*
     * The top, bottom, left, and right border of the window (like the title bar) is
     * included when setting the size because `Window` extends `JFrame` and not `JWindow`.
     * Therefore the actual "playable" window size will be slightly smaller.
     */

    private final int WIDTH_INCLUDING_BORDER = 500;
    private final int HEIGHT_INCLUDING_BORDER = 500;

    public static int width;        // actual size, excluding border
    public static int height;

    private static int offsetX;     // size of left border
    private static int offsetY;     // size of title bar

    private ArrayList<Sprite> sprites = new ArrayList<>();      // where all sprites will be saved

    private BufferedImage bufferedCanvas;       // actual canvas, excluding borders.
    private Graphics2D bufferedCanvasG;         // the drawable graphics of `bufferedCanvas`

    public Window() {

        setTitle("Thin Ice"); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // set the default close operation
        
        setSize(WIDTH_INCLUDING_BORDER, HEIGHT_INCLUDING_BORDER);
        setResizable(false); 

        setLocationRelativeTo(null); // center the window on the screen

        /*
         * NOTE: setVisible(true) internally calls repaint(), which uses
         * `Window.bufferedCanvasG`, but that has not yet been created.
         * Hence, `setIgnoreRepaint` is set to true here, and false
         * after getting the border sizes.
         */
        setIgnoreRepaint(true);
        setVisible(true);

        // save border offsets
        Insets border = getInsets();
        setIgnoreRepaint(false);
        offsetX = border.left;
        offsetY = border.top;
        width = WIDTH_INCLUDING_BORDER - border.left - border.right;
        height = HEIGHT_INCLUDING_BORDER - border.top - border.bottom;
        
        // NOTE: Only RGB (not ARGB), since this is the bottom layer that all other sprites are drawn on.
        bufferedCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        bufferedCanvasG = bufferedCanvas.createGraphics();
        bufferedCanvasG.setBackground(BG_COLOR);
    }

    public void addSprite(Sprite sprite) {
        // NOTE: duplicates will not be added since `sprites` is a set.
        sprites.add(sprite);
        repaint();
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
        
        // clear canvas
        bufferedCanvasG.clearRect(0, 0, width, height);

        // loop through backwards to preserve drawing order
        // (first elem should be drawn last)
        for (int i=sprites.size()-1; i>=0; i--) {
            sprites.get(i).draw(bufferedCanvasG);
        }

        // NOTE: This must be drawn using exactX and Y, because this is drawn
        // on the window, which includes invisible space underneath the borders.
        g.drawImage(bufferedCanvas, Window.exactX(0), Window.exactY(0), null);
    }

}
