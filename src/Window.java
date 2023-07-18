/**
 * Class for creating and setting up a JFrame.
 */
import java.awt.Color;

import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Window {
    
    private static final Color BG_COLOR = Color.LIGHT_GRAY;
    private static final String TITLE = "Thin Ice";
    
    public static int width = 500;        // actual canvas size, excluding border
    public static int height = 500;

    public static JPanel sprites = new JPanel();   // a panel containing all sprites

    public static JFrame frame;     // only used publically in UI to add popup dialog

    public static void setup() {
        
        frame = new JFrame(TITLE);
        frame.addKeyListener(new KeyHandler());

        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);        // center the window on screen

        frame.setResizable(false);
        frame.setLayout(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        sprites.setLayout(null);

        frame.setVisible(true);
        Insets ins = frame.getInsets();
        
        width = width - ins.left - ins.right;
        height = height - ins.top - ins.bottom;

        sprites.setBounds(ins.left, ins.top, width, height);
        sprites.setBackground(BG_COLOR);

        sprites.setLocation(0, 0);
        
        frame.add(sprites);   // finally add the panel

    }

}
