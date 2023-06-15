import java.awt.Color;
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
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class UI {
 
    private static boolean mazeConfigIsNew = false;

    // constraints
    private static Insets insets = new Insets(5, 5, 5, 5);

    private static GridBagConstraints gbcCol1 = new GridBagConstraints();
    private static GridBagConstraints gbcCol2 = new GridBagConstraints();
    
    static {
        gbcCol1.gridx = 0;
        gbcCol1.insets = insets;

        gbcCol2.gridx = 1;
        gbcCol2.insets = insets;
    }

    public static void setUpKeyConfig() {
        
        JPanel panel = new JPanel(new GridBagLayout());

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

        JButton btnConfig = getConfigPopupButton("Key config", panel);
        
        // move to bottomleft
        int pad = 10;
        int y = Window.height - btnConfig.getHeight() - pad;
        btnConfig.setLocation(pad, y);

    }

    public static void setUpColorConfig() {
          
        JPanel panel = new JPanel(new GridBagLayout());

        // new label in 1st column, new btn in 2nd column
        for (Node.Type type : Config.BLOCK_COLORS.keySet()) {

            JLabel label = new JLabel(type.name());
            JButton btn = new JButton();
            Color currentColor = Config.BLOCK_COLORS.get(type);
            btn.setBackground(currentColor);
            
            label.setPreferredSize(new Dimension(125, label.getPreferredSize().height));
            btn.setPreferredSize(new Dimension(30, 30));
            
            panel.add(label, gbcCol1);
            panel.add(btn, gbcCol2);

            btn.addActionListener(e -> {
                Color newColor = JColorChooser.showDialog(Main.window, "Color for " + type, currentColor);

                if (newColor != null) {
                    btn.setBackground(newColor);
                    
                    Config.setBlockColor(type, newColor);
                    Main.newBlockColors();
                }
            });

        }

        JButton btnConfig = getConfigPopupButton("Color config", panel);
        
        int pad = 10;
        int x = (Window.width - btnConfig.getWidth()) / 2;
        int y = Window.height - btnConfig.getHeight() - pad;
        btnConfig.setLocation(x, y);

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

    public static void setUpMazeConfig() {

        JPanel panel = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        // label and slider for chance of double
        int valDouble = (int)(Config.mazeGen.chanceDouble * 100);
        JLabel labelDouble = new JLabel("Chance of double: " + valDouble + "%");
        JSlider sliderDouble = getNewSlider(100, valDouble);

        sliderDouble.addChangeListener(e -> {
            int newVal = sliderDouble.getValue();
            Config.mazeGen.chanceDouble = (float)newVal / 100;
            labelDouble.setText("Chance of double: " + newVal + "%");
            mazeConfigIsNew = true;
        });

        panel.add(labelDouble, gbc);
        panel.add(sliderDouble, gbc);

        // label and slider for width and height
        JSlider sliderWidth = getNewSlider(20, Config.mazeGen.width);
        JSlider sliderHeight = getNewSlider(20, Config.mazeGen.height);

        JLabel labelWidth = new JLabel("Width: " + Config.mazeGen.width);
        JLabel labelHeight = new JLabel("Height: " + Config.mazeGen.height);

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
        JLabel labelLen = new JLabel("Min path length: " + Config.mazeGen.minPathLength);
        JSlider sliderLen = getNewSlider(100, Config.mazeGen.minPathLength);
        sliderLen.addChangeListener(e -> {
            int newVal = sliderLen.getValue();
            Config.setMazeMinPathLength(newVal);
            labelLen.setText("Min path length: " + newVal);

            mazeConfigIsNew = true;
        });
        panel.add(labelLen, gbc);
        panel.add(sliderLen, gbc);

        // slider for max hint size
        JLabel labelHint = new JLabel("Hint length: " + Config.hintMax);
        JSlider sliderHint = getNewSlider(100, Config.mazeGen.minPathLength);
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

        JButton btn = getConfigPopupButton("Maze config", panel);

        // move to bottomright
        int pad = 10;
        int x = Window.width - btn.getWidth() - pad;
        int y = Window.height - btn.getHeight() - pad;
        btn.setLocation(x, y);
    }

    public static JButton getConfigPopupButton(String title, JPanel contentPane) {
        
        // setup button that opens a dialog
        JButton btn = new JButton(title);
        btn.setSize(btn.getPreferredSize());
        Main.window.sprites.add(btn);

        // open popup with `contentPane`
        btn.addActionListener(e -> {
            JDialog dialog = new JDialog(Main.window, title, true);
            dialog.setResizable(false);
            dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            dialog.setContentPane(contentPane);
            dialog.setLocationRelativeTo(Main.window);
            dialog.pack();
            dialog.setVisible(true);

            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    Main.window.requestFocus();  // prevents focus back to the config btn and rather the game canvas
                    
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
