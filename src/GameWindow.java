import javax.swing.JFrame;

public class GameWindow extends JFrame {

    public GameWindow() {
        super("Thin Ice"); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // set the default close operation
        setSize(960, 720); 
        setLocationRelativeTo(null); // center the window on the screen
        setResizable(false); 
        setVisible(true); 
    }

    public static void main(String[] args) {
        new GameWindow();
    }
}