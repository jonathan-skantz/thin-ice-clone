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

    // save as fields in order to modify when setting size
    private static JLabel pathLengthLabel;
    private static JSlider pathLengthSlider;

    private static JSlider hintMaxSlider;
    private static JLabel hintMaxLabel;

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

    public static void setupConfigs() {
        setupKeyConfig();
        setupColorConfig();
        setupMazeConfig();
    }



    // ---------- KEY CONFIG ----------

    private static void setupKeyConfig() {

        JPanel panel = new JPanel(new GridBagLayout());

        setupKeyConfigKeybinds(panel);
        setupKeyConfigContinuousMovement(panel);

        // add config button to bottom left
        JButton btnConfig = getConfigPopupButton("Key config", panel);
        int pad = 10;
        int y = Window.height - btnConfig.getHeight() - pad;
        btnConfig.setLocation(pad, y);
    }

    private static void setupKeyConfigKeybinds(JPanel panel) {
      
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

    }

    private static void setupKeyConfigContinuousMovement(JPanel panel) {
        
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
    }

    
    
    // ---------- COLOR CONFIG ----------

    private static void setupColorConfig() {
          
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

        // add config button to bottom mid
        JButton btnConfig = getConfigPopupButton("Color config", panel);
        int pad = 10;
        int x = (Window.width - btnConfig.getWidth()) / 2;
        int y = Window.height - btnConfig.getHeight() - pad;
        btnConfig.setLocation(x, y);

    }



    // ---------- MAZE CONFIG ----------

    private static void setupMazeConfig() {

        JPanel panel = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        setupMazeConfigChanceDouble(panel, gbc);
        setupMazeConfigSize(panel, gbc);
        setupMazeConfigPathLength(panel, gbc);
        setupMazeConfigHintMax(panel, gbc);
        setupMazeConfigHintType(panel, gbc);
        
        // add config button to bottom right
        JButton btn = getConfigPopupButton("Maze config", panel);
        int pad = 10;
        int x = Window.width - btn.getWidth() - pad;
        int y = Window.height - btn.getHeight() - pad;
        btn.setLocation(x, y);
    }

    private static void setupMazeConfigChanceDouble(JPanel panel, GridBagConstraints gbc) {
        
        // label and slider for chance of double
        int val = (int)(MazeGen.fractionDoubleNodes * 100);
        JLabel labelDouble = new JLabel("Double node frequency: " + val + "%");
        JSlider sliderDouble = getNewSlider(100, val);

        sliderDouble.addChangeListener(e -> {
            int newVal = sliderDouble.getValue();
            MazeGen.fractionDoubleNodes = (float)newVal / 100;  // amountDoubles is set in .generate()

            labelDouble.setText("Double node frequency: " + newVal + "%");
            mazeConfigIsNew = true;
        });

        panel.add(labelDouble, gbc);
        panel.add(sliderDouble, gbc);
    }

    private static void setupMazeConfigSize(JPanel panel, GridBagConstraints gbc) {
        
        // label and slider for width and height
        JSlider sliderWidth = getNewSlider(20, MazeGen.getWidth());
        JSlider sliderHeight = getNewSlider(20, MazeGen.getHeight());

        JLabel labelWidth = new JLabel("Width: " + MazeGen.getWidth());
        JLabel labelHeight = new JLabel("Height: " + MazeGen.getHeight());

        sliderWidth.addChangeListener(e -> {
            int val = sliderWidth.getValue();
            labelWidth.setText("Width: " + val);
            mazeConfigIsNew = true;
            
            boolean pathLengthDecreased = MazeGen.setWidth(val);
            limitAfterNewSize(pathLengthDecreased);
        });
        
        sliderHeight.addChangeListener(e -> {
            int val = sliderHeight.getValue();
            labelHeight.setText("Height: " + val);
            mazeConfigIsNew = true;
            
            boolean pathLengthDecreased = MazeGen.setHeight(val);
            limitAfterNewSize(pathLengthDecreased);
        });

        panel.add(labelWidth, gbc);
        panel.add(sliderWidth, gbc);
        
        panel.add(labelHeight, gbc);
        panel.add(sliderHeight, gbc);
    }

    private static void limitAfterNewSize(boolean pathLengthDecreased) {
        // new width or height should also update labels and sliders of pathLengthMax and hintMax

        // update pathLengthMax
        if (pathLengthDecreased) {
            pathLengthLabel.setText("Path length: " + MazeGen.pathLengthMax);
        }
        pathLengthSlider.setMajorTickSpacing((int)(MazeGen.pathLengthMax * 0.10));
        pathLengthSlider.setMaximum(MazeGen.pathLengthMax);
        
        // update hintMax
        if (MazeGen.pathLengthMax < Config.hintMax) {
            Config.hintMax = MazeGen.pathLengthMax;
            hintMaxLabel.setText("Hint length: " + Config.hintMax);
        }
        hintMaxSlider.setMajorTickSpacing((int)(MazeGen.pathLengthMax * 0.10));
        hintMaxSlider.setMaximum(MazeGen.pathLengthMax);

    }

    private static void setupMazeConfigPathLength(JPanel panel, GridBagConstraints gbc) {
        
        // slider for maze length
        pathLengthLabel = new JLabel("Path length: " + MazeGen.pathLength);
        pathLengthSlider = getNewSlider(MazeGen.pathLengthMax, MazeGen.pathLength);
        pathLengthSlider.addChangeListener(e -> {
            int newVal = pathLengthSlider.getValue();
            MazeGen.setPathLength(newVal);
            pathLengthLabel.setText("Path length: " + newVal);
            mazeConfigIsNew = true;
        });

        panel.add(pathLengthLabel, gbc);
        panel.add(pathLengthSlider, gbc);
    }

    private static void setupMazeConfigHintMax(JPanel panel, GridBagConstraints gbc) {

        // slider for max hint size
        hintMaxLabel = new JLabel("Hint length: " + Config.hintMax);
        hintMaxSlider = getNewSlider(MazeGen.pathLengthMax, Config.hintMax);
        hintMaxSlider.addChangeListener(e -> {
            int newVal = hintMaxSlider.getValue();
            Config.setHintMax(newVal);
            hintMaxLabel.setText("Hint length: " + Config.hintMax);

            mazeConfigIsNew = true;
        });
        panel.add(hintMaxLabel, gbc);
        panel.add(hintMaxSlider, gbc);
    }

    private static void setupMazeConfigHintType(JPanel panel, GridBagConstraints gbc) {
        
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
    }



    
    private static JSlider getNewSlider(int max, int currentVal) {

        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, max, currentVal);

        slider.setMajorTickSpacing((int)(max * 0.10));
        slider.setMinorTickSpacing((int)(max * 0.05));
        slider.setSnapToTicks(true);       
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(500, 100));
        slider.setBorder(BorderFactory.createEmptyBorder(5, 5, 50, 5));

        return slider;
    }

    private static JButton getConfigPopupButton(String title, JPanel contentPane) {
        
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
