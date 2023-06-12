import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class UI {
 
    private static boolean mazeConfigIsNew = false;

    public static void setUpKeyConfig(Window window) {
        
        JPanel panel = new JPanel(new GridBagLayout());

        // constraints
        Insets insets = new Insets(5, 5, 5, 5);

        // 1st column
        GridBagConstraints gbcCol1 = new GridBagConstraints();
        gbcCol1.gridx = 0;
        gbcCol1.insets = insets;
        
        // 2nd column
        GridBagConstraints gbcCol2 = new GridBagConstraints();
        gbcCol2.gridx = 1;
        gbcCol2.insets = insets;

        // new label in 1st column, new btn in 2nd column
        for (KeyHandler.ActionKey key : KeyHandler.ActionKey.values()) {

            JLabel label = new JLabel(key.name());
            JButton btn = new JButton(KeyEvent.getKeyText(key.keyCode));
            
            label.setPreferredSize(new Dimension(125, label.getPreferredSize().height));
            btn.setPreferredSize(new Dimension(100, btn.getPreferredSize().height));
            
            panel.add(label, gbcCol1);
            panel.add(btn, gbcCol2);

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

        // checkbox for continous key presses
        JLabel label = new JLabel("Allow continuous");
        label.setPreferredSize(new Dimension(125, label.getPreferredSize().height));

        JCheckBox cb = new JCheckBox(String.valueOf(KeyHandler.allowContinuous), KeyHandler.allowContinuous);
        cb.setPreferredSize(new Dimension(100, cb.getPreferredSize().height));

        cb.addItemListener(e -> {
            KeyHandler.allowContinuous = cb.isSelected();
        });

        panel.add(label, gbcCol1);
        panel.add(cb, gbcCol2);

        JButton btnConfig = getConfigPopupButton(window, "Key config", panel);
        
        // move to bottomleft
        int pad = 10;
        int y = Window.height - btnConfig.getHeight() - pad;
        btnConfig.setLocation(pad, y);

    }

    public static JSlider getNewSlider(int max, int currentVal) {

        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, max, currentVal);

        if (max == 100) {
            slider.setMajorTickSpacing(10);
            slider.setMinorTickSpacing(5);
            slider.setSnapToTicks(true);
        }
        else {
            slider.setMajorTickSpacing(5);
            slider.setMinorTickSpacing(1);
        }
        
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(500, 100));
        slider.setBorder(BorderFactory.createEmptyBorder(5, 5, 50, 5));

        return slider;
    }

    public static void setUpMazeConfig(Window window, MazeGen mazeGen) {

        JPanel panel = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        // (label followed by slider) x 3
        for (MazeGen.ChanceNextNode chanceItem : MazeGen.ChanceNextNode.values()) {

            int val = (int)(chanceItem.chance * 100);
            JLabel label = new JLabel(chanceItem.name() + ": " + val + "%");
            JSlider slider = getNewSlider(100, val);

            slider.addChangeListener(e -> {
                int newVal = slider.getValue();
                chanceItem.chance = (float)newVal / 100;
                label.setText(chanceItem.name() + ": " + newVal + "%");
                mazeConfigIsNew = true;
            });

            panel.add(label, gbc);
            panel.add(slider, gbc);
        }

        // label and slider for width and height
        JSlider sliderWidth = getNewSlider(20, mazeGen.width);
        JSlider sliderHeight = getNewSlider(20, mazeGen.height);

        JLabel labelWidth = new JLabel("Width: " + mazeGen.width);
        JLabel labelHeight = new JLabel("Height: " + mazeGen.height);

        sliderWidth.addChangeListener(e -> {
            int val = sliderWidth.getValue();
            Config.setMazeWidth(val);
            labelWidth.setText("Width: " + val);
            mazeConfigIsNew = true;
        });
        
        sliderHeight.addChangeListener(e -> {
            int val = sliderHeight.getValue();
            Config.setMazeHeight(val);
            labelHeight.setText("Height: " + val);
            mazeConfigIsNew = true;
        });

        panel.add(labelWidth, gbc);
        panel.add(sliderWidth, gbc);
        
        panel.add(labelHeight, gbc);
        panel.add(sliderHeight, gbc);

        // slider for min maze length
        JLabel labelLen = new JLabel("Min path length: " + mazeGen.minPathLength);
        JSlider sliderLen = getNewSlider(100, mazeGen.minPathLength);
        sliderLen.addChangeListener(e -> {
            int newVal = sliderLen.getValue();
            mazeGen.minPathLength = newVal;
            labelLen.setText("Min path length: " + newVal);

            mazeConfigIsNew = true;
        });
        panel.add(labelLen, gbc);
        panel.add(sliderLen, gbc);


        // slider for max hint size
        JLabel labelHint = new JLabel("Hint length: " + Config.hintMax);
        JSlider sliderHint = getNewSlider(100, mazeGen.minPathLength);
        sliderHint.addChangeListener(e -> {
            int newVal = sliderHint.getValue();
            Config.setHintMax(newVal);
            labelHint.setText("Hint length: " + Config.hintMax);

            mazeConfigIsNew = true;
        });
        panel.add(labelHint, gbc);
        panel.add(sliderHint, gbc);

        // button for hint types
        JButton btnHintType = new JButton();
        if (Config.hintTypeLongest) {
            btnHintType.setText("Hint path type: Longest path");
        }
        else {
            btnHintType.setText("Hint path type: Shortest path");
        }

        panel.add(btnHintType, gbc);
        btnHintType.addActionListener(e -> {
            Config.hintTypeLongest = !Config.hintTypeLongest;

            if (Config.hintTypeLongest) {
                btnHintType.setText("Hint path type: Longest path");
            }
            else {
                btnHintType.setText("Hint path type: Shortest path");
            }
        });

        JButton btn = getConfigPopupButton(window, "Maze config", panel);

        // move to bottomright
        int pad = 10;
        int x = Window.width - btn.getWidth() - pad;
        int y = Window.height - btn.getHeight() - pad;
        btn.setLocation(x, y);
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
                    window.requestFocus();  // prevents focus back to the config btn and rather the game canvas
                    
                    // TODO: this also checks when closing the key config popup (not desired)
                    if (mazeConfigIsNew) {
                        Main.generateNewMaze();
                        mazeConfigIsNew = false;
                    }
                }
            });
        });

        return btn;
    }

}
