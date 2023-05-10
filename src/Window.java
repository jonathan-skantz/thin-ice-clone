import java.awt.Color;

import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Class for creating and setting up a JFrame.
 */
public class Window extends JFrame {
    
    private final Color BG_COLOR = Color.LIGHT_GRAY;
    private final String TITLE = "Thin Ice";
    
    public static int width = 500;        // actual canvas size, excluding border
    public static int height = 500;

    public JPanel sprites = new JPanel();   // a panel containing all sprites

    public Window() {
        
        Sprite.window = this;   // allow sprites to reference the window

        setTitle(TITLE);
        setSize(width, height);
        setLocationRelativeTo(null);        // center the window on screen

        setResizable(false);
        setLayout(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        sprites.setLayout(null);

        setVisible(true);
        Insets ins = getInsets();
        
        width = width - ins.left - ins.right;
        height = height - ins.top - ins.bottom;

        sprites.setBounds(ins.left, ins.top, width, height);
        sprites.setBackground(BG_COLOR);

        sprites.setLocation(0, 0);
        
        add(sprites);   // finally add the panel

    }

}
