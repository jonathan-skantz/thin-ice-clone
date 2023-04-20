import javax.swing.JFrame;

public class GameWindow extends JFrame {

    public GameWindow() {
        setTitle("Thin Ice"); 
        setSize(960, 720); 
        setResizable(false); 

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // set the default close operation
        
        setLocationRelativeTo(null); // center the window on the screen
        setVisible(true); 
    }

    public static void main(String[] args) {
        JFrame window = new GameWindow();

        window.addKeyListener(new GameKeyListener());

    }
}