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

    private static JSlider hintMaxSlider;
    private static JLabel hintMaxLabel;

    private static JSlider amountGroundSlider;
    private static JLabel amountGroundLabel;
    private static JSlider amountDoublesSlider;
    private static JLabel amountDoublesLabel;

    private static JButton endMustBeDouble;

    // constraints
    private static Insets insets = new Insets(5, 5, 5, 5);

    private static GridBagConstraints horizontalGbcCol1 = new GridBagConstraints();
    private static GridBagConstraints horizontalGbcCol2 = new GridBagConstraints();

    private static GridBagConstraints verticalGbc = new GridBagConstraints();
    
    static {
        horizontalGbcCol1.gridx = 0;
        horizontalGbcCol1.insets = insets;

        horizontalGbcCol2.gridx = 1;
        horizontalGbcCol2.insets = insets;

        verticalGbc.gridx = 0;
        verticalGbc.anchor = GridBagConstraints.CENTER;
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
            
            panel.add(label, horizontalGbcCol1);
            panel.add(btn, horizontalGbcCol2);

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

        panel.add(label, horizontalGbcCol1);
        panel.add(cb, horizontalGbcCol2);
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
            
            panel.add(label, horizontalGbcCol1);
            panel.add(btn, horizontalGbcCol2);

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
        
        setupMazeConfigEndCanBeDouble(panel);
        setupMazeConfigEndMustBeDouble(panel);
        setupMazeConfigDoublesArePlacedFirst(panel);
        setupMazeConfigTryChangeNodeType(panel);
        setupMazeConfigAmountDoubles(panel);
        setupMazeConfigAmountGround(panel);
        setupMazeConfigPathLength(panel);
        setupMazeConfigSize(panel);
        setupMazeConfigHintMax(panel);
        setupMazeConfigHintType(panel);
        
        // add config button to bottom right
        JButton btn = getConfigPopupButton("Maze config", panel);
        int pad = 10;
        int x = Window.width - btn.getWidth() - pad;
        int y = Window.height - btn.getHeight() - pad;
        btn.setLocation(x, y);
    }

    private static void updateAmountDoubles(int v) {

        int lastVal = MazeGen.amountDoubles;
        int convertedVal = MazeGen.setAmountDoubles(v);

        if (convertedVal != v && v > lastVal) {
            // increment instead of decrementing
            convertedVal = MazeGen.setAmountDoubles(v+1);
        }
        
        amountDoublesSlider.setValue(convertedVal);   // potentially decreased
        amountDoublesLabel.setText("Amount of double nodes: " + convertedVal);
    
        amountGroundSlider.setMaximum(MazeGen.amountGroundMax);
        amountGroundLabel.setText("Amount of ground nodes: " + MazeGen.amountGround);
        
        // limit amountGround
        amountGroundSlider.setMinimum(MazeGen.amountGroundMin);

        int slideDiff = amountGroundSlider.getMaximum() - amountGroundSlider.getMinimum();
        amountGroundSlider.setEnabled(slideDiff == 0 ? false : true);
    
        pathLengthLabel.setText("Path length: " + MazeGen.pathLength);

        limitHintMax();
        
        mazeConfigIsNew = true;

        endMustBeDouble.setEnabled(MazeGen.endCanBeDouble && MazeGen.amountDoubles > 0);
        endMustBeDouble.setText("End must be double: " + MazeGen.endMustBeDouble);
    }

    private static void setupMazeConfigAmountDoubles(JPanel panel) {
        
        // label and slider for amount of doubles
        amountDoublesLabel = new JLabel("Amount of double nodes: " + MazeGen.amountDoubles);
        amountDoublesSlider = getNewSlider(MazeGen.amountDoublesMax, MazeGen.amountDoubles);

        amountDoublesSlider.addChangeListener(e -> {
            updateAmountDoubles(amountDoublesSlider.getValue());
        });

        panel.add(amountDoublesLabel, verticalGbc);
        panel.add(amountDoublesSlider, verticalGbc);
    }

    private static void setupMazeConfigAmountGround(JPanel panel) {
        
        // label and slider for amount of ground
        amountGroundLabel = new JLabel("Amount of ground nodes: " + MazeGen.amountGround);
        amountGroundSlider = getNewSlider(MazeGen.amountGroundMax, MazeGen.amountGround);

        amountGroundSlider.addChangeListener(e -> {
            int newVal = amountGroundSlider.getValue();

            amountGroundLabel.setText("Amount of ground nodes: " + newVal);
            
            MazeGen.setAmountGround(newVal);
            
            pathLengthLabel.setText("Path length: " + MazeGen.pathLength);
            limitHintMax();
            
            mazeConfigIsNew = true;
        });

        panel.add(amountGroundLabel, verticalGbc);
        panel.add(amountGroundSlider, verticalGbc);
    }

    private static void setupMazeConfigSize(JPanel panel) {
        
        // label and slider for width and height
        JSlider sliderWidth = getNewSlider(20, MazeGen.getWidth());
        JSlider sliderHeight = getNewSlider(20, MazeGen.getHeight());

        JLabel labelWidth = new JLabel("Width: " + MazeGen.getWidth());
        JLabel labelHeight = new JLabel("Height: " + MazeGen.getHeight());

        sliderWidth.addChangeListener(e -> {
            int val = sliderWidth.getValue();
            labelWidth.setText("Width: " + val);
            mazeConfigIsNew = true;
            
            MazeGen.setWidth(val);
            System.out.println();
            System.out.println("new width");
            limitAfterNewSize();
        });
        
        sliderHeight.addChangeListener(e -> {
            int val = sliderHeight.getValue();
            labelHeight.setText("Height: " + val);
            mazeConfigIsNew = true;

            MazeGen.setHeight(val);
            limitAfterNewSize();
        });

        panel.add(labelWidth, verticalGbc);
        panel.add(sliderWidth, verticalGbc);
        
        panel.add(labelHeight, verticalGbc);
        panel.add(sliderHeight, verticalGbc);
    }

    private static void limitHintMax() {
        
        if (MazeGen.amountNodesAll < Config.hintMax) {
            Config.setHintMax(MazeGen.amountNodesAll);
            hintMaxLabel.setText("Hint length: " + Config.hintMax);
        }
        hintMaxSlider.setMaximum(MazeGen.pathLength);
    }

    private static void limitAfterNewSize() {

        limitHintMax();

        amountGroundSlider.setMaximum(MazeGen.amountGroundMax);
        amountGroundLabel.setText("Amount of ground nodes: " + MazeGen.amountGround);
        
        amountDoublesSlider.setMaximum(MazeGen.amountDoublesMax);
        amountDoublesLabel.setText("Amount of double nodes: " + MazeGen.amountDoubles);
    }

    private static void setupMazeConfigPathLength(JPanel panel) {
        
        // slider for maze length
        pathLengthLabel = new JLabel("Path length: " + MazeGen.pathLength);
        pathLengthLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0));

        panel.add(pathLengthLabel, verticalGbc);
    }

    private static void setupMazeConfigHintMax(JPanel panel) {

        // slider for max hint size
        hintMaxLabel = new JLabel("Hint length: " + Config.hintMax);
        hintMaxSlider = getNewSlider(MazeGen.pathLength, Config.hintMax);
        hintMaxSlider.addChangeListener(e -> {
            int newVal = hintMaxSlider.getValue();
            Config.setHintMax(newVal);
            hintMaxLabel.setText("Hint length: " + Config.hintMax);

            mazeConfigIsNew = true;
        });
        panel.add(hintMaxLabel, verticalGbc);
        panel.add(hintMaxSlider, verticalGbc);
    }

    private static void setupMazeConfigHintType(JPanel panel) {
        
        // button for hint types
        JButton btnHintType = new JButton();
        if (Config.hintTypeLongest) {
            btnHintType.setText("Hint path type: Longest path");
        }
        else {
            btnHintType.setText("Hint path type: Shortest path");
        }

        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        container.add(btnHintType);

        panel.add(container, verticalGbc);

        // panel.add(btnHintType, verticalGbc);
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

    private static JButton getButton(JPanel panel) {
        JButton btn = new JButton();
        
        JPanel container = new JPanel();
        container.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        container.add(btn);

        panel.add(container, verticalGbc);
        
        return btn;
    }

    private static void setupMazeConfigEndCanBeDouble(JPanel panel) {

        JButton btn = getButton(panel);
        btn.setText("End can be double: " + MazeGen.endCanBeDouble);

        btn.addActionListener(e -> {
            MazeGen.setEndCanBeDouble(!MazeGen.endCanBeDouble);
            btn.setText("End can be double: " + MazeGen.endCanBeDouble);
            mazeConfigIsNew = true;

            updateAmountDoubles(MazeGen.amountDoubles);
        });
    }
    
    private static void setupMazeConfigEndMustBeDouble(JPanel panel) {

        endMustBeDouble = getButton(panel);
        endMustBeDouble.setText("End must be double: " + MazeGen.endMustBeDouble);
        endMustBeDouble.setEnabled(MazeGen.endCanBeDouble && MazeGen.amountDoubles > 0);

        endMustBeDouble.addActionListener(e -> {
            if (MazeGen.setEndMustBeDouble(!MazeGen.endMustBeDouble)) {
                endMustBeDouble.setText("End must be double: " + MazeGen.endMustBeDouble);
                mazeConfigIsNew = true;
            }
        });
    }
    
    private static void setupMazeConfigDoublesArePlacedFirst(JPanel panel) {
        
        JButton btn = getButton(panel);
        btn.setText("Doubles are placed first (faster): " + MazeGen.doublesArePlacedFirst);
    
        btn.addActionListener(e -> {
            MazeGen.doublesArePlacedFirst = !MazeGen.doublesArePlacedFirst;
            btn.setText("Doubles are placed first (faster): " + MazeGen.doublesArePlacedFirst);
            mazeConfigIsNew = true;
        });
    }

    private static void setupMazeConfigTryChangeNodeType(JPanel panel) {
        
        JButton btn = getButton(panel);
        btn.setText("Change types during backtrack: " + MazeGen.tryChangeNodeType);
    
        btn.addActionListener(e -> {
            MazeGen.tryChangeNodeType = !MazeGen.tryChangeNodeType;
            btn.setText("Change types during backtrack: " + MazeGen.tryChangeNodeType);
            mazeConfigIsNew = true;
        });
    }

    private static JSlider getNewSlider(int max, int currentVal) {

        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, max, currentVal);

        slider.setMajorTickSpacing(3);
        slider.setMinorTickSpacing(1);
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
