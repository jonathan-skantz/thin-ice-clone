import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class UI {
 
    public static void setUpKeyConfig(Window window) {

        Insets insets = new Insets(5, 5, 5, 5);

        // constraints for first column
        GridBagConstraints gbcCol1 = new GridBagConstraints();
        gbcCol1.gridx = 0;
        gbcCol1.gridy = GridBagConstraints.RELATIVE;
        gbcCol1.weightx = 0.0; // set weight to 0 to make column fixed size
        gbcCol1.insets = insets;
        
        // constraints for second column
        GridBagConstraints gbcCol2 = new GridBagConstraints();
        gbcCol2.gridx = 1;
        gbcCol2.gridy = GridBagConstraints.RELATIVE;
        gbcCol2.weightx = 1; // set weight to 1 to make column take up remaining horizontal space
        gbcCol2.insets = insets;


        JPanel keyConfig = new JPanel(new GridBagLayout());

        for (KeyHandler.ActionKey key : KeyHandler.ActionKey.values()) {

            JLabel label = new JLabel(key.name());
            label.setPreferredSize(new Dimension(150, label.getPreferredSize().height));
            keyConfig.add(label, gbcCol1);

            JButton btn = new JButton(KeyEvent.getKeyText(key.keyCode));
            btn.setPreferredSize(new Dimension(100, btn.getPreferredSize().height));
            keyConfig.add(btn, gbcCol2);

            btn.addActionListener(e -> {
                // when clicked, change text to "?" and start listening for key press
                btn.setText("?");

                btn.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        // change control and stop listening
                        key.keyCode = e.getKeyCode();
                        btn.setText(KeyEvent.getKeyText(key.keyCode));
                        btn.removeKeyListener(this);
                    }
                });
            });

        }

        JButton btnConfig = getConfigPopupButton(window, "Key config", keyConfig);
        
        // move to bottomleft
        int pad = 10;
        int y = Window.height - btnConfig.getHeight() - pad;
        btnConfig.setLocation(pad, y);

    }

    public static JButton getConfigPopupButton(Window window, String title, JPanel contentPane) {
        // setup button that opens a dialog
        JButton btn = new JButton(title);
        btn.setSize(btn.getPreferredSize());
        window.sprites.add(btn);

        // open popup with `contentPane`
        btn.addActionListener(e -> {
            JDialog dialog = new JDialog(window, title, true);
            dialog.setResizable(false);
            dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            dialog.setContentPane(contentPane);
            dialog.setLocationRelativeTo(window);
            dialog.pack();
            dialog.setVisible(true);

            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    window.requestFocus();  // prevents focus back to the config btn
                }
            });
        });

        return btn;
    }

}
